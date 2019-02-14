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
import java.util.stream.Collectors;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

public final class JsonUtil {
  private static final Log LOG = LogFactory.getLog(JsonUtil.class);

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

    if (!path.startsWith("$.")) {
      throw new IllegalArgumentException("Path should start with $.");
    } else {
      path = path.substring(2).trim();
    }

    if (path.equals("")) {
      return Arrays.asList(new JsonMatchingPath("$.", Collections.EMPTY_MAP, jsonData));
    }

    List<JsonMatchingPath> allMatchingPaths = new ArrayList<JsonMatchingPath>();

    // NOTE: all matching ending leaf objects will during the processing receive the same instance of this Map. However, it will
    // contain different values for given keys, depending on which patch is at which point being inspected. When creating result,
    // code has to create a copy of this Map and not reuse existing map instance (since it will change over time and not hold values
    // specific to each node) 
    Map<String, String> pathAttributes = new HashMap<String, String>();

    String[] nodes = parseNodes(path);

    traverse("$", jsonData, nodes, 0, allMatchingPaths, pathAttributes);

    return allMatchingPaths;
  }

  private static final Map<String, String[]> CACHED_PARSED_NODES = new UnifiedMap<String, String[]>();

  public static String[] parseNodes(String path) {
    // TODO - Currently we are caching the results. The cache might have millions of entries.
    // Without caching,  we might have to parse the same thing N times.
    String[] resNodes = CACHED_PARSED_NODES.get(path);
    if (resNodes != null) {
      return resNodes;
    }

    List<String> nodes = new ArrayList<String>(3);

    boolean arrayExpOpen = false;
    int indexOfNodeStart = 0;
    int i = 0;
    List<Integer> positionsOfEscapeChars = new ArrayList<Integer>();

    for (; i < path.length(); i++) {
      char c = path.charAt(i);

      if (c == '[') {
        if (!arrayExpOpen) {
          arrayExpOpen = true;
        }
      } else if (c == '.') {
        if (arrayExpOpen) {
          ;
        } else {
          nodes.add(clearEscapeChars(path.substring(indexOfNodeStart, i), positionsOfEscapeChars));
          indexOfNodeStart = i + 1;
          positionsOfEscapeChars.clear();
        }
      } else if (c == ']') {
        if (arrayExpOpen && ((i == path.length() - 1) || path.charAt(i + 1) == '.')) {
          // consider as end of the array expression
          nodes.add(clearEscapeChars(path.substring(indexOfNodeStart, i + 1), positionsOfEscapeChars));
          arrayExpOpen = false;
          i++; // also skip the next .
          indexOfNodeStart = i + 1;
          positionsOfEscapeChars.clear();
        }
      } else if (c == '\\') {
        // escape char, meaning, the following char shouldn't be treated as special char
        if (i < path.length() - 1) {
          positionsOfEscapeChars.add(i - indexOfNodeStart);
          i++;
        }
      } else {
        ;
      }
    }

    if (i != indexOfNodeStart) {
      nodes.add(clearEscapeChars(path.substring(indexOfNodeStart, i), positionsOfEscapeChars));
    }

    resNodes = nodes.toArray(new String[nodes.size()]);
    CACHED_PARSED_NODES.put(path, resNodes);

    return resNodes;
  }

  private static String clearEscapeChars(String nodeName, List<Integer> positionsOfEscapeChars) {
    if (positionsOfEscapeChars.size() > 0) {
      String newStr = "";
      int prevEscapeCharPos = -1;
      for (Integer i : positionsOfEscapeChars) {
        newStr = newStr + nodeName.substring(prevEscapeCharPos + 1, i);
        prevEscapeCharPos = i;
      }
      newStr = newStr + nodeName.substring(prevEscapeCharPos + 1);
      return newStr;
    } else {
      return nodeName;
    }
  }

  private static void traverse(String pathSoFar, Object jsonNodeData, String[] nodes, int i,
                               List<JsonMatchingPath> allMatchingPaths,
                               Map<String, String> pathAttributes) {
    if (jsonNodeData == null) {
      return;
    }

    if (i == nodes.length) {
      // we reached the leaf we were looking for
      Map<String, String> currentNodePathAttributes;
      if (pathAttributes != null && pathAttributes.size() > 0) {
        currentNodePathAttributes = new HashMap<String, String>(pathAttributes);
      } else {
        currentNodePathAttributes = Collections.EMPTY_MAP;
      }
      allMatchingPaths.add(new JsonMatchingPath(pathSoFar, currentNodePathAttributes, jsonNodeData));
    } else {
      // need to dig further
      String node = nodes[i].trim();

      int indexOfArrayDefOpen = node.indexOf("[");
      int lastIndexOfArrayDefClose = node.lastIndexOf("]");

      boolean array = false;
      boolean arrayMatchAll = false;
      String arrayPartOfPath = "";
      String arrayExpression = null;

      if (indexOfArrayDefOpen != -1 && lastIndexOfArrayDefClose != -1
          && indexOfArrayDefOpen < lastIndexOfArrayDefClose) {
        String arrayDef = node.substring(indexOfArrayDefOpen + 1, lastIndexOfArrayDefClose).trim();
        array = true;
        // TODO add support for various array expressions
        if (arrayDef.trim().equals("*")) {
//          arrayMatchAll = true;
//          arrayDef = arrayDef.trim();
//          arrayPartOfPath = "[*]";
          throw new IllegalArgumentException("Unsupported array expression '*' - it is ambiguous, matching paths can't be used for monitoring");
        } else if (arrayDef.startsWith("?(@.") && arrayDef.endsWith(")")) {
          arrayMatchAll = true;
          arrayExpression = arrayDef.substring("?(".length(), arrayDef.lastIndexOf(")"));
        } else {
          // no sub-expression, just specific element position
          arrayMatchAll = false;
          arrayExpression = arrayDef;
        }

        node = node.substring(0, indexOfArrayDefOpen).trim();
      }

      if (jsonNodeData instanceof Map) {
        Map<String, Object> jsonNodeDataMap = (Map<String, Object>) jsonNodeData;

        if (node.startsWith("${") && node.endsWith("}")) {
          // means all nodes on "current" level match and we have to remember each node name as value
          String nodeName = node.substring(2, node.lastIndexOf("}"));

          for (String key : jsonNodeDataMap.keySet()) {
            String nodeValue = key;

            // add to map
            pathAttributes.put(nodeName, nodeValue);

            Object element = jsonNodeDataMap.get(key);

            traverseNode(pathSoFar + "." + escapeSpecialChars(nodeValue)
                             + arrayPartOfPath, nodes, i, allMatchingPaths, pathAttributes, array,
                         arrayMatchAll, arrayExpression, element);

            // remove from map
            pathAttributes.remove(nodeName);
          }
        } else {
          traverseNode(pathSoFar + "." + node + arrayPartOfPath, nodes, i, allMatchingPaths, pathAttributes, array,
                       arrayMatchAll, arrayExpression, jsonNodeDataMap.get(node));
        }
      } else if (jsonNodeData instanceof List) {
        if (node.trim().equals("")) {
          traverseNode(pathSoFar + "." + node + arrayPartOfPath, nodes, i, allMatchingPaths, pathAttributes, array,
                  arrayMatchAll, arrayExpression, jsonNodeData);
        } else {
          throw new UnsupportedOperationException("Lists were supposed to be handled differently");
        }
      } else {
        // if neither a map nor a list, and we still didn't reach the leaf we are looking for, just end the search
        return;
      }
    }
  }

  private static Object evaluateFunction(String node, Object element) {
    if (!(element instanceof List)) {
      throw new UnsupportedOperationException(String.format("Cannot evaluate function %s. Functions are allowed only on lists.", node));
    }
    List elementList = (List)element;
    String function = node.substring(0, node.indexOf("(")).trim();
    Object result;
    if ("length".equals(function)) {
      result = elementList.size();
    } else if ("max".equals(function)) {
      result = Collections.max(elementList);
    } else if ("min".equals(function)) {
      result = Collections.min(elementList);
    } else if ("sum".equals(function)) {
      result = elementList.stream().collect(Collectors.summingDouble(e -> ((Number) e).doubleValue()));
    } else if ("avg".equals(function)) {
      result = elementList.stream().collect(Collectors.averagingDouble(e -> ((Number) e).doubleValue()));
    } else {
      throw new UnsupportedOperationException(String.format("Unknown function %s", node));
    }
    return result;
  }

  private static String escapeSpecialChars(String nodeValue) {
    return nodeValue.replace(".", "\\.").replace("[", "\\[").replace("]", "\\]");
  }

  public static Object findValueIn(String expression, Object jsonNodeData) {
    // seems caching here had negative effect on CPU and only gave very small improvements in heap usage
//    String [] expressionNodes = EXPRESSION_NODES.get(expression);
//    if (expressionNodes == null) {
    String[] expressionNodes = extractNodes(expression, ".");
//      EXPRESSION_NODES.put(expression, expressionNodes);
//    }

    int i = 0;
    for (String exNode : expressionNodes) {
      i++;
      if (jsonNodeData == null) {
        return null;
      }
      if (exNode.contains("(")) {
        boolean function = exNode.replaceAll(" ", "").endsWith("()");
        if (function) {
          // function should be the last node in the expression.
          if (i != expressionNodes.length) {
            throw new IllegalArgumentException("function should be the last node in the expression.");
          }
          jsonNodeData = evaluateFunction(exNode, jsonNodeData);
        }
      } else {
        jsonNodeData = ((Map<String, Object>) jsonNodeData).get(exNode);
      }
    }

    return jsonNodeData;
  }

  public static String[] extractNodes(String value, String separator) {
    String[] res;
    int separatorLength = separator.length();
    if (value.indexOf(separator) == -1) {
      res = new String[1];
      res[0] = value;
    } else {
      res = new String[countOf(value, separator) + 1];
      int indexOfNextSeparator = value.indexOf(separator);
      int clauseCount = 0;
      while (indexOfNextSeparator != -1) {
        res[clauseCount++] = value.substring(0, indexOfNextSeparator);
        value = value.substring(indexOfNextSeparator + separatorLength);
        indexOfNextSeparator = value.indexOf(separator);
      }
      res[clauseCount++] = value;
    }
    return res;
  }

  private static void traverseNode(String pathSoFar, String[] nodes, int i, List<JsonMatchingPath> allMatchingPaths,
                                   Map<String, String> pathAttributes, boolean array, boolean arrayMatchAll,
                                   String arrayExpression, Object element) {
    // TODO one day add support for array within array within array...
    if (array) {
      if (element instanceof List) {
        // TODO add support for other array expressions
        if (arrayMatchAll) {
          List<String> nodePaths;
          List<String> nodePathValueNames;
          List<Boolean> nodePathValueNameIsAttribute;
          List<String> nodePathAttributeNames;
          if (arrayExpression != null) {
            // most often only a single clause, so use predefined 1-sized array in that case, otherwise use size 2
            nodePaths = new ArrayList<String>(
                arrayExpression.indexOf("@.") == arrayExpression.lastIndexOf("@.") ? 1 : 2);
            nodePathValueNames = new ArrayList<String>(nodePaths.size());
            nodePathValueNameIsAttribute = new ArrayList<Boolean>(nodePaths.size());
            nodePathAttributeNames = new ArrayList<String>(nodePaths.size());
            for (String expression : extractExpressions(arrayExpression)) {
              expression = expression.trim();
              if (expression.equals("")) {
                continue;
              }
              if (expression.endsWith("&&")) {
                expression = expression.substring(0, expression.length() - 2);
              }
              String path = expression.substring(0, expression.indexOf("=")).trim();
              String value = expression.substring(expression.indexOf("=") + 1).trim();
              nodePaths.add(path);
  
              if (value.startsWith("${") && value.endsWith("}")) {
                String attribName = value.substring(2, value.length() - 1).trim();
                nodePathValueNames.add(attribName);
                nodePathValueNameIsAttribute.add(Boolean.TRUE);
                nodePathAttributeNames.add(attribName);
              } else {
                nodePathValueNames.add(value);
                nodePathValueNameIsAttribute.add(Boolean.FALSE);
              }
            }
          } else {
            nodePaths = Collections.EMPTY_LIST;
            nodePathValueNames = Collections.EMPTY_LIST;
            nodePathValueNameIsAttribute = Collections.EMPTY_LIST;
            nodePathAttributeNames = Collections.EMPTY_LIST;
          }
  
          // jump into every element, but before that check if all expression paths match condition and collect any path attribs
          for (Object listElement : ((List) element)) {
            // NOTE: for now we are assuming all clauses act as &&
  
            String tmpPathSoFar = pathSoFar;
  
            // first check all clauses match...
            boolean allClausesMatch = true;
            for (int j = 0; j < nodePaths.size(); j++) {
              String path = nodePaths.get(j);
              String value = nodePathValueNames.get(j);
              Boolean valueIsAttribute = nodePathValueNameIsAttribute.get(j);
  
              if (!valueIsAttribute) {
                Object expressionValue = findValueIn(path, listElement);
                // check if matches; if yes, continue, otherwise fail
                if (expressionValue == null) {
                  allClausesMatch = false;
                  break;
                } else {
                  String[] values = EXTRACTED_CLAUSES.get(value);
                  if (values == null) {
                    values = extractNodes(value, "||");
                    EXTRACTED_CLAUSES.put(value, values);
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
              for (int j = 0; j < nodePaths.size(); j++) {
                String path = nodePaths.get(j);
                String value = nodePathValueNames.get(j);
                Boolean valueIsAttribute = nodePathValueNameIsAttribute.get(j);
  
                if (j > 0) {
                  resolvedExpression = resolvedExpression + " && ";
                }
  
                if (valueIsAttribute) {
                  Object expressionValue = findValueIn(path, listElement);
                  resolvedExpression = resolvedExpression + "@." + path + "=" + expressionValue;
                  pathAttributes.put(value, String.valueOf(expressionValue));
                } else {
                  resolvedExpression = resolvedExpression + "@." + path + "=" + value;
                }
              }
              resolvedExpression = resolvedExpression + ")";
              tmpPathSoFar = tmpPathSoFar + resolvedExpression + "]";
  
              traverse(tmpPathSoFar, listElement, nodes, i + 1, allMatchingPaths, pathAttributes);
  
              for (String attribName : nodePathAttributeNames) {
                // when done with "current" list element, clear pathAttributes added by it
                pathAttributes.remove(attribName);
              }
            }
          }
        } else {
          arrayExpression = arrayExpression.trim();
          int indexOfColon = arrayExpression.indexOf(":");
          
          if (indexOfColon != -1) {
            processArrayRange(pathSoFar, nodes, i, allMatchingPaths, pathAttributes, arrayExpression, element,
                indexOfColon);
          } else {
            processSingleArrayElement(pathSoFar, nodes, i, allMatchingPaths, pathAttributes, arrayExpression, element);            
          }
        }
      }
      // otherwise just ignore it, it doesn't match since it is not an array
    } else {
      // jump into each of them
      traverse(pathSoFar, element, nodes, i + 1, allMatchingPaths, pathAttributes);
    }
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
        traverse(pathSoFar + "[" + k + "]", elementList.get(k), nodes, i + 1, allMatchingPaths,
            pathAttributes);
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
        traverse(pathSoFar + "[" + arrayExpression + "]", elementList.get(index), nodes, i + 1, allMatchingPaths,
            pathAttributes);
      }
    } else {
      LOG.warn("Expected to find a list at " + pathSoFar + ", instead found " + element);
    }
  }

  private static final Map<String, String[]> EXTRACTED_CLAUSES = new UnifiedMap<String, String[]>();

  private static int countOf(String value, String substring) {
    int indexOfNextOperator = value.indexOf(substring);
    int countOfSubstring = 0;
    int substringLength = substring.length();
    while (indexOfNextOperator != -1) {
      countOfSubstring++;
      value = value.substring(indexOfNextOperator + substringLength);
      indexOfNextOperator = value.indexOf(substring);
    }
    return countOfSubstring;
  }

  private static final Map<String, String[]> EXTRACTED_EXPRESSIONS = new UnifiedMap<String, String[]>();

  private static String[] extractExpressions(String arrayExpression) {
    String[] res = EXTRACTED_EXPRESSIONS.get(arrayExpression);
    if (res == null) {
      res = arrayExpression.split("@\\.");
      EXTRACTED_EXPRESSIONS.put(arrayExpression, res);
    }

    return res;
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
}
