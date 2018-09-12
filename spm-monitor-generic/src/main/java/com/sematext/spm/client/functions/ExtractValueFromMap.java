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

package com.sematext.spm.client.functions;

import java.util.Map;

import com.sematext.spm.client.observation.CalculationFunction;

/**
 * Extracts value of key specified with function parameter. Function assumes monitored value
 * (from which resuting attribute is extracted) is in key-value format.
 */
public abstract class ExtractValueFromMap<T> implements CalculationFunction {
  private static final String MAP_SEPARATOR = ", ";
  private static final int LENGTH_OF_MAP_SEPARATOR = MAP_SEPARATOR.length();
  
  @Override public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    String value = extract(metrics, params);
    if (value == null) {
      return null;
    } else {
      return convertToResult(value);
    }
  }

  protected abstract T convertToResult(String value);
  
  @Override public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    throw new UnsupportedOperationException("Can't be used in tag context");
  }

  private <T> String extract(Map<String, T> values, Object... params) {
    if (params != null && (params.length == 2 || params.length == 3)) {
      String mapAttribName = params[0].toString();
      String key = params[1].toString();
      Object mapObject = values.get(mapAttribName);
      
      if (mapObject == null) {
        return null;
      }
      
      return findValue(mapObject, key);
    } else {
      throw new IllegalArgumentException("Two parameters are mandatory: monitored attribute name and key");
    }
  }

  private String findValue(Object mapObject, String metricKey) {
    // example: {lookups=0, evictions=0, cumulative_inserts=0, cumulative_hits=0, hits=0, cumulative_evictions=0, size=0, hitratio=0.0, cumulative_lookups=0, cumulative_hitratio=0.0, warmupTime=0, inserts=0}
    String map = mapObject.toString().trim();
    
    if (map.startsWith("{") && map.endsWith("}")) {
      map = map.substring(1, map.length() - 1);
    }
    
    while (map != null && !map.isEmpty()) {
      int indexOfNext = map.indexOf(MAP_SEPARATOR);
      String currentKeyValue;
      if (indexOfNext == -1) {
        currentKeyValue = map;
        map = null;
      } else {
        currentKeyValue = map.substring(0, indexOfNext);
        map = map.substring(indexOfNext + LENGTH_OF_MAP_SEPARATOR).trim();
      }
      
      int indexOfEquals = currentKeyValue.indexOf('=');
      String key = currentKeyValue.substring(0, indexOfEquals).trim();
      
      if (key.equals(metricKey)) {
        return currentKeyValue.substring(indexOfEquals + 1).trim();
      }
    }
    
    return null;
  }
}
