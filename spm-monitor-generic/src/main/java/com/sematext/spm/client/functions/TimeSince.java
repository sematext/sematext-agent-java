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
 * Returns time in ms passed since event was accured. So simple calculate now()-eventTime
 */
public class TimeSince implements CalculationFunction {
  private static final Log LOG = LogFactory.getLog(TimeSince.class);
  
  @Override
  public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    if (params != null && params.length == 1) {
      String metricName = params[0].toString();
      Object metricValue = metrics.get(metricName);
      if (metricValue != null) {
        if (metricValue instanceof Long) {
          long timeSinceNow = System.currentTimeMillis() - (Long) metricValue;
          //we don't want to see negative delay if there is clocks synchronization problem
          return timeSinceNow < 0 ? 0 : timeSinceNow;
        } else {
          LOG.warn(String.format("Value of unsupported type: %s", metricValue));
          return null;
        }
      } else {
        LOG.warn(String.format("Cannot find %s in metrics", metricName));
        return null;
      }
    } else {
      throw new IllegalArgumentException("Missing metric name in params");
    }
  }

  @Override
  public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    return null;
  }
}
