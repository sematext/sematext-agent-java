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
package com.sematext.spm.client.instrumentation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class MixinCast {
  private MixinCast() {
  }

  @SuppressWarnings("unchecked")
  private static <T, K extends Class<T>> T newProxy(final Object from, final K klass, final String originalField)
      throws Exception {
    final Object handler = Proxy.getInvocationHandler(from);

    final Field field = handler.getClass().getDeclaredField(originalField);
    field.setAccessible(true);

    final Object original = field.get(handler);
    return (T) Proxy.newProxyInstance(from.getClass().getClassLoader(), new Class[] { klass }, new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(original, args);
      }
    });
  }

  public static <T, K extends Class<T>> T cast(Object from, K klass, String originalField) throws Exception {
    if (Proxy.isProxyClass(from.getClass())) {
      return newProxy(from, klass, originalField);
    }
    return klass.cast(from);
  }
}
