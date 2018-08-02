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
package com.sematext.spm.client.unlogger.utils;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javassist.ClassPool;

public final class ClassPoolStatistics {
  private ClassPoolStatistics() {
  }

  private static final Set<ClassPool> pools = new HashSet<ClassPool>();

  public static void update(ClassPool pool) {
    pools.add(pool);
  }

  public static String dump() {
    try {
      String stats = "";
      for (ClassPool pool : pools) {
        Field field = ClassPool.class.getDeclaredField("classes");
        Hashtable table = (Hashtable) field.get(field);
        stats += pool + " -> " + table.size() + " classes loaded\n";
      }
      return stats;
    } catch (Exception e) {
      return "";
    }
  }
}
