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

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.observation.CalculationFunction;

/**
 * Divides the values of metrics by each other, starting from the lft to right.
 * e.g. func:LongDivide(request.time,request.count)
 */
public class LongDivide implements CalculationFunction {
  private static final Log LOG = LogFactory.getLog(LongDivide.class);

  @Override public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    if (params != null && params.length >= 2) {
      String metricName = params[0].toString();
      Object metricValue = metrics.get(metricName);
      if (metricValue != null) {
        double total = ((Number) metricValue).doubleValue();
        for (int i = 1; i < params.length; i++) {
          Object divMetric = metrics.get(params[i].toString());
          if (divMetric == null) {
            LOG.warn("Cannot find " + params[i].toString() + " in metrics: " + metrics);
            return null;
          }
          double div = Double.parseDouble(String.valueOf(divMetric));
          if (div == 0) {
            return 0;
          }
          total = total / div;
        }
        return (long) total;
      } else {
        LOG.warn("Cannot find " + metricName + " in metrics: " + metrics);
        return null;
      }
    } else {
      throw new IllegalArgumentException("Missing metric name and multiplication factor in params");
    }
  }

  @Override public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    throw new UnsupportedOperationException("Can't be used in tag context");
  }
}
