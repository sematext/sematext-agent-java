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
package com.sematext.spm.client.observation;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.ResolverHelper;
import com.sematext.spm.client.aggregation.AgentAggregationFunction;
import com.sematext.spm.client.attributes.MetricType;
import com.sematext.spm.client.json.JsonObservation;

public class DerivedAttribute {
  private static final Log LOG = LogFactory.getLog(DerivedAttribute.class);
  private String name;
  private String expression;
  private CalculationFunction function;
  private boolean stateful;
  private ObservationBean parentObservation;
  private Object[] args;

  private AgentAggregationFunction agentAggregationFunction;
  private MetricType metricType;

  // TODO - Check for better alternative for this cache
  private static Map<String, CalculationFunction> FUNCTIONS_MAP = new UnifiedMap<String, CalculationFunction>();

  // objectNameTags should be null when reading from config, it can be passed as param only when real bean was found in jmx/json/...
  // and there are objectNameTags which can be used for resolving
  public DerivedAttribute(String name, String expression, boolean stateful, Map<String, String> objectNameTags,
                          ObservationBean parentObservation, AgentAggregationFunction agentAggregationFunction,
                          MetricType metricType) {
    if (name == null || "".equals(name.trim())) {
      throw new IllegalArgumentException("DerivedAttribute name missing, expression was: " + expression);
    }

    this.name = name;
    this.stateful = stateful;
    this.parentObservation = parentObservation;
    this.expression = expression;

    this.agentAggregationFunction = agentAggregationFunction;
    this.metricType = metricType;

    if (objectNameTags != null) {
      expression = resolveExpression(expression, objectNameTags);
      expression = expression.trim();
      this.expression = expression;
    }

    if (objectNameTags == null && (usesPlaceholders() || this.stateful)) {
      // while still initializing (objectNameTags=null), if attribute uses placeholders or is stateful, we can't
      // create function object
    } else {
      // otherwise create the object
      if (this.stateful) {
        // can't reuse, should have its own instance
        function = getFunction(expression);
      } else {
        synchronized (FUNCTIONS_MAP) {
          String funcName = expression;
          // if function has args then extract only func:func_name as key
          if (funcName.startsWith("func:") && funcName.indexOf('(') != -1) {
            funcName = funcName.substring(0, funcName.indexOf('(')).trim();
          }
          function = FUNCTIONS_MAP.get(funcName);

          if (function == null) {
            function = getFunction(expression);
            FUNCTIONS_MAP.put(funcName, function);
          }
        }
      }
      args = getFunctionArgs(expression);
    }
  }

  public DerivedAttribute(DerivedAttribute original, Map<String, String> objectNameTags) {
    this(original.name, original.expression, original.stateful, objectNameTags,
         original.parentObservation, original.agentAggregationFunction, original.metricType);
  }

  public String resolveExpression(String expression, Map<String, String> objectNameTags) {
    expression = expression.trim();
    if (objectNameTags != null && objectNameTags.size() > 0) {
      if (expression.startsWith("jmx:") || expression.startsWith("func:") || expression.startsWith("json:") ||
          expression.startsWith("outer:")) {
        expression = ResolverHelper.resolvePlaceholders(expression, objectNameTags);
      }
    }
    return expression;
  }

  private Object[] getFunctionArgs(String expression) {
    Object[] args = null;
    if (expression.startsWith("func:") &&
        expression.indexOf('(') != -1) {  //function contains args
      String argsExpr = expression.substring(expression.indexOf('(') + 1,
                                             expression.indexOf(')'));
      if (!argsExpr.trim().isEmpty()) {
        String[] tokens = argsExpr.split(",");
        if (tokens.length > 0) {
          args = new Object[tokens.length];
          int i = 0;
          for (String token : tokens) {
            args[i++] = token.trim();
          }
        }
      }
    }
    return args;
  }

  public CalculationFunction getFunction(String expression) {
    expression = expression.trim();
    String className;
    if (expression.startsWith("func:")) {
      if (expression.indexOf('(') != -1) {
        className = expression.substring("func:".length(),
                                         expression.indexOf('(')).trim();
      } else {
        className = expression.substring("func:".length()).trim();
      }
      if (!className.contains(".")) {
        className = "com.sematext.spm.client.functions." + className;
      }
      try {
        Class c = Class.forName(className);
        return (CalculationFunction) c.newInstance();
      } catch (Throwable thr) {
        LOG.error("Error while loading custom CalculationFunction : " + className, thr);
        return null;
      }
    } else if (expression.startsWith("jmx:")) {
      expression = expression.substring("jmx:".length()).trim();
      return JmxExpressionFunction.getFunction(expression);
    } else if (expression.startsWith("json:")) {
      expression = expression.substring("json:".length()).trim();
      if (!(parentObservation instanceof JsonObservation)) {
        throw new IllegalArgumentException(
            "Can't use json expression " + expression + " within non-json observation config - " +
                parentObservation.getName());
      }

      return JsonExpressionFunction.getFunction(expression, (JsonObservation) parentObservation);
    } else if (expression.startsWith("eval:")) {
      expression = expression.substring("eval:".length()).trim();
      return new SimpleExpressionCalculation(expression);
    } else if (expression.startsWith("outer:")) {
      expression = expression.substring("outer:".length()).trim();
      return new OuterMetricCalculation(expression);
    } else {
      throw new IllegalArgumentException("Unrecognized format of expression: " + expression);
    }
  }

  public Object apply(Map<String, Object> metrics, Map<String, Object> outerMetrics) {
    if (function != null) {
      try {
        if (function instanceof OuterMetricCalculation) {
          // special case
          return function.calculateAttribute(metrics, outerMetrics);
        } else {
          return function.calculateAttribute(metrics, args);
        }
      } catch (Throwable thr) {
        LOG.error("Error while calculating with function " + function + ", metrics: " + metrics, thr);
        return null;
      }
    } else {
      LOG.warn(
          "Function was null, probably because of error when loading its definition, skipping the calculation for: "
              + name);
      return null;
    }
  }

  public boolean usesPlaceholders() {
    return (expression.indexOf("${") != -1 && expression.indexOf("}") > expression.indexOf("${"));
  }

  public String getName() {
    return name;
  }

  public boolean isStateful() {
    return stateful;
  }

  public AgentAggregationFunction getAgentAggregationFunction() {
    return agentAggregationFunction;
  }

  public MetricType getMetricType() {
    return metricType;
  }
}
