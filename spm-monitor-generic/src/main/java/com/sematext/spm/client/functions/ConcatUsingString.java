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
 * Concats the value of metrics specified the metric names using the string specific as first arg
 * e.g. func:ConcatUsingString(-,collection,shard,replica)
 */
public class ConcatUsingString implements CalculationFunction {

  @Override public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    return concat(metrics, params);
  }

  @Override public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    return concat(objectNameTags, params);
  }

  private <T> String concat(Map<String, T> values, Object... params) {
    StringBuilder builder = new StringBuilder();
    if (params != null && params.length > 1) {
      String concatChar = params[0].toString();
      for (int i = 1; i < params.length; i++) {
        String name = params[i].toString();
        if (values.get(name) != null) {
          builder.append(values.get(name)).append(concatChar);
        }
      }
      if (builder.length() > 0) {
        builder.deleteCharAt(builder.length() - 1);
      }
    } else {
      throw new IllegalArgumentException("Missing concat string and names");
    }
    return builder.toString();
  }

}
