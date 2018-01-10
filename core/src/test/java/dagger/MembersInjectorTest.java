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
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests MembersInjector injection, and how object graph features interact with
 * types unconstructable types (types that support members injection only).
 */
public final class MembersInjectorTest {
  @Test
  public void injectMembers() {
    class TestEntryPoint {
      @Inject MembersInjector<Injectable> membersInjector;
    }

    @Module(injects = TestEntryPoint.class)
    class StringModule {
      @Provides String provideString() {
        return "injected";
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    ObjectGraph.createWith(new TestingLoader(), new StringModule()).inject(entryPoint);
    Injectable injectable = new Injectable();
    entryPoint.membersInjector.injectMembers(injectable);
    assertEquals(injectable.injected, "injected");
  }

  static class Injectable {
    @Inject String injected;
  }

  static class Unconstructable {
    final String constructor;
    @Inject String injected;
    Unconstructable(String constructor) {
      this.constructor = constructor;
    }
  }

  @Test
  public void membersInjectorOfUnconstructableIsOkay() {
    class TestEntryPoint {
      @Inject MembersInjector<Unconstructable> membersInjector;
    }

    @Module(injects = TestEntryPoint.class)
    class StringModule {
      @Provides String provideString() {
        return "injected";
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    ObjectGraph.createWith(new TestingLoader(), new StringModule()).inject(entryPoint);
    Unconstructable object = new Unconstructable("constructor");
    entryPoint.membersInjector.injectMembers(object);
    assertEquals(object.constructor, "constructor");
    assertEquals(object.injected, "injected");
  }


  @Test
  public void injectionOfUnconstructableFails() {
    class TestEntryPoint {
      @Inject Unconstructable unconstructable;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    try {
      graph.get(TestEntryPoint.class);
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @Test
  public void instanceInjectionOfMembersOnlyType() {
    class TestEntryPoint {
      @Inject Provider<Unconstructable> provider;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    try {
      graph.get(TestEntryPoint.class);
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @Test
  public void rejectUnconstructableSingleton() {
    class TestEntryPoint {
      @Inject MembersInjector<UnconstructableSingleton> membersInjector;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    try {
      graph.get(TestEntryPoint.class);
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @Singleton
  static class UnconstructableSingleton {
    final String constructor;
    @Inject String injected;
    UnconstructableSingleton(String constructor) {
      this.constructor = constructor;
    }
  }

  class NonStaticInner {
    @Inject String injected;
  }

  @Test
  public void membersInjectorOfNonStaticInnerIsOkay() {
    class TestEntryPoint {
      @Inject MembersInjector<NonStaticInner> membersInjector;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides String provideString() {
        return "injected";
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    ObjectGraph.createWith(new TestingLoader(), new TestModule()).inject(entryPoint);
    NonStaticInner nonStaticInner = new NonStaticInner();
    entryPoint.membersInjector.injectMembers(nonStaticInner);
    assertEquals(nonStaticInner.injected, "injected");
  }

  @Test
  public void instanceInjectionOfNonStaticInnerFailsEarly() {
    class TestEntryPoint {
      @Inject NonStaticInner nonStaticInner;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    try {
      graph.get(TestEntryPoint.class);
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @Test
  public void providesMethodsAndMembersInjectionDoNotConflict() {
    class InjectsString {
      @Inject String value;
    }

    class TestEntryPoint {
      @Inject Provider<InjectsString> provider;
      @Inject MembersInjector<InjectsString> membersInjector;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides InjectsString provideInjectsString() {
        InjectsString result = new InjectsString();
        result.value = "provides";
        return result;
      }
      @Provides String provideString() {
        return "members";
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    ObjectGraph.createWith(new TestingLoader(), new TestModule()).inject(entryPoint);

    InjectsString provided = entryPoint.provider.get();
    assertEquals(provided.value, "provides");

    InjectsString membersInjected = new InjectsString();
    entryPoint.membersInjector.injectMembers(membersInjected);
    assertEquals(membersInjected.value, "members");
  }
}
