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
 * Converts bool value to 0 or 1
 */
public class BoolToInt implements CalculationFunction {
  @Override public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    if (params != null && params.length == 1) {
      String metricName = params[0].toString();
      Object metricValue = metrics.get(metricName);
      if (metricValue != null) {
        if (metricValue instanceof Boolean) {
          return (Boolean) metricValue ? 1 : 0;
        } else if (metricValue instanceof String) {
          return Boolean.parseBoolean((String) metricValue) ? 1 : 0;
        } else {
          throw new IllegalArgumentException(String.format("Value of unkwnown type: %s", metricValue));
        }
      } else {
        throw new IllegalArgumentException(String.format("Cannot find %s in metrics", metricName));
      }
    } else {
      throw new IllegalArgumentException("Missing metric name and multiplication factor in params");
    }
  }

  @Override public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    return null;
  }
}
