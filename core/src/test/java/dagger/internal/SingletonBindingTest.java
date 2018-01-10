/**
 * Copyright (C) 2013 Square, Inc.
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

package dagger.internal;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SingletonBindingTest {
  private static Binding<String> wrappedBinding;
  private static Binding<String> singletonBinding;

  @BeforeAll
  public static void setUp() {
    wrappedBinding = new StringBinding();
    singletonBinding = Linker.scope(wrappedBinding);
  }

  @Test
  public void testSingletonBindingIsSingleton() {
    assertTrue(singletonBinding.isSingleton());
  }

  // This next batch of tests validates that SingletonBinding consistently delegates to the wrapped binding for state.
  @Test
  public void testSingletonBindingDelegatesSetLinked() {
    singletonBinding.setLinked();
    assertTrue(wrappedBinding.isLinked());
  }

  @Test
  public void testSingletonBindingDelegatesIsLinked() {
    wrappedBinding.setLinked();
    assertTrue(singletonBinding.isLinked());
  }

  @Test
  public void testSingletonBindingDelegatesSetVisiting() {
    singletonBinding.setVisiting(true);
    assertTrue(wrappedBinding.isVisiting());
  }

  @Test
  public void testSingletonBindingDelegatesIsVisiting() {
    wrappedBinding.setVisiting(true);
    assertTrue(singletonBinding.isVisiting());
  }

  @Test
  public void testSingletonBindingDelegatesSetCycleFree() {
    singletonBinding.setCycleFree(true);
    assertTrue(wrappedBinding.isCycleFree());
  }

  @Test
  public void testSingletonBindingDelegatesIsCycleFree() {
    wrappedBinding.setCycleFree(true);
    assertTrue(singletonBinding.isCycleFree());
  }

  private static class StringBinding extends Binding<String> {
    private StringBinding() {
      super("dummy", "dummy", true, "dummy"); // 3rd arg true => singleton
    }

  }
}
