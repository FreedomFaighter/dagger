/**
 * Copyright (C) 2012 Square, Inc.
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

import dagger.Lazy;
import dagger.MembersInjector;
import dagger.Provides;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static dagger.Provides.Type.SET;

public final class KeysTest {
  int primitive;
  @Test
  public void lonePrimitiveGetsBoxed() throws NoSuchFieldException {
    assertEquals(fieldKey("primitive"), "java.lang.Integer");
  }

  Map<String, List<Integer>> mapStringListInteger;
  @Test
  public void parameterizedTypes() throws NoSuchFieldException {
    assertEquals(fieldKey("mapStringListInteger"), "java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>");
  }

  Map<String, int[]> mapStringArrayInt;
  @Test
  public void parameterizedTypeOfPrimitiveArray() throws NoSuchFieldException {
    assertEquals(fieldKey("mapStringArrayInt"), "java.util.Map<java.lang.String, int[]>");
  }

  @Named("foo") String annotatedType;
  @Test
  public void annotatedType() throws NoSuchFieldException {
    assertEquals(fieldKey("annotatedType"), "@jakarta.inject.Named(value=foo)/java.lang.String");
  }

  String className;
  @Test
  public void testGetClassName() throws NoSuchFieldException {
    assertEquals(Keys.getClassName(fieldKey("className")), "java.lang.String");
  }

  @Named("foo") String classNameWithAnnotation;
  @Test
  public void testGetClassNameWithoutAnnotation() throws NoSuchFieldException {
    assertEquals(Keys.getClassName(fieldKey("classNameWithAnnotation")), "java.lang.String");
  }

  String[] classNameArray;
  @Test
  public void testGetClassNameArray() throws NoSuchFieldException {
    assertNull(Keys.getClassName(fieldKey("classNameArray")));
  }

  List<String> classNameParameterized;
  @Test
  public void testGetClassParameterized() throws NoSuchFieldException {
    assertNull(Keys.getClassName(fieldKey("classNameParameterized")));
  }

  @Named("foo") String annotated;
  @Test
  public void testAnnotated() throws NoSuchFieldException {
    assertEquals(fieldKey("annotated"), "@jakarta.inject.Named(value=foo)/java.lang.String");
    assertTrue(Keys.isAnnotated(fieldKey("annotated")));
  }

  String notAnnotated;
  @Test
  public void testIsAnnotatedFalse() throws NoSuchFieldException {
    assertFalse(Keys.isAnnotated(fieldKey("notAnnotated")));
  }

  Provider<String> providerOfType;
  String providedType;
  @Test
  public void testGetDelegateKey() throws NoSuchFieldException {
    assertEquals(Keys.getBuiltInBindingsKey(fieldKey("providerOfType")), fieldKey("providedType"));
  }

  @Named("/@") Provider<String> providerOfTypeAnnotated;
  @Named("/@") String providedTypeAnnotated;
  @Test
  public void testGetDelegateKeyWithAnnotation() throws NoSuchFieldException {
    assertEquals(Keys.getBuiltInBindingsKey(fieldKey("providerOfTypeAnnotated")), fieldKey("providedTypeAnnotated"));
  }

  @Named("/@") MembersInjector<String> membersInjectorOfType;
  @Named("/@") String injectedType;
  @Test
  public void testGetDelegateKeyWithMembersInjector() throws NoSuchFieldException {
    assertEquals(Keys.getBuiltInBindingsKey(fieldKey("membersInjectorOfType")), "members/java.lang.String");
  }

  @Named("/@") Lazy<String> lazyAnnotatedString;
  @Named("/@") String eagerAnnotatedString;
  @Test
  public void testAnnotatedGetLazyKey() throws NoSuchFieldException {
    assertEquals(Keys.getLazyKey(fieldKey("lazyAnnotatedString")), fieldKey("eagerAnnotatedString"));
  }

  Lazy<String> lazyString;
  String eagerString;
  @Test
  public void testGetLazyKey() throws NoSuchFieldException {
    assertEquals(Keys.getLazyKey(fieldKey("lazyString")), fieldKey("eagerString"));
  }

  @Test
  public void testGetLazyKey_WrongKeyType() throws NoSuchFieldException {
    assertNull(Keys.getLazyKey(fieldKey("providerOfTypeAnnotated")));
  }

  @Provides(type=SET) String elementProvides() { return "foo"; }

  @Test
  public void testGetElementKey_NoQualifier() throws NoSuchMethodException {
    Method method = KeysTest.class.getDeclaredMethod("elementProvides", new Class<?>[]{});
    assertEquals(Keys.getSetKey(method.getGenericReturnType(), method.getAnnotations(), method), "java.util.Set<java.lang.String>");
  }

  @Named("foo")
  @Provides(type=SET) String qualifiedElementProvides() { return "foo"; }

  @Test
  public void testGetElementKey_WithQualifier() throws NoSuchMethodException {
    Method method = KeysTest.class.getDeclaredMethod("qualifiedElementProvides", new Class<?>[]{});
    assertEquals(Keys.getSetKey(method.getGenericReturnType(), method.getAnnotations(), method), "@jakarta.inject.Named(value=foo)/java.util.Set<java.lang.String>");
  }

  private String fieldKey(String fieldName) throws NoSuchFieldException {
    Field field = KeysTest.class.getDeclaredField(fieldName);
    return Keys.get(field.getGenericType(), field.getAnnotations(), field);
  }

}
