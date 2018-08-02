/*
 * Licensed to Sematext Group, Inc
 *
 * See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Sematext Group, Inc licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sematext.spm.client.util;

import static java.util.Arrays.asList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Our own version of reflection utils. The 'commons' not used, due to it can be used by monitored app too.
 */
public final class ReflectionUtils {

  private ReflectionUtils() {
    // It's utility class, can't be instantiated.
  }

  public static <T> T instance(Class<T> type, Class<?>[] constructorTypes, Object[] constructorParams) {
    try {
      Constructor<T> constructor = type.getDeclaredConstructor(constructorTypes);
      return constructor.newInstance(constructorParams);
    } catch (Exception e) {
      return null;
    }
  }

  public static <T> T instance(Class<T> type, Object[] constructorParams) {
    try {
      Constructor<T> constructor = (Constructor<T>) type.getDeclaredConstructors()[0];
      return constructor.newInstance(constructorParams);
    } catch (Exception e) {
      return null;
    }
  }

  public static <T> T instance(Class<T> type, Class<?> constructorType, Object constrcutorParam) {
    return instance(type, new Class<?>[] { constructorType }, new Object[] { constrcutorParam });
  }

  public static <T> T instance(Class<T> type, ClassValue<?>... cv) {
    return instance(type, asList(cv));
  }

  public static <T> T instance(Class<T> type, Collection<ClassValue<?>> cv) {
    Class<?>[] constructorParamTypes = new Class<?>[cv.size()];
    Object[] constructorParamValues = new Object[cv.size()];

    int i = 0;
    for (ClassValue<?> classValue : cv) {
      constructorParamTypes[i] = classValue.type;
      constructorParamValues[i] = classValue.value;
      i++;
    }
    return instance(type, constructorParamTypes, constructorParamValues);
  }

  public static <T> T instance(Class<T> type) {
    try {
      return type.newInstance();
    } catch (Exception e) {
      return null;
    }
  }

  public static Method getMethod(Class<?> claxx, String methodName, Class<?>... paramTypes) {
    try {
      Method method;
      try {
        method = claxx.getMethod(methodName, paramTypes);
      } catch (NoSuchMethodException e) {
        method = claxx.getDeclaredMethod(methodName, paramTypes);
      }
      method.setAccessible(true);
      return method;
    } catch (Exception e) {
      return null;
    }
  }

  public static Field getField(Class<?> claxx, String fieldName) {
    try {
      Field field;
      try {
        field = claxx.getField(fieldName);
      } catch (NoSuchFieldException e) {
        field = claxx.getDeclaredField(fieldName);
      }
      field.setAccessible(true);
      return field;
    } catch (Exception e) {
      if (claxx.getSuperclass() == null) {
        return null;
      }
      return getField(claxx.getSuperclass(), fieldName);
    }
  }

  public static <T> T silentMethodGet(Object object, String methodName) {
    if (object == null) {
      return null;
    }
    try {
      Method method = getMethod(object.getClass(), methodName);
      if (method == null) {
        return null;
      }
      return (T) method.invoke(object);

    } catch (Exception e) {
      return null;
    }
  }

  public static boolean silentFieldSet(Object instance, String fieldName, Object value) {
    if (instance == null) {
      return false;
    }
    Class<?> klass = instance.getClass();
    Field field = getField(klass, fieldName);
    if (field == null) {
      return false;
    }
    try {
      field.set(instance, value);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static Object silentFieldGet(Object instance, String fieldName) {
    if (instance == null) {
      return null;
    }
    Class<?> klass = instance.getClass();
    Field field = getField(klass, fieldName);
    if (field == null) {
      return null;
    }
    try {
      return field.get(instance);
    } catch (Exception e) {
      return null;
    }
  }

  public static Object silentInvoke(Method method, Object instance, Object... params) {
    try {
      return method.invoke(instance, params);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (Exception e) {
      return null;
    }
  }

  public static Class<?> silentGetClass(String className, ClassLoader classLoader) {
    try {
      return classLoader.loadClass(className);
    } catch (Exception e) {
      return null;
    }
  }

  public static Class<?>[] silentGetClasses(Collection<String> classNames, ClassLoader classLoader) {
    Class<?>[] classes = new Class<?>[classNames.size()];
    int i = 0;
    for (String className : classNames) {
      Class<?> claxx = silentGetClass(className, classLoader);
      if (claxx == null) {
        return null;
      }
      classes[i] = claxx;
      i++;
    }
    return classes;
  }

  public static List<Annotation> getAnnotatedAnnotations(Class<?> type,
                                                         Class<? extends Annotation> anotationForAnotation) {
    List<Annotation> res = new ArrayList<Annotation>();
    for (Annotation annotation : type.getAnnotations()) {
      if (annotation.annotationType().isAnnotationPresent(anotationForAnotation)) {
        res.add(annotation);
      }
    }
    return res;
  }

  public static final class ClassValue<T> {

    private final Class<T> type;
    private final T value;

    private ClassValue(Class<T> type, T value) {
      this.type = type;
      this.value = value;
    }

    public static ClassValue<Object> cvu(Class<?> type, Object val) {
      return new ClassValue<Object>((Class<Object>) type, val);
    }

    public static <T> ClassValue<T> cv(Class<T> type, T val) {
      return new ClassValue<T>(type, val);
    }

  }

}
