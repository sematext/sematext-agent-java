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
 * Function to compare a metric value against Long or Double constant, and then return metric value or constant based on result
 * e.g func:IfThenElse(web.execution.time.min.raw, =, 0x7fffffffffffffffL, 0, metric:web.execution.time.min.raw)
 * func:IfThenElse(web.execution.time.min.raw, =, 22.3d, 0, 22.7)
 * <p>
 * Return data type will dataType of the metrics or Long if specified or double.
 * <p>
 * Signature : IfThenElse(metric_name_to_compare, operator, value_to_compare, value_to_return_if_true, value_to_return_if_false)
 * <p>
 * Values can be literal like 55L or 55d or refer to other metrics like metric:name
 */
public class IfThenElse implements CalculationFunction {
  private static final Log LOG = LogFactory.getLog(IfThenElse.class);
  @Override public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    return calculateValue(metrics, params);
  }

  private Object calculateValue(Map<String, Object> metrics, Object[] params) {
    if (params.length != 5) {
      throw new IllegalArgumentException("Invalid params to function");
    }
    String metricName = params[0].toString();
    if (metrics.get(metricName) != null) {
      return compareNumber(metrics, metricName, params);
    } else {
      LOG.warn("Cannot find metric " + metricName);
      return null;
    }
  }

  private Object compare(String operator, double metricValue, double valueToCompare, Object valueToReturnIfTrue,
                         Object valueToReturnIfFalse) {
    if ("==".equals(operator) || "=".equals(operator)) {
      if (metricValue == valueToCompare) {
        return valueToReturnIfTrue;
      }
    } else if ("<".equals(operator)) {
      if (metricValue < valueToCompare) {
        return valueToReturnIfTrue;
      }
    } else if (">".equals(operator)) {
      if (metricValue > valueToCompare) {
        return valueToReturnIfTrue;
      }
    } else if ("<=".equals(operator)) {
      if (metricValue <= valueToCompare) {
        return valueToReturnIfTrue;
      }
    } else if (">=".equals(operator)) {
      if (metricValue >= valueToCompare) {
        return valueToReturnIfTrue;
      }
    } else {
      throw new IllegalArgumentException("Invalid operator");
    }
    return valueToReturnIfFalse;
  }

  private Object compareNumber(Map<String, Object> metrics, String metricName, Object[] params) {
    double metricValue = Double.parseDouble(metrics.get(metricName).toString());
    String operator = params[1].toString();
    double valueToCompare = new Double(parseValue(metrics, params[2].toString()).toString());
    Object valueToReturnIfTrue = parseValue(metrics, params[3].toString());
    Object valueToReturnIfFalse = parseValue(metrics, params[4].toString());
    return compare(operator, metricValue, valueToCompare, valueToReturnIfTrue, valueToReturnIfFalse);
  }

  protected Object parseValue(Map<String, ?> metrics, String argValue) {
    if (argValue.startsWith("metric")) {
      String metricName = trimPrefix(argValue);
      if (metrics.get(metricName) != null) {
        return metrics.get(metricName);
      } else {
        LOG.warn("Cannot find metric " + metricName);
        return null;
      }
    } else {
      if (argValue.equalsIgnoreCase("null")) {
        return null;
      } else if (argValue.endsWith("l") ||
          argValue.endsWith("L")) {
        return parseLongLiteral(argValue);
      } else {
        return Double.parseDouble(argValue);
      }
    }
  }

  private long parseLongLiteral(String value) {
    if (value.endsWith("L") || value.endsWith("l")) {
      value = value.substring(0, value.length() - 1);
    }
    if (value.startsWith("0x") || value.startsWith("0X")) {
      value = value.substring(2);
      return Long.parseLong(value, 16);
    }
    return Long.parseLong(value);
  }

  protected String trimPrefix(String value) {
    if (value.startsWith("metric")) {
      value = value.substring("metric".length() + 1).trim();
    } else if (value.startsWith("tag")) {
      value = value.substring("tag".length() + 1).trim();
    }
    return value;
  }

  @Override public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    throw new UnsupportedOperationException("Can't be used in tag context");
  }
}
