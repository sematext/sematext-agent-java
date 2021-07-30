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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sematext.spm.client.JsonFunction;
import com.sematext.spm.client.JsonFunctionFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class JsonPathExpressionParser {
  public static final Cache<String, String[]> CACHED_PARSED_NODES = CacheBuilder.newBuilder()
      .maximumSize(300000)
      .expireAfterAccess(30, TimeUnit.MINUTES)
      .build();

  public static final Cache<String, String[]> EXTRACTED_EXPRESSIONS = CacheBuilder.newBuilder()
      .maximumSize(300000)
      .expireAfterAccess(30, TimeUnit.MINUTES)
      .build();

  private JsonPathExpressionParser() {}

  public static String[] extractExpressionClauses(String value, String separator) {
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

  public static boolean isFunction(String node) {
    return node.replaceAll(" ", "").endsWith("()");
  }

  public static boolean isPlaceholder(String value) {
    return value.startsWith("${") && value.endsWith("}");
  }

  public static String extractPlaceholderName(String node) {
    return node.substring(2, node.lastIndexOf("}"));
  }

  public static boolean isBracketExpression(String node) {
    return node.startsWith("?(@.");
  }

  public static boolean isMatchAll(String node) {
    return node.equals("*");
  }

  public static String[] extractExpressions(String arrayExpression) {
    String[] res;
    res = EXTRACTED_EXPRESSIONS.getIfPresent(arrayExpression);
    if (res == null) {
      res = arrayExpression.split("@\\.");
      EXTRACTED_EXPRESSIONS.put(arrayExpression, res);
    }

    return res;
  }

  public static String[] parseNodes(String path) {
    String[] resNodes;
    resNodes = CACHED_PARSED_NODES.getIfPresent(path);
    if (resNodes != null) {
      return resNodes;
    }

    List<String> nodes = new ArrayList<String>(3);

    boolean bracketExpOpen = false;
    int indexOfNodeStart = 0;
    int i = 0;
    List<Integer> positionsOfEscapeChars = new ArrayList<Integer>();

    for (; i < path.length(); i++) {
      char c = path.charAt(i);

      if (c == '[') {
        if (!bracketExpOpen) {
          bracketExpOpen = true;
          addPreviousNode(path, nodes, indexOfNodeStart, i, positionsOfEscapeChars);
          indexOfNodeStart = i + 1;
        }
      } else if (c == '.') {
        if (bracketExpOpen) {
          ;
        } else {
          addPreviousNode(path, nodes, indexOfNodeStart, i, positionsOfEscapeChars);
          indexOfNodeStart = i + 1;
        }
      } else if (c == ']') {
        if (bracketExpOpen) {
          // consider as end of the bracket expression (and drop the brackets)
          addPreviousNode(path, nodes, indexOfNodeStart, i, positionsOfEscapeChars);
          indexOfNodeStart = i + 1;
          bracketExpOpen = false;
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

  private static void addPreviousNode(String path, List<String> nodes, int indexOfNodeStart, int i,
      List<Integer> positionsOfEscapeChars) {
    if (indexOfNodeStart != i) {
      nodes.add(clearEscapeChars(path.substring(indexOfNodeStart, i), positionsOfEscapeChars));
      positionsOfEscapeChars.clear();      
    }
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
}
