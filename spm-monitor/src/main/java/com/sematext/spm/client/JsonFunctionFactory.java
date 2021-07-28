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
package com.sematext.spm.client;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;

public final class JsonFunctionFactory {
  private static Map<String, JsonFunction> JSON_FUNCTIONS_MAP = new UnifiedMap<String, JsonFunction>();

  private JsonFunctionFactory() {}

  public static JsonFunction getFunction(String name) {
    if (!JSON_FUNCTIONS_MAP.containsKey(name)) {
      try {
        String className = name;

        // if the classname does not contain a package, prepend the default package
        if (!className.contains(".")) {
          className = "com.sematext.spm.client.functions.json." + className;
        }

        Class<?> c = Class.forName(className);
        JsonFunction function = (JsonFunction) c.newInstance();
        JSON_FUNCTIONS_MAP.put(name, function);
      } catch (Throwable thr) {
        JSON_FUNCTIONS_MAP.put(name, null);
        throw new UnsupportedOperationException(String.format("Unknown function %s", name));
      }
    }

    return JSON_FUNCTIONS_MAP.get(name);
  }
}
