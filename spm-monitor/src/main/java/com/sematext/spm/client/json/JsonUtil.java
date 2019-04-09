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
package com.sematext.spm.client.json;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

public final class JsonUtil {
  private static final Log LOG = LogFactory.getLog(JsonUtil.class);
  private static final Map<String, String[]> EXTRACTED_CLAUSES = new UnifiedMap<String, String[]>();

  private JsonUtil() {
  }

  public static Collection<JsonMatchingPath> findDistinctMatchingPaths(Object jsonData, String path) {
    List<JsonMatchingPath> matchingPaths = findMatchingPaths(jsonData, path);
    if (matchingPaths.size() <= 1) {
      return matchingPaths;
    }

    Map<String, JsonMatchingPath> resultingMatchingPaths = new UnifiedMap<String, JsonMatchingPath>(
        matchingPaths.size() / 2);
    for (JsonMatchingPath mPath : matchingPaths) {
      if (!resultingMatchingPaths.containsKey(mPath.getFullObjectPath())) {
        // note: ideally we would also clear matchedObject
        resultingMatchingPaths.put(mPath.getFullObjectPath(), mPath);
      }
    }

    return resultingMatchingPaths.values();
  }

  public static List<JsonMatchingPath> findMatchingPaths(Object jsonData, String path) {
    if (jsonData == null || (jsonData instanceof Map && ((Map) jsonData).isEmpty()) ||
        (jsonData instanceof List && ((List) jsonData).isEmpty())) {
      return Collections.EMPTY_LIST;
    }

    path = path.trim();

    if (!path.startsWith("$")) {
      throw new IllegalArgumentException("Path should start with $");
    } else {
      path = path.substring(1).trim();
    }

    if (path.equals("")) {
      return Arrays.asList(new JsonMatchingPath("$", Collections.EMPTY_MAP, jsonData));
    }

    List<JsonMatchingPath> allMatchingPaths = new ArrayList<JsonMatchingPath>();

    // NOTE: all matching ending leaf objects will during the processing receive the same instance of this Map. However, it will
    // contain different values for given keys, depending on which patch is at which point being inspected. When creating result,
    // code has to create a copy of this Map and not reuse existing map instance (since it will change over time and not hold values
    // specific to each node) 
    Map<String, String> pathAttributes = new HashMap<String, String>();

    String[] nodes = JsonPathExpressionParser.parseNodes(path);

    evaluateNode("$", jsonData, nodes, 0, allMatchingPaths, pathAttributes);

    return allMatchingPaths;
  }

  private static void evaluateNode(String pathSoFar, Object jsonNodeData, String[] nodes, int i,
                               List<JsonMatchingPath> allMatchingPaths,
                               Map<String, String> pathAttributes) {
    if (jsonNodeData == null) {
      return;
    }

    if (i == nodes.length) {
      addMatchingNode(pathSoFar, jsonNodeData, allMatchingPaths, pathAttributes);
    } else {
      // need to dig further
      String node = nodes[i].trim();

      // TODO in case of function we stepInto here and once more in if-else block below?
      stepIntoFunctionNode(pathSoFar, jsonNodeData, nodes, i, allMatchingPaths, pathAttributes, node);

      if (jsonNodeData instanceof Map) {
        stepIntoMapNode(pathSoFar, jsonNodeData, nodes, i, allMatchingPaths, pathAttributes, node);
      } else if (jsonNodeData instanceof List) {
        stepIntoListNode(pathSoFar, jsonNodeData, nodes, i, allMatchingPaths, pathAttributes);
      } else {
        // if neither a map nor a list, and we still didn't reach the leaf we are looking for, just end the search
        return;
      }
    }
  }

  private static void stepIntoListNode(String pathSoFar, Object jsonNodeData, String[] nodes, int i,
      List<JsonMatchingPath> allMatchingPaths, Map<String, String> pathAttributes) {
    stepIntoNode(pathSoFar, nodes, i - 1, allMatchingPaths, pathAttributes, jsonNodeData);
  }

  private static void stepIntoMapNode(String pathSoFar, Object jsonNodeData, String[] nodes, int i,
      List<JsonMatchingPath> allMatchingPaths, Map<String, String> pathAttributes, String node) {
    Map<String, Object> jsonNodeDataMap = (Map<String, Object>) jsonNodeData;

    if (JsonPathExpressionParser.isPlaceholder(node)) {
      stepIntoPlaceholder(pathSoFar, nodes, i, allMatchingPaths, pathAttributes, node, jsonNodeDataMap);
    } else {
      stepIntoNode(pathSoFar + "." + node, nodes, i, allMatchingPaths, pathAttributes, jsonNodeDataMap.get(node));            
    }
  }

  private static void stepIntoPlaceholder(String pathSoFar, String[] nodes, int i,
      List<JsonMatchingPath> allMatchingPaths, Map<String, String> pathAttributes, String node,
      Map<String, Object> jsonNodeDataMap) {
    // means all nodes on "current" level match and we have to remember each node name as value
    String nodeName = JsonPathExpressionParser.extractPlaceholderName(node);

    for (String key : jsonNodeDataMap.keySet()) {
      String nodeValue = key;

      // temporarily add to map, remove after exiting from this node
      pathAttributes.put(nodeName, nodeValue);

      stepIntoNode(pathSoFar + "." + escapeSpecialChars(nodeValue), nodes, i, allMatchingPaths, pathAttributes,
          jsonNodeDataMap.get(key));

      pathAttributes.remove(nodeName);
    }
  }

  private static void stepIntoFunctionNode(String pathSoFar, Object jsonNodeData, String[] nodes, int i,
      List<JsonMatchingPath> allMatchingPaths, Map<String, String> pathAttributes, String node) {
    if (JsonPathExpressionParser.isFunction(node)) {
      stepIntoNode(pathSoFar + "." + node, nodes, i, allMatchingPaths, pathAttributes,
          JsonUtilFunctionEvaluator.evaluateFunction(node, jsonNodeData));            
    }
  }
  
  private static void addMatchingNode(String pathSoFar, Object jsonNodeData, List<JsonMatchingPath> allMatchingPaths,
      Map<String, String> pathAttributes) {
    Map<String, String> currentNodePathAttributes;
    if (pathAttributes != null && pathAttributes.size() > 0) {
      currentNodePathAttributes = new HashMap<String, String>(pathAttributes);
    } else {
      currentNodePathAttributes = Collections.EMPTY_MAP;
    }
    allMatchingPaths.add(new JsonMatchingPath(pathSoFar, currentNodePathAttributes, jsonNodeData));
  }

  public static Object findValueIn(String expression, Object jsonNodeData) {
    // seems caching here had negative effect on CPU and only gave very small improvements in heap usage
//    String [] expressionNodes = EXPRESSION_NODES.get(expression);
//    if (expressionNodes == null) {
    String[] expressionNodes = JsonPathExpressionParser.extractExpressionClauses(expression, ".");
//      EXPRESSION_NODES.put(expression, expressionNodes);
//    }

    int i = 0;
    for (String exNode : expressionNodes) {
      i++;
      if (jsonNodeData == null) {
        return null;
      }
      if (JsonPathExpressionParser.isFunction(exNode)) {
        // function should be the last node in the expression.
        if (i != expressionNodes.length) {
          throw new IllegalArgumentException("function should be the last node in the expression.");
        }
        jsonNodeData = JsonUtilFunctionEvaluator.evaluateFunction(exNode, jsonNodeData);
      } else {
        jsonNodeData = ((Map<String, Object>) jsonNodeData).get(exNode);
      }
    }

    return jsonNodeData;
  }
  
  private static void stepIntoNode(String pathSoFar, String[] nodes, int i, List<JsonMatchingPath> allMatchingPaths,
                                   Map<String, String> pathAttributes, Object element) {
    if (element instanceof List) {
      evaluateListNode(pathSoFar, (List) element, nodes, i + 1, allMatchingPaths, pathAttributes);
    } else {
      evaluateNode(pathSoFar, element, nodes, i + 1, allMatchingPaths, pathAttributes);
    }
  }

  private static void evaluateListNode(String pathSoFar, List element, String[] nodes, int i,
      List<JsonMatchingPath> allMatchingPaths, Map<String, String> pathAttributes) {
    if (i == nodes.length) {
      addMatchingNode(pathSoFar, element, allMatchingPaths, pathAttributes);
      return;
    }
    
    String node = nodes[i];
    
    if (JsonPathExpressionParser.isMatchAll(node)) {
      int index = 0;
      for (Object listElement : element) {
        String tmpPathSoFar = pathSoFar + "[" + index++ + "]";
        stepIntoNode(tmpPathSoFar, nodes, i, allMatchingPaths, pathAttributes, listElement);
      }
    } else if (JsonPathExpressionParser.isBracketExpression(node)) {
      node = node.substring(2); // removes ?(
      node = node.substring(0,  node.length() - 1); // removes ending )

      // most often only a single clause, so use predefined 1-sized array in that case, otherwise use size 2
      List<BracketExpressionClause> clauses =
          new ArrayList<BracketExpressionClause>(node.indexOf("@.") == node.lastIndexOf("@.") ? 1 : 2);
      
      for (String expression : JsonPathExpressionParser.extractExpressions(node)) {
        expression = expression.trim();
        if (!expression.equals("")) {
          clauses.add(parseBracketClause(expression));
        }
      }

      // jump into every element, but before that check if all expression paths match condition and collect any path
      // attribs
      for (Object listElement : element) {
        // NOTE: for now we are assuming all clauses act as &&

        String tmpPathSoFar = pathSoFar;

        // first check all clauses match...
        boolean allClausesMatch = true;
        for (BracketExpressionClause clause : clauses) {
          if (!clause.isAttribute) {
            Object expressionValue = findValueIn(clause.path, listElement);
            // check if matches; if yes, continue, otherwise fail
            if (expressionValue == null) {
              allClausesMatch = false;
              break;
            } else {
              String[] values = EXTRACTED_CLAUSES.get(clause.value);
              if (values == null) {
                values = JsonPathExpressionParser.extractExpressionClauses(clause.value, "||");
                EXTRACTED_CLAUSES.put(clause.value, values);
              }
              boolean atLeastOneMatches = false;
              for (String singleValue : values) {
                if (singleValue.equals(String.valueOf(expressionValue))) {
                  atLeastOneMatches = true;
                  break;
                }
              }
              if (atLeastOneMatches) {
                continue;
              } else {
                allClausesMatch = false;
                break;
              }
            }
          }
        }

        if (allClausesMatch) {
          tmpPathSoFar = tmpPathSoFar + "[";
          String resolvedExpression = "?(";
          // now extract attributes and along the way prepare path
          boolean first = true;
          for (BracketExpressionClause clause : clauses) {
            if (!first) {
              resolvedExpression = resolvedExpression + " && ";
            }

            if (clause.isAttribute) {
              Object expressionValue = findValueIn(clause.path, listElement);
              resolvedExpression = resolvedExpression + "@." + clause.path + "=" + expressionValue;
              pathAttributes.put(clause.value, String.valueOf(expressionValue));
            } else {
              resolvedExpression = resolvedExpression + "@." + clause.path + "=" + clause.value;
            }
            first = false;
          }
          resolvedExpression = resolvedExpression + ")";
          tmpPathSoFar = tmpPathSoFar + resolvedExpression + "]";

          stepIntoNode(tmpPathSoFar, nodes, i, allMatchingPaths, pathAttributes, listElement);

          for (BracketExpressionClause clause : clauses) {
            // when done with "current" list element, clear pathAttributes added by it
            pathAttributes.remove(clause.attribute);              
          }
        }
      }
    } else if (JsonPathExpressionParser.isPlaceholder(node)) {
      // TODO de-duplicate the logic copied from traverse() method
      
      // means all nodes on "current" level match and we have to remember each node name as value
      String nodeName = JsonPathExpressionParser.extractPlaceholderName(node);
      
      int counter = 0;
      for (Object listElement : element) {
        pathAttributes.put(nodeName, String.valueOf(counter));
        
        stepIntoNode(pathSoFar + "[" + counter + "]", nodes, i, allMatchingPaths, pathAttributes, listElement);

        counter++;
        pathAttributes.remove(nodeName);
      }
    } else {
      int indexOfColon = node.indexOf(":");
      
      if (indexOfColon != -1) {
        processArrayRange(pathSoFar, nodes, i, allMatchingPaths, pathAttributes, node, element, indexOfColon);
      } else {
        processSingleArrayElement(pathSoFar, nodes, i, allMatchingPaths, pathAttributes, node, element);            
      }
    }
  }

  private static BracketExpressionClause parseBracketClause(String expression) {
    if (expression.endsWith("&&")) {
      expression = expression.substring(0, expression.length() - 2);
    }
    String path = expression.substring(0, expression.indexOf("=")).trim();
    String value = expression.substring(expression.indexOf("=") + 1).trim();
    
    BracketExpressionClause clause;
    if (JsonPathExpressionParser.isPlaceholder(value)) {
      String attribName = value.substring(2, value.length() - 1).trim();
      clause = new BracketExpressionClause(path, attribName, attribName, true);
    } else {
      clause = new BracketExpressionClause(path, null, value, false);
    }
    return clause;
  }

  private static void processArrayRange(String pathSoFar, String[] nodes, int i,
      List<JsonMatchingPath> allMatchingPaths, Map<String, String> pathAttributes, String arrayExpression,
      Object element, int indexOfColon) {
    if (indexOfColon == arrayExpression.length() - 1) {
      throw new UnsupportedOperationException("Array expression 'from tail' not supported yet: " +
          arrayExpression);
    }
    
    int arrayFirstElementIndex;
    int arrayAfterLastElementIndex;
    try {
      arrayFirstElementIndex = indexOfColon == 0 ?
          0 : Integer.parseInt(arrayExpression.substring(0, indexOfColon).trim());
      arrayAfterLastElementIndex = Integer.parseInt(arrayExpression.substring(indexOfColon + 1).trim());
    } catch (Throwable thr) {
      throw new IllegalArgumentException("Incorrect array expression : " + arrayExpression, thr);              
    }

    if (element instanceof List) {
      List elementList = ((List) element);
      
      for (int k = arrayFirstElementIndex; k < Math.min(arrayAfterLastElementIndex, elementList.size()); k++) {                
        stepIntoNode(pathSoFar + "[" + k + "]", nodes, i, allMatchingPaths,
            pathAttributes, elementList.get(k));
      }
    } else {
      LOG.warn("Expected to find a list at " + pathSoFar + ", instead found " + element);
    }
  }

  private static void processSingleArrayElement(String pathSoFar, String[] nodes, int i,
      List<JsonMatchingPath> allMatchingPaths, Map<String, String> pathAttributes, String arrayExpression,
      Object element) {
    int index;
    try {
      index = Integer.parseInt(arrayExpression);
    } catch (Throwable thr) {
      throw new IllegalArgumentException("Expected position in array as integer, instead found " +
          arrayExpression, thr);
    }
    if (element instanceof List) {
      List elementList = ((List) element);
      if (elementList.size() <= index) {
        LOG.warn("Tried to extract element at position " + index + " while there are only " +
            elementList.size() + " elements in array. Path so far was: " + pathSoFar);
      } else {
        // allMatchingPaths.add(new JsonMatchingPath(pathSoFar, Collections.EMPTY_MAP, elementList.get(index)));
        stepIntoNode(pathSoFar + "[" + arrayExpression + "]", nodes, i, allMatchingPaths,
            pathAttributes, elementList.get(index));
      }
    } else {
      LOG.warn("Expected to find a list at " + pathSoFar + ", instead found " + element);
    }
  }

  /**
   * Optimized version of navigateToElement with simplified logic, doesn't accept expressions, only full json
   * node names and drills into jsonData using nodes one by one
   *
   * @param jsonData
   * @param nodes
   * @return
   */
  public static Map<String, Object> quickNavigateToElement(Map<String, Object> jsonData, String... nodes) {
    for (String node : nodes) {
      if (jsonData == null) {
        return null;
      }

      Object tmp = jsonData.get(node);
      if (tmp == null) {
        return null;
      }

      jsonData = (Map<String, Object>) tmp;
    }

    return jsonData;
  }
  
  private static String escapeSpecialChars(String nodeValue) {
    return nodeValue.replace(".", "\\.").replace("[", "\\[").replace("]", "\\]");
  }  
}

class BracketExpressionClause {
  String path;
  String attribute;
  String value;
  boolean isAttribute;
  
  BracketExpressionClause(String path, String attribute, String value, boolean isAttribute) {
    this.path = path;
    this.attribute = attribute;
    this.value = value;
    this.isAttribute = isAttribute;
  }
}