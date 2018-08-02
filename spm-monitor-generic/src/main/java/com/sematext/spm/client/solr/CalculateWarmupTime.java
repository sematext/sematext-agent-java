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

package com.sematext.spm.client.solr;

import java.util.Map;

import com.sematext.spm.client.observation.CalculationFunction;

public class CalculateWarmupTime implements CalculationFunction {
  // has a state, therefore its derivedAttrib has to be declared as stateful when used in config
  private String searcherName;

  @Override
  public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    throw new UnsupportedOperationException("Can't be used in tag context");
  }

  @Override
  public Number calculateAttribute(Map<String, Object> metrics, Object... params) {

    if (params != null && params.length == 2) {
      // the logic is: if searcherName changed, then there had to be a warmup and whatever warmupTime value is, it should be
      // reported; if searcherName didn't change, warmupTime could still be > 0, but we should ignore it (it is not a sign of
      // new warmup, it is just old, already reported recording)
      String searcherMetricName = params[1].toString();
      String newSearcherName = (String) metrics.get(searcherMetricName);

      if (searcherName == null) {
        // first time this runs should just record current searcher name (warmup happened at some point in the past, we
        // shouldn't report it as if it happened now)
        searcherName = newSearcherName;
        return null;
      }

      if (newSearcherName != null && !newSearcherName.equals(searcherName)) {
        searcherName = newSearcherName;
        String metricName = params[0].toString();
        return (Long) metrics.get(metricName);
      } else {
        return null;
      }
    } else {
      throw new IllegalArgumentException("Missing metric name(s) in params");
    }
  }
}
