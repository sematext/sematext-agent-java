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

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.http.CachableReliableDataSourceBase;
import com.sematext.spm.client.json.JsonDataProvider;
import com.sematext.spm.client.json.JsonDataSourceCachedFactory;
import com.sematext.spm.client.json.JsonMatchingPath;
import com.sematext.spm.client.json.JsonObservation;
import com.sematext.spm.client.json.JsonUtil;

public class JsonExpressionFunction implements CalculationFunction {
  private static final Log LOG = LogFactory.getLog(JsonExpressionFunction.class);

  private String url;
  private String fullyResolvedUrl;
  private boolean smile;
  private String jsonDataNodePath;
  private String returnExpression;
  private String returnBeanPathAttribName;
  private String returnValueOfKey;
  private String returnValueFunction;
  private boolean returnCountMatches;
  private JsonObservation parentObservation;
  private CachableReliableDataSourceBase<Object, JsonDataProvider> dataSource;
  private String fullPlaceholderResolvedExpression;

  public static JsonExpressionFunction getFunction(String fullPlaceholderResolvedExpression,
                                                   JsonObservation parentObservation) {
    String jsonExpressionDefinition;
    if (fullPlaceholderResolvedExpression.startsWith("json:")) {
      jsonExpressionDefinition = fullPlaceholderResolvedExpression.substring("json:".length()).trim();
    } else {
      jsonExpressionDefinition = fullPlaceholderResolvedExpression;
    }

    if (jsonExpressionDefinition.indexOf(" ") != -1) {
      // there may be placeholders left since they can be needed for json navigation logic
      boolean smile = false;
      if (jsonExpressionDefinition.startsWith("smile:")) {
        smile = true;
        jsonExpressionDefinition = jsonExpressionDefinition.substring("smile:".length()).trim();
      }

      int indexOfNodePath = jsonExpressionDefinition.indexOf(" ");
      if (indexOfNodePath == -1) {
        LOG.warn(
            "Incorrect form of json expression, missing URL and/or node path: " + fullPlaceholderResolvedExpression);
        return null;
      }

      String url = jsonExpressionDefinition.substring(0, indexOfNodePath);
      jsonExpressionDefinition = jsonExpressionDefinition.substring(indexOfNodePath).trim();

      int indexOfReturn = jsonExpressionDefinition.indexOf(" return:");
      String returnExpression = null;
      if (indexOfReturn != -1) {
        returnExpression = jsonExpressionDefinition.substring(indexOfReturn + " return:".length()).trim();
        jsonExpressionDefinition = jsonExpressionDefinition.substring(0, indexOfReturn).trim();
      }

      return new JsonExpressionFunction(fullPlaceholderResolvedExpression, url, smile, jsonExpressionDefinition, returnExpression, parentObservation);
    } else {
      LOG.warn("Incorrect format of json expression, node path should be separated from url with a space: "
                   + fullPlaceholderResolvedExpression);
      return null;
    }
  }

  public JsonExpressionFunction(String fullPlaceholderResolvedExpression, String url, boolean smile,
                                String jsonDataNodePath, String returnExpression, JsonObservation parentObservation) {
    // expression is supposed to be defined as json:smile:/something if smile is to be used; however, smile is used when
    // initializing the datasource, and at this point it is already too late, making smile setting not needed; for the
    // future consider reworking datasources so we have smile setting per datasource instead, in which case this setting
    // would be useful
    this.fullPlaceholderResolvedExpression = fullPlaceholderResolvedExpression;
    this.url = url;
    this.smile = smile;
    this.jsonDataNodePath = jsonDataNodePath;
    this.returnExpression = returnExpression == null ? null : returnExpression.trim();

    if (this.returnExpression != null && this.returnExpression.startsWith("substring_")) {
      returnValueFunction = this.returnExpression.substring(0, this.returnExpression.indexOf("(")).trim();
      this.returnExpression = this.returnExpression.substring(this.returnExpression.indexOf("(") + 1);
      if (this.returnExpression.endsWith(")")) {
        this.returnExpression = this.returnExpression.substring(0, this.returnExpression.length() - 1).trim();
      }
    }

    this.returnBeanPathAttribName = this.returnExpression != null && this.returnExpression.startsWith("pathTags:") ?
        this.returnExpression.substring("pathTags:".length()).trim() : null;
    this.returnValueOfKey = this.returnExpression != null && this.returnExpression.startsWith("valueOfKey:") ?
        this.returnExpression.substring("valueOfKey:".length()).trim() : null;
    this.returnCountMatches = "countMatches".equalsIgnoreCase(this.returnExpression);
    this.parentObservation = parentObservation;

  }

  protected CachableReliableDataSourceBase<Object, JsonDataProvider> getDataSource(String url, boolean useSmile) {
    // can't be async since we need the result right away; no support for custom json handlers for json expressions yet
    return JsonDataSourceCachedFactory.getDataSource(parentObservation.getJsonServerInfo(), url, false, useSmile, null);
  }

  protected Object calculate() {
    if (fullyResolvedUrl == null) {
      // first call...
      fullyResolvedUrl = (url.startsWith("http://") || url.startsWith("https://")) ? url :
          (parentObservation.getJsonServerInfo().getServer() + (!url.startsWith("/") ? "/" : "") + url);
      dataSource = getDataSource(fullyResolvedUrl, smile);
    }

    Object jsonData = getData();
    List<JsonMatchingPath> paths = JsonUtil.findMatchingPaths(jsonData, jsonDataNodePath);

    if (LOG.isDebugEnabled()) {
      LOG.debug("For JSON expression url=" + fullyResolvedUrl + ", nodePath=" + jsonDataNodePath + ", returnExpression="
                    + returnExpression +
                    ", found data=" + jsonData + " with " + paths.size() + " matching paths");
    }

    if (returnCountMatches) {
      return paths.size();
    }

    if (paths.size() == 0) {
      return null;
    } else if (paths.size() == 1) {
      if (returnExpression != null) {
        Object extractedValue = null;

        if (returnBeanPathAttribName != null) {
          extractedValue = paths.get(0).getPathAttributes().get(returnBeanPathAttribName);
        } else if (returnValueOfKey != null) {
          Object match = paths.get(0).getMatchedObject();
          if (match != null && match instanceof Map) {
            extractedValue = ((Map<String, Object>) match).get(returnValueOfKey);
          } else {
            throw new IllegalArgumentException("Can't return valueOfKey for object " + match);
          }
        } else {
          throw new IllegalArgumentException("Unsupported return expression: " + returnExpression);
        }

        if (extractedValue != null && returnValueFunction != null) {
          try {
            if (returnValueFunction.startsWith("substring_")) {
              String tmp1 = returnValueFunction.substring(returnValueFunction.indexOf("_") + 1);
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
            throw new IllegalArgumentException("Can't apply returnValueFunction: " + returnValueFunction +
                                                   " on value: " + extractedValue + ", error was: " + thr.getClass()
                .getName() + ":" + thr.getMessage());
          }
        }

        return extractedValue;
      } else {
        return paths.get(0).getMatchedObject();
      }
    } else {
      throw new IllegalStateException(
          "Found multiple (only 1 should match) matching paths for url:" + fullyResolvedUrl + ", nodePath:"
              + jsonDataNodePath);
    }
  }

  protected Object getData() {
    return dataSource.fetchData();
  }

  @Override
  public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    return calculate();
  }

  @Override
  public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    // cache used only for tags, they will typically be reused very often, while attributes would be reused almost never
    if (ExpressionCache.containsResult(fullPlaceholderResolvedExpression)) {
      Object res = ExpressionCache.getResult(fullPlaceholderResolvedExpression);
      if (res == null) {
        return null;
      } else if (res instanceof String) {
        return (String) res;
      } else {
        return String.valueOf(res);
      }
    } else {
      Object res = calculate();
      String resultStr = res != null ? String.valueOf(res) : null;
      ExpressionCache.addResult(fullPlaceholderResolvedExpression, resultStr);
      return resultStr;
    }
  }
}
