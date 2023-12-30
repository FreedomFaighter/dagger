/*
 * Copyright (C) 2010 Google Inc.
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
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

public final class InjectionTest {
  @Test
  public void basicInjection() {
    class TestEntryPoint {
      @Inject Provider<G> gProvider;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides E provideE(F f) {
        return new E(f);
      }
      @Provides F provideF() {
        return new F();
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    ObjectGraph.createWith(new TestingLoader(), new TestModule()).inject(entryPoint);
    G g = entryPoint.gProvider.get();
    assertNotNull(g.a);
    assertNotNull(g.b);
    assertNotNull(g.c);
    assertNotNull(g.d);
    assertNotNull(g.e);
    assertNotNull(g.e.f);
  }

  static class A {
    @Inject A() {}
  }

  static class B {
    @Inject B() {}
  }

  @Singleton
  static class C {
    @Inject C() {}
  }

  @Singleton
  static class D {
    @Inject D() {}
  }

  static class E {
    F f;
    E(F f) {
      this.f = f;
    }
  }

  static class F {}

  static class G {
    @Inject A a;
    @Inject B b;
    C c;
    D d;
    @Inject E e;
    @Inject G(C c, D d) {
      this.c = c;
      this.d = d;
    }
  }

  @Test
  public void providerInjection() {
    class TestEntryPoint {
      @Inject Provider<A> aProvider;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    ObjectGraph.createWith(new TestingLoader(), new TestModule()).inject(entryPoint);

    assertNotNull(entryPoint.aProvider.get());
  }


  @Test
  public void singletons() {
    class TestEntryPoint {
      @Inject Provider<F> fProvider;
      @Inject Provider<I> iProvider;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides @Singleton F provideF() {
        return new F();
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    ObjectGraph.createWith(new TestingLoader(), new TestModule()).inject(entryPoint);
    assertSame(entryPoint.iProvider.get(), entryPoint.iProvider.get());
  }

  @Singleton
  static class I {
    @Inject I() {}
  }

  @Test
  public void bindingAnnotations() {
    final A one = new A();
    final A two = new A();

    class TestEntryPoint {
      @Inject A a;
      @Inject @Named("one") A aOne;
      @Inject @Named("two") A aTwo;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides @Named("one") A getOne() {
        return one;
      }
      @Provides @Named("two") A getTwo() {
        return two;
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    ObjectGraph.createWith(new TestingLoader(), new TestModule()).inject(entryPoint);
    assertNotNull(entryPoint.a);
    assertSame(one, entryPoint.aOne);
    assertSame(two, entryPoint.aTwo);
  }

  @Test
  public void singletonBindingAnnotationAndProvider() {
    class TestEntryPoint {
      @Inject Provider<L> lProvider;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      A a1;
      A a2;

      @Provides @Singleton @Named("one") F provideF(Provider<A> aProvider) {
        a1 = aProvider.get();
        a2 = aProvider.get();
        return new F();
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    TestModule module = new TestModule();
    ObjectGraph.createWith(new TestingLoader(), module).inject(entryPoint);
    entryPoint.lProvider.get();

    assertNotNull(module.a1);
    assertNotNull(module.a2);
    assertNotSame(module.a1, module.a2);
    assertSame(entryPoint.lProvider.get(), entryPoint.lProvider.get());
  }

  @Singleton
  public static class L {
    @Inject @Named("one") F f;
    @Inject Provider<L> lProvider;
  }

  @Test
  public void singletonInGraph() {
    class TestEntryPoint {
      @Inject N n1;
      @Inject N n2;
      @Inject F f1;
      @Inject F f2;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides @Singleton F provideF() {
        return new F();
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    ObjectGraph.createWith(new TestingLoader(), new TestModule()).inject(entryPoint);

    assertSame(entryPoint.f1, entryPoint.f2);
    assertSame(entryPoint.f1, entryPoint.n1.f1);
    assertSame(entryPoint.f1, entryPoint.n1.f2);
    assertSame(entryPoint.f1, entryPoint.n2.f1);
    assertSame(entryPoint.f1, entryPoint.n2.f2);
    assertSame(entryPoint.f1, entryPoint.n1.fProvider.get());
    assertSame(entryPoint.f1, entryPoint.n2.fProvider.get());
  }

  public static class N {
    @Inject F f1;
    @Inject F f2;
    @Inject Provider<F> fProvider;
  }

  @Test
  public void noJitBindingsForAnnotations() {
    class TestEntryPoint {
      @Inject @Named("a") A a;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    try {
      graph.validate();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @Test
  public void injectableSupertypes() {
    class TestEntryPoint {
      @Inject Q q;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides F provideF() {
        return new F();
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    ObjectGraph.createWith(new TestingLoader(), new TestModule()).inject(entryPoint);
    assertNotNull(entryPoint.q.f);
  }

  @Test
  public void uninjectableSupertypes() {
    class TestEntryPoint {
      @Inject T t;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    ObjectGraph.createWith(new TestingLoader(), new TestModule()).inject(entryPoint);
    assertNotNull(entryPoint.t);
  }

  public static class P {
    @Inject F f;
  }

  public static class Q extends P {
    @Inject Q() {}
  }

  static class S {
  }

  public static class T extends S {
    @Inject T() {}
  }

  @Test
  public void singletonsAreNotEager() {
    class TestEntryPoint {
      @Inject Provider<A> aProvider;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      boolean sInjected = false;

      @Provides F provideF(R r) {
        return new F();
      }

      @Provides @Singleton S provideS() {
        sInjected = true;
        return new S();
      }
    }

    R.injected = false;
    TestEntryPoint entryPoint = new TestEntryPoint();
    TestModule module = new TestModule();
    ObjectGraph.createWith(new TestingLoader(), module).inject(entryPoint);

    assertFalse(R.injected);
    assertFalse(module.sInjected);
  }

  @Singleton
  static class R {
    static boolean injected = false;
    @Inject R() {
      injected = true;
    }
  }

  @Test
  public void providesSet() {
    final Set<A> set = Collections.emptySet();

    class TestEntryPoint {
      @Inject Set<A> set;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides Set<A> provideSet() {
        return set;
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    TestModule module = new TestModule();
    ObjectGraph.createWith(new TestingLoader(), module).inject(entryPoint);

    assertSame(entryPoint.set, set);
  }

  @Test
  public void providesSetValues() {
    final Set<A> set = Collections.emptySet();

    class TestEntryPoint {
      @Inject Set<A> set;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides(type = Provides.Type.SET_VALUES) Set<A> provideSet() {
        return set;
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    TestModule module = new TestModule();
    ObjectGraph.createWith(new TestingLoader(), module).inject(entryPoint);

    // copies into immutable collection
    assertNotSame(entryPoint.set, set);
    assertEquals(entryPoint.set, set);
  }

  @Test
  public void providerMethodsConflict() {
    @Module
    class TestModule {
      @Provides A provideA1() {
        throw new AssertionError();
      }
      @Provides A provideA2() {
        throw new AssertionError();
      }
    }

    try {
      ObjectGraph.createWith(new TestingLoader(), new TestModule());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void providesSetConflictsWithProvidesTypeSet() {
    @Module
    class TestModule {
      @Provides(type = Provides.Type.SET) A provideSetElement() {
        throw new AssertionError();
      }
      @Provides Set<A> provideSet() {
        throw new AssertionError();
      }
    }

    try {
      ObjectGraph.createWith(new TestingLoader(), new TestModule());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void providesSetConflictsWithProvidesTypeSetValues() {
    @Module
    class TestModule {
      @Provides(type = Provides.Type.SET_VALUES) Set<A> provideSetContribution() {
        throw new AssertionError();
      }
      @Provides Set<A> provideSet() {
        throw new AssertionError();
      }
    }

    try {
      ObjectGraph.createWith(new TestingLoader(), new TestModule());
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void providesSetOfProvidersIsDifferentThanProvidesTypeSetValues() {
    final Set<A> set = Collections.emptySet();
    final Set<Provider<A>> providers = Collections.emptySet();

    class TestEntryPoint {
      @Inject Set<A> set;
      @Inject Set<Provider<A>> providers;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides(type = Provides.Type.SET_VALUES) Set<A> provideSetContribution() {
        return set;
      }
      @Provides Set<Provider<A>> provideProviders() {
        return providers;
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    TestModule module = new TestModule();
    ObjectGraph.createWith(new TestingLoader(), module).inject(entryPoint);

    // copies into immutable collection
    assertNotSame(entryPoint.set, set);
    assertEquals(entryPoint.set, set);
    assertSame(entryPoint.providers, providers);
  }

  @Test
  public void singletonsInjectedOnlyIntoProviders() {
    class TestEntryPoint {
      @Inject Provider<A> aProvider;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides @Singleton A provideA() {
        return new A();
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    ObjectGraph.createWith(new TestingLoader(), new TestModule()).inject(entryPoint);
    assertSame(entryPoint.aProvider.get(), entryPoint.aProvider.get());
  }

  @Test
  public void moduleOverrides() {
    class TestEntryPoint {
      @Inject Provider<E> eProvider;
    }

    @Module(injects = TestEntryPoint.class)
    class BaseModule {
      @Provides F provideF() {
        throw new AssertionError();
      }
      @Provides E provideE(F f) {
        return new E(f);
      }
    }

    @Module(overrides = true)
    class OverridesModule {
      @Provides F provideF() {
        return new F();
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    ObjectGraph.createWith(new TestingLoader(), new BaseModule(), new OverridesModule()).inject(entryPoint);
    E e = entryPoint.eProvider.get();
    assertNotNull(e);
    assertNotNull(e.f);
  }

  @Test
  public void noJitBindingsForInterfaces() {
    class TestEntryPoint {
      @Inject RandomAccess randomAccess;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    try {
      graph.validate();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @Test
  public void objectGraphGetInterface() {
    final Runnable runnable = new Runnable() {
      @Override public void run() {
      }
    };

    @Module(injects = Runnable.class)
    class TestModule {
      @Provides Runnable provideRunnable() {
        return runnable;
      }
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    graph.validate();
    assertSame(graph.get(Runnable.class), runnable);
  }

  @Test
  public void noProvideBindingsForAbstractClasses() {
    class TestEntryPoint {
      @Inject AbstractList abstractList;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    try {
      graph.validate();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  static class ExtendsParameterizedType extends AbstractList<Integer> {
    @Inject String string;
    @Override public Integer get(int i) {
      throw new AssertionError();
    }
    @Override public int size() {
      throw new AssertionError();
    }
  }

  /**
   * We've had bugs where we look for the wrong keys when a class extends a
   * parameterized class. Explicitly test that we can inject such classes.
   */
  @Test
  public void extendsParameterizedType() {
    class TestEntryPoint {
      @Inject ExtendsParameterizedType extendsParameterizedType;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides String provideString() {
        return "injected";
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    ObjectGraph.createWith(new TestingLoader(), new TestModule()).inject(entryPoint);
    assertEquals(entryPoint.extendsParameterizedType.string, "injected");
  }

  @Test
  public void injectParameterizedType() {
    class TestEntryPoint {
      @Inject List<String> listOfStrings;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides List<String> provideList() {
        return Arrays.asList("a", "b");
      }
    }

    TestEntryPoint entryPoint = new TestEntryPoint();
    ObjectGraph.createWith(new TestingLoader(), new TestModule()).inject(entryPoint);
    assertEquals(entryPoint.listOfStrings, Arrays.asList("a", "b"));
  }

  @Test
  public void injectWildcardType() {
    class TestEntryPoint {
      @Inject List<? extends Number> listOfNumbers;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides List<? extends Number> provideList() {
        return Arrays.asList(1, 2);
      }
    }

    try {
      ObjectGraph.createWith(new TestingLoader(), new TestModule());
      fail();
    } catch (UnsupportedOperationException expected) {
    }
  }

  static class Parameterized<T> {
      @Inject String string;
    }

  @Test
  public void noConstructorInjectionsForClassesWithTypeParameters() {

    class TestEntryPoint {
      @Inject Parameterized<Long> parameterized;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides String provideString() {
        return "injected";
      }
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    try {
      graph.validate();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @Test
  public void moduleWithNoProvidesMethods() {
    @Module
    class TestModule {
    }

    ObjectGraph.createWith(new TestingLoader(), new TestModule());
  }

  @Test
  public void getInstance() {
    final AtomicInteger next = new AtomicInteger(0);

    @Module(injects = Integer.class)
    class TestModule {
      @Provides Integer provideInteger() {
        return next.getAndIncrement();
      }
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    assertEquals((int) graph.get(Integer.class), 0);
    assertEquals((int) graph.get(Integer.class), 1);
  }

  @Test
  public void getInstanceRequiresEntryPoint() {
    @Module
    class TestModule {
      @Provides Integer provideInteger() {
        throw new AssertionError();
      }
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    try {
      graph.get(Integer.class);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void getInstanceOfPrimitive() {
    @Module(injects = int.class)
    class TestModule {
      @Provides int provideInt() {
        return 1;
      }
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    assertEquals(1, (int) graph.get(int.class));
  }

  @Test
  public void getInstanceOfArray() {
    @Module(injects = int[].class)
    class TestModule {
      @Provides int[] provideIntArray() {
        return new int[] { 1, 2, 3 };
      }
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    assertEquals("[1, 2, 3]", Arrays.toString(graph.get(int[].class)));
  }

  @Test
  public void getInstanceAndInjectMembersUseDifferentKeys() {
    class BoundTwoWays {
      @Inject String s;
    }

    @Module(injects = BoundTwoWays.class)
    class TestModule {
      @Provides
      BoundTwoWays provideBoundTwoWays() {
        BoundTwoWays result = new BoundTwoWays();
        result.s = "Pepsi";
        return result;
      }

      @Provides String provideString() {
        return "Coke";
      }
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    BoundTwoWays provided = graph.get(BoundTwoWays.class);
    assertEquals("Pepsi", provided.s);

    BoundTwoWays membersInjected = new BoundTwoWays();
    graph.inject(membersInjected);
    assertEquals("Coke", membersInjected.s);
  }

  static class NoInjections {
    NoInjections(Void noDefaultConstructorEither) {
    }
  }

  @Test
  public void entryPointNeedsNoInjectAnnotation() {
    @Module(injects = NoInjections.class)
    class TestModule {
    }

    ObjectGraph.createWith(new TestingLoader(), new TestModule()).validate();
  }

  static class InjectMembersOnly {
    InjectMembersOnly(Void noInjectableConstructor) {
    }
    @Inject String string;
  }

  @Test
  public void cannotGetOnMembersOnlyInjectionPoint() {
    @Module(injects = InjectMembersOnly.class)
    class TestModule {
      @Provides String provideString() {
        return "injected";
      }
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    try {
      graph.get(InjectMembersOnly.class);
      fail();
    } catch (IllegalStateException expected) {
    }

    InjectMembersOnly instance = new InjectMembersOnly(null);
    graph.inject(instance);
    assertEquals(instance.string, "injected");
  }

  @Test
  public void nonEntryPointNeedsInjectAnnotation() {
    @Module
    class TestModule {
      @Provides String provideString(NoInjections noInjections) {
        throw new AssertionError();
      }
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    try {
      graph.validate();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  static class TwoAtInjectConstructors {
    @Inject TwoAtInjectConstructors() {
    }
    @Inject TwoAtInjectConstructors(String s) {
    }
  }

  @Test
  public void twoAtInjectConstructorsIsRejected() {
    @Module(injects = TwoAtInjectConstructors.class)
    class TestModule {
      @Provides String provideString() {
        throw new AssertionError();
      }
    }

    ObjectGraph graph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    try {
      graph.validate();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  @Test
  public void runtimeProvidesMethodsExceptionsAreNotWrapped() {
    class TestEntryPoint {
      @Inject String string;
    }

    @Module(injects = TestEntryPoint.class)
    class TestModule {
      @Provides String provideString() {
        throw new ClassCastException("foo");
      }
    }

    try {
      ObjectGraph.createWith(new TestingLoader(), new TestModule()).inject(new TestEntryPoint());
      fail();
    } catch (ClassCastException e) {
      assertEquals(e.getMessage(), "foo");
    }
  }

  static class ThrowsOnConstruction {
    @Inject ThrowsOnConstruction() {
      throw new ClassCastException("foo");
    }
  }

  @Test
  public void runtimeConstructorExceptionsAreNotWrapped() {
    @Module(injects = ThrowsOnConstruction.class)
    class TestModule {
    }

    try {
      ObjectGraph.createWith(new TestingLoader(), new TestModule()).get(ThrowsOnConstruction.class);
      fail();
    } catch (ClassCastException e) {
      assertEquals(e.getMessage(), "foo");
    }
  }

  static class SingletonLinkedFromExtension {
    @Inject C c; // Singleton.
  }

  @Module(complete = false, injects = C.class)
  static class RootModule { }

  @Module(addsTo = RootModule.class, injects = SingletonLinkedFromExtension.class)
  static class ExtensionModule { }

  @Test
  public void testSingletonLinkingThroughExtensionGraph() {
    ObjectGraph root = ObjectGraph.createWith(new TestingLoader(), new RootModule());
    // DO NOT CALL root.get(C.class)) HERE to get forced-linking behaviour from plus();
    ObjectGraph extension = root.plus(new ExtensionModule());
    assertSame(extension.get(SingletonLinkedFromExtension.class).c, root.get(C.class));
  }

  @Test
  public void privateFieldsFail() {
    class Test {
      @Inject private Object nope;
    }

    @Module(injects = Test.class)
    class TestModule {
      @Provides Object provideObject() {
        return null;
      }
    }

    try {
      ObjectGraph.createWith(new TestingLoader(), new TestModule()).inject(new Test());
      fail();
    } catch (IllegalStateException e) {
      assertTrue(e.getMessage().contains("Can't inject private field: "));
    }
  }

  @Test
  public void privateConstructorsFail() {
    class Test {
      @Inject private Test() {
      }
    }

    @Module(injects = Test.class)
    class TestModule {
    }

    try {
      ObjectGraph.createWith(new TestingLoader(), new TestModule()).get(Test.class);
      fail();
    } catch (IllegalStateException e) {
      assertTrue(e.getMessage().contains("Can't inject private constructor: "));
    }
  }

  /** https://github.com/square/dagger/issues/231 */
  @Test
  public void atInjectAlwaysRequiredForConstruction() {
    @Module(injects = ArrayList.class)
    class TestModule {
    }

    ObjectGraph objectGraph = ObjectGraph.createWith(new TestingLoader(), new TestModule());
    objectGraph.validate();
    try {
      objectGraph.get(ArrayList.class);
      fail();
    } catch (IllegalStateException e) {
      assertTrue(e.getMessage().contains("Unable to create binding for java.util.ArrayList"));
    }
  }
}
