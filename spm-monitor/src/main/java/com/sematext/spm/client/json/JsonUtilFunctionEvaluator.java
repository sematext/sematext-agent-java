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
package com.sematext.spm.client.json;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public final class JsonUtilFunctionEvaluator {
  private JsonUtilFunctionEvaluator() {}

  public static Object evaluateFunction(String node, Object element) {
    Collection elementCollection;
    if (element instanceof Collection) {
      elementCollection = (Collection) element;
    } else if (element instanceof Map) {
      elementCollection = ((Map) element).values();
    } else {
      throw new UnsupportedOperationException(
          String.format(
              "Cannot evaluate function %s. Functions are allowed only on collections and maps, %s is not allowed",
              node, element.getClass()));
    }

    if (elementCollection.isEmpty()) {
      return null;
    }
    
    String function = node.substring(0, node.indexOf("(")).trim();
    Object result;
    if ("length".equals(function)) {
      result = elementCollection.size();
    } else if ("max".equals(function)) {
      result = Collections.max(elementCollection);
    } else if ("min".equals(function)) {
      result = Collections.min(elementCollection);
    } else if ("sum".equals(function)) {
      result = summarizeElements(node, elementCollection);
    } else if ("avg".equals(function)) {
      result = summarizeElements(node, elementCollection) / elementCollection.size();
    } else {
      throw new UnsupportedOperationException(String.format("Unknown function %s", node));
    }
    return result;
  }
  
  private static double summarizeElements(String node, Collection elementList) {
    double tmpSum = 0d;
    for (Object e : elementList) {
      if (e instanceof Number) {
        tmpSum += ((Number) e).doubleValue();
      } else {
        throw new IllegalArgumentException("For node " + node + " found element which is not a Number : " + e +
            ", class: " + e.getClass());
      }
    }
    return tmpSum;
  }
}
