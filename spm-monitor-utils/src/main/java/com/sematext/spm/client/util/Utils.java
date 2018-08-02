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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Utils {
  private Utils() {
  }

  public static boolean equal(Object a, Object b) {
    return a == b || (a != null && a.equals(b));
  }

  public static <T, V> List<Tuple<T, List<V>>> groupByFirst(List<Tuple<T, V>> tuples) {
    Map<T, List<V>> groupedMap = new HashMap<T, List<V>>();
    for (Tuple<T, V> tuple : tuples) {
      List<V> group = groupedMap.get(tuple.getFirst());
      if (group == null) {
        group = new ArrayList<V>();
      }
      group.add(tuple.getSecond());
      groupedMap.put(tuple.getFirst(), group);
    }

    List<Tuple<T, List<V>>> groupedList = new ArrayList<Tuple<T, List<V>>>();
    for (T groupKey : groupedMap.keySet()) {
      groupedList.add(Tuple.tuple(groupKey, groupedMap.get(groupKey)));
    }
    return groupedList;
  }
}
