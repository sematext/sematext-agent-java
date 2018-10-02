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
 * Compares string values. First argument is field name. Second is string which should be used to compare.
 * Optional third argument is ignore case. By default ignore case is false.
 * Returns 1/0 for true/false, based on the result of equals.
 * e.g. func:StringEquals(field,foo)
 */
public class StringEquals implements CalculationFunction {
  private static final Log LOG = LogFactory.getLog(StringEquals.class);
  
  @Override public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    if (params != null && (params.length == 2 || params.length == 3) && params[0] != null && params[1] != null) {
      String metricName = params[0].toString();
      Object metricValue = metrics.get(metricName);
      if (metricValue != null) {
        boolean ignoreCase = false;
        if (params.length == 3) {
          ignoreCase = Boolean.parseBoolean(params[2].toString());
        }
        if (ignoreCase) {
          return metricValue.toString().equalsIgnoreCase(params[1].toString()) ? 1 : 0;
        } else {
          return metricValue.toString().equals(params[1].toString()) ? 1 : 0;
        }
      } else {
        LOG.warn(String.format("Cannot find %s in metrics", metricName));
      }
    } else {
      throw new IllegalArgumentException("Missing metric name and value to compare in params");
    }
    return null;
  }

  @Override public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    return null;
  }
}
