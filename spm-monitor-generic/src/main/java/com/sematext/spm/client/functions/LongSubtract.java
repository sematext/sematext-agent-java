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
 * Subtract value of one or more metrics from another.
 * e.g. func:LongSubtract(batches.received.processStartTime,batches.received.submissionTime)
 */
public class LongSubtract implements CalculationFunction {
  private static final Log LOG = LogFactory.getLog(LongSubtract.class);

  @Override public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    if (params != null && params.length >= 2) {
      String metricName = params[0].toString();
      Object metricValue = metrics.get(metricName);
      if (metricValue != null) {
        double total = ((Number) metricValue).doubleValue();
        for (int i = 1; i < params.length; i++) {
          Object subMetric = metrics.get(params[i].toString());
          if (subMetric == null) {
            LOG.warn("Cannot find subtrahend " + i + " [" + params[i].toString() + "] in metrics: "
                         + metrics + ". Assuming zero");
          } else {
            total = total - Double.parseDouble(String.valueOf(subMetric));
          }
        }
        return (long) total;
      } else {
        LOG.warn("Cannot find " + metricName + " in metrics: " + metrics);
        return null;
      }
    } else {
      throw new IllegalArgumentException("Too few metrics provided for subtraction [" + (params != null ?
          params.length : "null") + "]. At least 2 metric names have to be provided");
    }
  }

  @Override public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    throw new UnsupportedOperationException("Can't be used in tag context");
  }
}
