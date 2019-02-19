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

import java.util.List;
import java.util.Map;

import com.sematext.spm.client.json.JsonMatchingPath;

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
    // just substring supported for now
    return expression != null && expression.startsWith("substring_");
  }
  
  public FunctionReturnValue(String expression) {
    if (expression != null && expression.startsWith("substring_")) {
      function = expression.substring(0, expression.indexOf("(")).trim();
      expression = expression.substring(expression.indexOf("(") + 1);
      if (expression.endsWith(")")) {
        expression = expression.substring(0, expression.length() - 1).trim();
        nestedReturnValue = JsonExpressionReturnValue.getReturnValue(expression);
      }
    }
  }

  @Override
  protected Object apply(List<JsonMatchingPath> matchingPaths) {
    Object extractedValue = nestedReturnValue.apply(matchingPaths);
    if (extractedValue != null) {
      try {
        if (function.startsWith("substring_")) {
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
      } catch (Throwable thr) {
        throw new IllegalArgumentException("Can't apply returnValueFunction: " + function +
                                               " on value: " + extractedValue + ", error was: " + thr.getClass()
            .getName() + ":" + thr.getMessage());
      }
    }
    
    return null;
  }

  @Override
  protected boolean applicableOnMultiElementList() {
    return false;
  }
}
