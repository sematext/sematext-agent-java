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

import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.List;
import java.util.Map;

import com.sematext.spm.client.json.JsonMatchingPath;
import com.sematext.spm.client.json.JsonPathExpressionParser;
import com.sematext.spm.client.json.JsonUtil;
import com.sematext.spm.client.json.JsonUtilFunctionEvaluator;

public class JsonExpressionReturnValue {
  public static ReturnValue getReturnValue(String expression) {
    expression = expression != null ? expression.trim() : null;
    if (NoExpressionReturnValue.applies(expression)) {
      return new NoExpressionReturnValue(expression);
    } else if (BeanPathAttribNameReturnValue.applies(expression)) {
      return new BeanPathAttribNameReturnValue(expression);
    } else if (ValueOfKeyReturnValue.applies(expression)) {
      return new ValueOfKeyReturnValue(expression);
    } else if (CountMatchesReturnValue.applies(expression)) {
      return new CountMatchesReturnValue(expression);
    } else if (FunctionReturnValue.applies(expression)) {
      return new FunctionReturnValue(expression);
    } else {
      throw new IllegalArgumentException("Unsupported json expression : " + expression);
    }
  }
}

abstract class ReturnValue {
  public Object getResult(List<JsonMatchingPath> matchingPaths) {
    if (matchingPaths != null) {
      if (matchingPaths.size() == 1 || applicableOnMultiElementList()) {
        return apply(matchingPaths);
      } else {
        throw new IllegalStateException("Found multiple matching paths where only 1 should match!");
      }
    } else {
      return null;
    }
  }
  
  protected abstract Object apply(List<JsonMatchingPath> matchingPaths);
  
  protected abstract boolean applicableOnMultiElementList();
}

class NoExpressionReturnValue extends ReturnValue {
  public static boolean applies(String expression) {
    return expression == null || expression.trim().equals("");
  }
  
  public NoExpressionReturnValue(String expression) {
  }

  @Override
  protected Object apply(List<JsonMatchingPath> matchingPaths) {
    return matchingPaths.get(0).getMatchedObject();
  }

  @Override
  protected boolean applicableOnMultiElementList() {
    return false;
  }
}

class BeanPathAttribNameReturnValue extends ReturnValue {
  private String returnAttribName;
  
  public static boolean applies(String expression) {
    return expression != null && expression.startsWith("pathTags:");
  }
  
  public BeanPathAttribNameReturnValue(String expression) {
    returnAttribName = expression != null && expression.startsWith("pathTags:") ?
        expression.substring("pathTags:".length()).trim() : null;
  }

  @Override
  protected Object apply(List<JsonMatchingPath> matchingPaths) {
    return matchingPaths.get(0).getPathAttributes().get(returnAttribName);
  }

  @Override
  protected boolean applicableOnMultiElementList() {
    return false;
  }
}

class ValueOfKeyReturnValue extends ReturnValue {
  private String key;
  
  public static boolean applies(String expression) {
    return expression != null && expression.startsWith("valueOfKey:");
  }
  
  public ValueOfKeyReturnValue(String expression) {
    key = expression != null && expression.startsWith("valueOfKey:") ?
        expression.substring("valueOfKey:".length()).trim() : null;
  }

  @Override
  protected Object apply(List<JsonMatchingPath> matchingPaths) {
    JsonMatchingPath path = matchingPaths.get(0);
    Object match = path.getMatchedObject();
    if (match != null && match instanceof Map) {
      return ((Map<String, Object>) match).get(key);
    } else {
      throw new IllegalArgumentException("Can't return valueOfKey for object " + match);
    }
  }

  @Override
  protected boolean applicableOnMultiElementList() {
    return false;
  }
}

class CountMatchesReturnValue extends ReturnValue {
  public static boolean applies(String expression) {
    return "countMatches".equalsIgnoreCase(expression);
  }
  
  public CountMatchesReturnValue(String expression) {
  }

  @Override
  protected Object apply(List<JsonMatchingPath> matchingPaths) {
    return matchingPaths.size();
  }

  @Override
  protected boolean applicableOnMultiElementList() {
    return true;
  }
}

class FunctionReturnValue extends ReturnValue {
  private String function;
  private ReturnValue nestedReturnValue;
  
  public static boolean applies(String expression) {
    return expression != null && (isSubstring(expression) || isCast(expression) ||
        JsonPathExpressionParser.isFunction(expression));
  }
  
  public FunctionReturnValue(String expression) {
    if (expression != null) {
      if (isSubstring(expression) || isCast(expression)) {
        function = expression.substring(0, expression.indexOf("(")).trim();
        expression = expression.substring(expression.indexOf("(") + 1);
        if (expression.endsWith(")")) {
          expression = expression.substring(0, expression.length() - 1).trim();
          nestedReturnValue = JsonExpressionReturnValue.getReturnValue(expression);
        }
      } else if (JsonPathExpressionParser.isFunction(expression)) {
        function = expression;
      }
    }
  }

  private static boolean isSubstring(String expression) {
    return expression.startsWith("substring_");
  }
  
  private static boolean isCast(String expression) {
    return expression.startsWith("long(") || expression.equals("long") ||
        expression.startsWith("double(") || expression.equals("double");
  }

  @Override
  protected Object apply(List<JsonMatchingPath> matchingPaths) {
    Object extractedValue = extractValue(matchingPaths);
    if (extractedValue != null) {
      try {
        if (isSubstring(function)) {
          return substring(extractedValue);
        } else if (isCast(function)) {
          return doCast(extractedValue);
        } else if (JsonPathExpressionParser.isFunction(function)) {
          return JsonUtilFunctionEvaluator.evaluateFunction(function, extractedValue);
        }
      } catch (Throwable thr) {
        throw new IllegalArgumentException("Can't apply returnValueFunction: " + function +
                                               " on value: " + extractedValue + ", error was: " + thr.getClass()
            .getName() + ":" + thr.getMessage());
      }
    }
    
    return null;
  }

  private Object doCast(Object extractedValue) {
    if (function.equals("long")) {
      return ((Number) extractedValue).longValue();
    } else if (function.equals("double")) {
      return ((Number) extractedValue).doubleValue();
    } else {
      throw new IllegalArgumentException("Unrecognize cast expression: " + function);
    }
  }

  private Object substring(Object extractedValue) {
    String tmp1 = function.substring(function.indexOf("_") + 1);
    int startIndex;
    int endIndex;
    int indexOfEndIndex = tmp1.indexOf("_");
    if (indexOfEndIndex != -1) {
      startIndex = Integer.parseInt(tmp1.substring(0, indexOfEndIndex));
      endIndex = Integer.parseInt(tmp1.substring(indexOfEndIndex + 1).trim());
    } else {
      startIndex = Integer.parseInt(tmp1);
      endIndex = -1;
    }
    return endIndex != -1 ? extractedValue.toString().substring(startIndex, endIndex) :
        extractedValue.toString().substring(startIndex);
  }

  private Object extractValue(List<JsonMatchingPath> matchingPaths) {
    if (matchingPaths.size() == 0) {
      return null;
    } else if (matchingPaths.size() == 1) {
      return nestedReturnValue != null ?
          nestedReturnValue.apply(matchingPaths) : matchingPaths.get(0).getMatchedObject();
    } else {
      if (nestedReturnValue != null) {
        if (nestedReturnValue.applicableOnMultiElementList()) {
          return nestedReturnValue.apply(matchingPaths);
        } else {
          // support not added yet
          throw new IllegalArgumentException("Can't apply nestedReturnValue when multiple matching paths exist!");          
        }
      } else {
        List<Object> matchingObjects = new FastList<Object>(matchingPaths.size());
        for (JsonMatchingPath path : matchingPaths) {
          matchingObjects.add(path.getMatchedObject());
        }
        return matchingObjects;
      }
    }
  }

  @Override
  protected boolean applicableOnMultiElementList() {
    return true;
  }
}
