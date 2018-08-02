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
package com.sematext.spm.client;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;

import com.sematext.spm.client.json.JsonObservation;
import com.sematext.spm.client.observation.CalculationFunction;
import com.sematext.spm.client.observation.JmxExpressionFunction;
import com.sematext.spm.client.observation.JsonExpressionFunction;
import com.sematext.spm.client.observation.ObservationBean;

public class ObservationConfigTagResolver {
  // TODO - Check for better alternatives than caching the functions
  static Map<String, CalculationFunction> FUNCTIONS_MAP = new UnifiedMap<String, CalculationFunction>();
  static Map<String, Object[]> ARGS_MAP = new UnifiedMap<String, Object[]>();
  static final Log LOG = LogFactory.getLog(ObservationConfigTagResolver.class);

  public static String resolve(Map<String, String> objectNameTags, String tagDefinition,
                               ObservationBean parentObservation) {
    tagDefinition = tagDefinition.trim();

    // we allow | expressions where we evaluate N clauses, until we find one that doesn't return null
    // if all return null, resolved value will be null
    for (String clause : tagDefinition.split("\\|")) {
      clause = clause.trim();
      if (clause.equals("")) {
        continue;
      }

      try {
        String resolvedValue = resolveSingleClause(objectNameTags, clause, parentObservation);
        if (resolvedValue != null) {
          return resolvedValue;
        }
      } catch (Throwable thr) {
        // just log and skip
        LOG.error(
            "Error while resolving clause: " + clause + " which was part of wider definition: " + tagDefinition, thr);
      }
    }
    return null;
  }

  public static String resolveSingleClause(Map<String, String> objectNameTags, String tagDefinition,
                                           ObservationBean parentObservation) {
    if (tagDefinition.startsWith("${")) {
      return ResolverImpl.PLAIN.resolve(objectNameTags, tagDefinition, parentObservation);
    } else if (tagDefinition.startsWith("func:")) {
      return ResolverImpl.FUNCTION.resolve(objectNameTags, tagDefinition, parentObservation);
    } else if (tagDefinition.startsWith("jmx:")) {
      return ResolverImpl.JMX.resolve(objectNameTags, tagDefinition, parentObservation);
    } else if (tagDefinition.startsWith("json:")) {
      return ResolverImpl.JSON.resolve(objectNameTags, tagDefinition, parentObservation);
    } else {
      return tagDefinition;
    }
  }
}

interface Resolver {
  String resolve(Map<String, String> objectNameTags, String tagDefinition, ObservationBean parentObservation);
}

enum ResolverImpl implements Resolver {
  PLAIN {
    @Override
    protected String resolveInternal(Map<String, String> objectNameTags, String tagDefinition,
                                     ObservationBean parentObservation) {
      String key = tagDefinition.substring("${".length(), tagDefinition.length() - 1);

      String objectNameVal = objectNameTags.get(key);
      if (objectNameVal != null) {
        return objectNameVal;
      } else {
        return tagDefinition;
      }
    }
  },
  FUNCTION {
    @Override
    protected String resolveInternal(Map<String, String> objectNameTags, String tagDefinition,
                                     ObservationBean parentObservation) {

      String funcName; //key for FUNCTIONS_MAP. func:func_name
      boolean argsPresent = tagDefinition.indexOf('(') != -1;
      if (argsPresent) {
        funcName = tagDefinition.substring(0, tagDefinition.indexOf('(')).trim();
      } else {
        funcName = tagDefinition.trim();
      }

      CalculationFunction function = ObservationConfigTagResolver.FUNCTIONS_MAP.get(funcName);
      if (function == null) {
        try {
          String className;
          if (tagDefinition.indexOf('(') != -1) {
            className = tagDefinition.substring("func:".length(),
                                                tagDefinition.indexOf('(')).trim();
          } else {
            className = tagDefinition.substring("func:".length()).trim();
          }

          //if classname does not have package, then append default package
          if (!className.contains(".")) {
            className = "com.sematext.spm.client.functions." + className;
          }

          Class<?> c = Class.forName(className);
          function = (CalculationFunction) c.newInstance();
          ObservationConfigTagResolver.FUNCTIONS_MAP.put(funcName, function);
        } catch (Throwable thr) {
          ObservationConfigTagResolver.LOG.error("Error while loading custom CalculationFunction : " + funcName, thr);
          return null;
        }
      }

      Object[] args = null;
      if (argsPresent) { //if tag definition contains args
        args = ObservationConfigTagResolver.ARGS_MAP.get(tagDefinition.trim());
        if (args == null) {
          args = getFunctionArgs(tagDefinition);
          if (args != null) {
            ObservationConfigTagResolver.ARGS_MAP.put(tagDefinition.trim(), args);
          }
        }
      }

      if (function != null) {
        return function.calculateTag(objectNameTags, args);
      } else {
        return null;
      }
    }

    private Object[] getFunctionArgs(String expression) {
      Object[] args = null;
      if (expression.startsWith("func:") &&
          expression.indexOf('(') != -1) {
        String argsExpr = expression.substring(expression.indexOf('(') + 1,
                                               expression.indexOf(')')); //function contains args
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
  },
  JMX {
    @Override
    protected String resolveInternal(Map<String, String> objectNameTags, String tagDefinition,
                                     ObservationBean parentObservation) {
      tagDefinition = ResolverHelper.resolvePlaceholders(tagDefinition, objectNameTags);
      return JmxExpressionFunction.getFunction(tagDefinition).calculateTag(null);
    }
  },
  JSON {
    @Override
    protected String resolveInternal(Map<String, String> objectNameTags, String tagDefinition,
                                     ObservationBean parentObservation) {
      if (parentObservation instanceof JsonObservation) {
        tagDefinition = ResolverHelper.resolvePlaceholders(tagDefinition, objectNameTags);
        return JsonExpressionFunction.getFunction(tagDefinition, ((JsonObservation) parentObservation))
            .calculateTag(null);
      } else {
        throw new IllegalArgumentException(
            "Can't process json expression: " + tagDefinition + " if not within of json observation bean definition: " +
                parentObservation.getName());
      }
    }
  };

  @Override
  public String resolve(Map<String, String> objectNameTags, String tagDefinition, ObservationBean parentObservation) {
    return resolveInternal(objectNameTags, tagDefinition, parentObservation);
  }

  protected abstract String resolveInternal(Map<String, String> objectNameTags, String tagDefinition,
                                            ObservationBean parentObservation);
}
