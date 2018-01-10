/*
 * Copyright (C) 2012 Google Inc.
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
import java.util.Arrays;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


public final class ExtensionTest {
  @Singleton
  static class A {
    @Inject A() {}
  }

  static class B {
    @Inject A a;
  }

  @Singleton
  static class C {
    @Inject A a;
    @Inject B b;
  }

  static class D {
    @Inject A a;
    @Inject B b;
    @Inject C c;
  }

  @Module(injects = { A.class, B.class }) static class RootModule { }

  @Module(addsTo = RootModule.class, injects = { C.class, D.class })
  static class ExtensionModule { }

  @Test
  public void basicExtension() {
    assertNotNull(ObjectGraph.createWith(new TestingLoader(), new RootModule())
        .plus(new ExtensionModule()));
  }

  @Test
  public void basicInjection() {
    ObjectGraph root = ObjectGraph.createWith(new TestingLoader(), new RootModule());
    assertNotNull(root.get(A.class));
    assertSame(root.get(A.class), root.get(A.class)); // Present and Singleton.
    assertNotSame(root.get(B.class), root.get(B.class)); // Not singleton.
    assertFailInjectNotRegistered(root, C.class); // Not declared in RootModule.
    assertFailInjectNotRegistered(root, D.class); // Not declared in RootModule.

    // Extension graph behaves as the root graph would for root-ish things.
    ObjectGraph extension = root.plus(new ExtensionModule());
    assertSame(root.get(A.class), extension.get(A.class));
    assertNotSame(root.get(B.class), extension.get(B.class));
    assertSame(root.get(B.class).a, extension.get(B.class).a);

    assertNotNull(extension.get(C.class).a);
    assertNotNull(extension.get(D.class).c);
  }

  @Test
  public void scopedGraphs() {
    ObjectGraph app = ObjectGraph.createWith(new TestingLoader(), new RootModule());
    assertNotNull(app.get(A.class));
    assertSame(app.get(A.class), app.get(A.class));
    assertNotSame(app.get(B.class), app.get(B.class));
    assertFailInjectNotRegistered(app, C.class);
    assertFailInjectNotRegistered(app, D.class);

    ObjectGraph request1 = app.plus(new ExtensionModule());
    ObjectGraph request2 = app.plus(new ExtensionModule());
    for (ObjectGraph request : Arrays.asList(request1, request2)) {
      assertNotNull(request.get(A.class));
      assertSame(request.get(A.class), request.get(A.class));
      assertNotSame(request.get(B.class), request.get(B.class));
      assertNotNull(request.get(C.class));
      assertSame(request.get(C.class), request.get(C.class));
      assertNotSame(request.get(D.class), request.get(D.class));
    }

    // Singletons are one-per-graph-instance where they are declared.
    assertNotSame(request1.get(C.class), request2.get(C.class));
    // Singletons that come from common roots should be one-per-common-graph-instance.
    assertSame(request1.get(C.class).a, request2.get(C.class).a);
  }

  private void assertFailInjectNotRegistered(ObjectGraph graph, Class<?> clazz) {
    try {
      assertNull(graph.get(clazz));
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("No inject"));
    }
  }
}
