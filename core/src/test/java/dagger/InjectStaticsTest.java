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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class InjectStaticsTest {
  @BeforeAll
  public static void setUp() {
    InjectsOneField.staticField = null;
    InjectsStaticAndNonStatic.staticField = null;
  }

  public static class InjectsOneField {
    @Inject static String staticField;
  }

  public static class InjectsStaticAndNonStatic {
    @Inject Integer nonStaticField;
    @Inject static String staticField;
  }

  @Test
  public void injectStatics() {
    @Module(staticInjections = InjectsOneField.class)
    class TestModule {
      @Provides String provideString() {
        return "static";
      }
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(),new TestModule());
    assertNull(InjectsOneField.staticField);
    graph.injectStatics();
    assertEquals(InjectsOneField.staticField, "static");
  }

  @Test
  public void instanceFieldsNotInjectedByInjectStatics() {
    @Module(
        staticInjections = InjectsStaticAndNonStatic.class,
        injects = InjectsStaticAndNonStatic.class)
    class TestModule {
      @Provides String provideString() {
        return "static";
      }
      @Provides Integer provideInteger() {
        throw new AssertionError();
      }
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    assertNull(InjectsStaticAndNonStatic.staticField);
    graph.injectStatics();
    assertEquals(InjectsStaticAndNonStatic.staticField, "static");
  }

  @Test
  public void staticFieldsNotInjectedByInjectMembers() {
    @Module(
        staticInjections = InjectsStaticAndNonStatic.class,
        injects = InjectsStaticAndNonStatic.class)
    class TestModule {
      @Provides String provideString() {
        throw new AssertionError();
      }
      @Provides Integer provideInteger() {
        return 5;
      }
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    assertNull(InjectsStaticAndNonStatic.staticField);
    InjectsStaticAndNonStatic object = new InjectsStaticAndNonStatic();
    graph.inject(object);
    assertNull(InjectsStaticAndNonStatic.staticField);
    assertEquals(object.nonStaticField, 5);
  }
}
