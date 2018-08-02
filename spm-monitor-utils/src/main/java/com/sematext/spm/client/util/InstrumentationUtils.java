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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtField;

public final class InstrumentationUtils {
  private InstrumentationUtils() {
  }

  public static byte[] getAsBytes(Class<?> klass) {
    final InputStream inputStream = klass.getClassLoader()
        .getResourceAsStream(klass.getName().replace(".", "/") + ".class");
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      IOUtils.copy(inputStream, os);
      return os.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Can't convert class " + klass + " to bytes.", e);
    }
  }

  private static void getHierarchy(Set<String> hierarchy, CtClass klass) throws Exception {
    if (klass == null) {
      return;
    }
    hierarchy.add(klass.getName());
    getHierarchy(hierarchy, klass.getSuperclass());
    for (CtClass iface : klass.getInterfaces()) {
      getHierarchy(hierarchy, iface);
    }
  }

  public static Set<String> getHierarchy(CtClass klass) throws Exception {
    final Set<String> hierarchy = new HashSet<String>();
    getHierarchy(hierarchy, klass);
    return hierarchy;
  }

  public static CtField addField(CtClass ctClass, CtClass type, String fieldName, int modifiers)
      throws CannotCompileException {
    final CtField field = new CtField(type, fieldName, ctClass);
    field.setModifiers(modifiers);
    ctClass.addField(field);
    return field;
  }

  public static void addField(CtClass ctClass, CtClass type, String fieldName, String initializer, int modifiers)
      throws CannotCompileException {
    final CtField field = new CtField(type, fieldName, ctClass);
    field.setModifiers(modifiers);
    ctClass.addField(field, initializer);
  }

}
