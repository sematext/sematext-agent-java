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

/**
 * Trims the specified units string from the end of value of the metricName and returns Long.
 * Can specify multiple units string, if the value can be in multiple units
 * e.g.LongTrimUnit(Value,ms) - trims `ms` from  the end of value of metric name `Value`
 * e.g.LongTrimUnit(Value,s,ms) - trims `ms` or `s` (whichever is longest match) from  the end of value of metric name `Value`
 */
public class LongTrimUnit extends TrimUnit {
  @Override public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    if (params != null && params.length > 1) {
      String metricName = params[0].toString();
      String value = (String) metrics.get(metricName);
      String[] unitsToTrim = getUnits(params);
      for (String unit : unitsToTrim) {
        if (value.contains(unit)) {
          return Long.parseLong(value.substring(0, value.indexOf(unit)).trim());
        }
      }
      throw new IllegalArgumentException(String.format("Cannot find units in value %s for %s", value, metricName));
    } else {
      throw new IllegalArgumentException("Missing metric name and unit in params");
    }
  }

  @Override public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    throw new UnsupportedOperationException("Can't be used in tag context");
  }
}
