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
 * Compare strings and returns string value if literal or metrics value data type
 * func:IfThenElseString(webapp_name,, /, tag:webapp_name,true)
 * <p>
 * Signature : IfThenElseString(metric_name_to_compare, value_to_compare, value_to_return_if_true, value_to_return_if_false, ignore_case)
 * <p>
 * Values can be literal like `hello` or `a` or refer to other metrics like metric:name
 */
public class IfThenElseString extends IfThenElse {

  @Override public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    return calculateValue(metrics, params);
  }

  @Override public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    return calculateValue(objectNameTags, params).toString();
  }

  private Object calculateValue(Map<String, ?> metrics, Object[] params) {
    if (params.length < 4) {
      throw new IllegalArgumentException("Invalid params to function");
    }
    boolean ignoreCase = false;
    if (params.length == 5) {
      ignoreCase = Boolean.parseBoolean(params[4].toString());
    }
    String metricName = params[0].toString();
    if (metrics.get(metricName) != null) {
      String valueToCompare = parseValue(metrics, params[1].toString()).toString();
      String metricValue = metrics.get(metricName).toString();
      Object valueToReturnIfTrue = parseValue(metrics, params[2].toString());
      Object valueToReturnIfFalse = parseValue(metrics, params[3].toString());
      boolean result = ignoreCase ? metricValue.equalsIgnoreCase(valueToCompare) :
          metricValue.equals(valueToCompare);
      if (result) {
        return valueToReturnIfTrue;
      } else {
        return valueToReturnIfFalse;
      }
    } else {
      throw new IllegalArgumentException("Cannot find metric " + metricName);
    }
  }

  protected Object parseValue(Map<String, ?> metrics, String argValue) {
    if (argValue.startsWith("metric") || argValue.startsWith("tag")) {
      String metricName = trimPrefix(argValue);
      if (metrics.get(metricName) != null) {
        return metrics.get(metricName);
      } else {
        throw new IllegalArgumentException("Cannot find metric " + metricName);
      }
    } else {
      return argValue;
    }
  }

}
