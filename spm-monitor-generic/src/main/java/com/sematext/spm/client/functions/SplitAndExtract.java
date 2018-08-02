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
 * Splits the value of specified metric name using the token and returns the value at index
 * e.g. func:SplitAndExtract(host_port,-,1) - splits value of host port by - and returns first index.
 */
public class SplitAndExtract implements CalculationFunction {
  @Override public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    return splitAndExtract(metrics, params);
  }

  @Override public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    return splitAndExtract(objectNameTags, params);
  }

  private <T> String splitAndExtract(Map<String, T> values, Object... params) {
    if (params != null && params.length == 3) {
      String name = params[0].toString();
      String token = params[1].toString();
      int index = Integer.parseInt(params[2].toString());
      String value = values.get(name).toString();
      String result = value;
      if (value.contains(token)) {
        result = result.split(token)[index];
      }
      return result;
    } else {
      throw new IllegalArgumentException("Missing name, token and index params");
    }
  }
}
