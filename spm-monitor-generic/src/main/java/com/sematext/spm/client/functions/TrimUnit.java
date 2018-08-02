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
 * Trims the specified unit string from the end of value of the metricName and returns Long.
 * e.g.TrimUnit(Value,ms) - trims `ms` from  the end of value of metric name `Value`
 */
public class TrimUnit implements CalculationFunction {
  @Override public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    if (params != null && params.length == 2) {
      String metricName = params[0].toString();
      String unit = params[1].toString();
      String autocommitMaxTimeStr = (String) metrics.get(metricName);
      return Long.parseLong(autocommitMaxTimeStr.substring(0, autocommitMaxTimeStr.indexOf(unit)).trim());
    } else {
      throw new IllegalArgumentException("Missing metric name and unit in params");
    }
  }

  @Override public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    throw new UnsupportedOperationException("Can't be used in tag context");
  }
}
