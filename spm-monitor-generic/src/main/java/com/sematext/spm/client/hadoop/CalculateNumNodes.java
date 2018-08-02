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

package com.sematext.spm.client.hadoop;

import org.apache.commons.lang.StringUtils;

import java.util.Map;

import com.sematext.spm.client.observation.CalculationFunction;

public class CalculateNumNodes implements CalculationFunction {
  @Override public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    if (metrics == null) {
      return 0;
    }

    if (params != null && params.length == 1) {
      String metricName = params[0].toString();

      if (metrics.get(metricName) == null) {
        return 0;
      }

      String str = String.valueOf(metrics.get(metricName));

      // format is: {} or {"af-slave1":{"numBlocks":193,"usedSpace":883417088,"lastContact":1,"capacity":104691462144,
      // "nonDfsUsedSpace":4381753344,"adminState":"In Service"},"af-slave2":{"numBlocks":193,"usedSpace":883253248,
      // "lastContact":1,"capacity":104691462144,"nonDfsUsedSpace":4620505088,"adminState":"In Service"}}

      // number of nodes is equal to number of "{" chars - 1
      return StringUtils.countMatches(str, "{") - 1;
    } else {
      throw new IllegalArgumentException("Missing metric name in params");
    }

  }

  @Override public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    throw new UnsupportedOperationException("Can't be used in tag context");
  }
}
