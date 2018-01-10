/*
 * Copyright (C) 2012 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dagger;

import dagger.internal.TestingLoader;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//TODO: Migrate to compiler.

public final class ModuleTest {
  static class TestEntryPoint {
    @Inject String s;
  }

  @Module(injects = TestEntryPoint.class)
  static class ModuleWithEntryPoint {
  }

  @Test
  public void childModuleWithEntryPoint() {
    @Module(includes = ModuleWithEntryPoint.class)
    class TestModule {
      @Provides String provideString() {
        return "injected";
      }
    }

    ObjectGraph objectGraph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    TestEntryPoint entryPoint = objectGraph.get(TestEntryPoint.class);
    assertEquals(entryPoint.s, "injected");
  }

  static class TestStaticInjection {
    @Inject static String s;
  }

  @Module(staticInjections = TestStaticInjection.class)
  static class ModuleWithStaticInjection {
  }

  @Test
  public void childModuleWithStaticInjection() {
    @Module(includes = ModuleWithStaticInjection.class)
    class TestModule {
      @Provides String provideString() {
        return "injected";
      }
    }

    ObjectGraph objectGraph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    TestStaticInjection.s = null;
    objectGraph.injectStatics();
    assertEquals(TestStaticInjection.s, "injected");
  }

  @Module
  static class ModuleWithBinding {
    @Provides String provideString() {
      return "injected";
    }
  }

  @Test
  public void childModuleWithBinding() {

    @Module(
        injects = TestEntryPoint.class,
        includes = ModuleWithBinding.class
    )
    class TestModule {
    }

    ObjectGraph objectGraph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    TestEntryPoint entryPoint = new TestEntryPoint();
    objectGraph.inject(entryPoint);
    assertEquals(entryPoint.s, "injected");
  }

  @Module(includes = ModuleWithBinding.class)
  static class ModuleWithChildModule {
  }

  @Test
  public void childModuleWithChildModule() {

    @Module(
        injects = TestEntryPoint.class,
        includes = ModuleWithChildModule.class
    )
    class TestModule {
    }

    ObjectGraph objectGraph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    TestEntryPoint entryPoint = new TestEntryPoint();
    objectGraph.inject(entryPoint);
    assertEquals(entryPoint.s, "injected");
  }

  @Module
  static class ModuleWithConstructor {
    private final String value;

    ModuleWithConstructor(String value) {
      this.value = value;
    }

    @Provides String provideString() {
      return value;
    }
  }

  @Test
  public void childModuleMissingManualConstruction() {
    @Module(includes = ModuleWithConstructor.class)
    class TestModule {
    }

    try {
      ObjectGraph.createWith(new TestingLoader(), new TestModule());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void childModuleWithManualConstruction() {

    @Module(
        injects = TestEntryPoint.class,
        includes = ModuleWithConstructor.class
    )
    class TestModule {
    }

    ObjectGraph objectGraph = ObjectGraph.createWith(new TestingLoader(), new ModuleWithConstructor("a"), new TestModule());
    TestEntryPoint entryPoint = new TestEntryPoint();
    objectGraph.inject(entryPoint);
    assertEquals(entryPoint.s, "a");
  }

  static class A {}

  static class B { @Inject A a; }

  @Module(injects = A.class) public static class TestModuleA {
    @Provides A a() { return new A(); }
  }

  @Module(includes = TestModuleA.class, injects = B.class) public static class TestModuleB {}

  @Test
  public void autoInstantiationOfModules() {
    // Have to make these non-method-scoped or instantiation errors occur.
    ObjectGraph objectGraph = ObjectGraph.createWith(new TestingLoader(), TestModuleA.class);
    assertNotNull(objectGraph.get(A.class));
  }

  @Test
  public void autoInstantiationOfIncludedModules() {
    // Have to make these non-method-scoped or instantiation errors occur.
    ObjectGraph objectGraph = ObjectGraph.createWith(new TestingLoader(), new TestModuleB()); // TestModuleA auto-created.
    assertNotNull(objectGraph.get(A.class));
    assertNotNull(objectGraph.get(B.class).a);
  }

  static class ModuleMissingModuleAnnotation {}

  @Module(includes = ModuleMissingModuleAnnotation.class)
  static class ChildModuleMissingModuleAnnotation {}

  @Test
  public void childModuleMissingModuleAnnotation() {
    try {
      ObjectGraph.createWith(new TestingLoader(), new ChildModuleMissingModuleAnnotation());
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage()
          .contains("No @Module on dagger.ModuleTest$ModuleMissingModuleAnnotation"));
    }
  }

  @Module
  static class ThreadModule extends Thread {}

  @Test
  public void moduleExtendingClassThrowsException() {
    try {
      ObjectGraph.createWith(new TestingLoader(), new ThreadModule());
      fail();
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().startsWith("Modules must not extend from other classes: "));
    }
  }

  @Test
  public void provideProviderFails() {
    @Module
    class ProvidesProviderModule {
      @Provides Provider<Object> provideObject() {
        return null;
      }
    }
    try {
      ObjectGraph.createWith(new TestingLoader(), new ProvidesProviderModule());
      fail();
    } catch (IllegalStateException e) {
      assertTrue(e.getMessage().startsWith("@Provides method must not return Provider directly: "));
      assertTrue(e.getMessage().endsWith("ProvidesProviderModule.provideObject"));
    }
  }

  @Test
  public void provideRawProviderFails() {
    @Module
    class ProvidesRawProviderModule {
      @Provides Provider provideObject() {
        return null;
      }
    }
    try {
      ObjectGraph.createWith(new TestingLoader(), new ProvidesRawProviderModule());
      fail();
    } catch (IllegalStateException e) {
      assertTrue(e.getMessage().startsWith("@Provides method must not return Provider directly: "));
      assertTrue(e.getMessage().endsWith("ProvidesRawProviderModule.provideObject"));
    }
  }

  @Test
  public void provideLazyFails() {
    @Module
    class ProvidesLazyModule {
      @Provides Lazy<Object> provideObject() {
        return null;
      }
    }
    try {
      ObjectGraph.createWith(new TestingLoader(), new ProvidesLazyModule());
      fail();
    } catch (IllegalStateException e) {
      assertTrue(e.getMessage().startsWith("@Provides method must not return Lazy directly: "));
      assertTrue(e.getMessage().endsWith("ProvidesLazyModule.provideObject"));
    }
  }

  @Test
  public void provideRawLazyFails() {
    @Module
    class ProvidesRawLazyModule {
      @Provides Lazy provideObject() {
        return null;
      }
    }
    try {
      ObjectGraph.createWith(new TestingLoader(), new ProvidesRawLazyModule());
      fail();
    } catch (IllegalStateException e) {
      assertTrue(e.getMessage().startsWith("@Provides method must not return Lazy directly: "));
      assertTrue(e.getMessage().endsWith("ProvidesRawLazyModule.provideObject"));
    }
  }
}
