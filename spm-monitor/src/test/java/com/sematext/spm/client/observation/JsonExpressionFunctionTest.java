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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.Map;

import com.sematext.spm.client.http.CachableReliableDataSourceBase;
import com.sematext.spm.client.json.JsonDataProvider;

public class JsonExpressionFunctionTest {
  @Test
  public void test_calculate() {
    JsonExpressionFunctionForTesting func = new JsonExpressionFunctionForTesting(
        "dummy1", "http://localhost/_cluster/health?format=smile", false, "$.", "valueOfKey:cluster_name");
    Assert.assertEquals("elasticsearch", func.calculate());

    // this should produce the same
    func = new JsonExpressionFunctionForTesting("dummy2", "http://localhost/_cluster/health?format=smile", false,
        "$.cluster_name", null);
    Assert.assertEquals("elasticsearch", func.calculate());

    func = new JsonExpressionFunctionForTesting("dummy3", "http://localhost/_cluster/health?format=smile", false,
        "$.", "substring_0_4(valueOfKey:cluster_name)");
    Assert.assertEquals("elas", func.calculate());

    func = new JsonExpressionFunctionForTesting("dummy4", "http://localhost/_cluster/health?format=smile", false,
        "$.", "substring_3(valueOfKey:cluster_name)");
    Assert.assertEquals("sticsearch", func.calculate());

    JsonExpressionFunctionForTesting2 func2 = new JsonExpressionFunctionForTesting2(
        "dummy5", "http://localhost/_cluster/health?format=smile", false, "$.datapoints", "length()");
    Assert.assertEquals(5, func2.calculate());

    func2 = new JsonExpressionFunctionForTesting2(
        "dummy5", "http://localhost/_cluster/health?format=smile", false, "$.datapoints", "avg()");
    Assert.assertEquals(32d, func2.calculate());

    func2 = new JsonExpressionFunctionForTesting2(
        "dummy5", "http://localhost/_cluster/health?format=smile", false, "$.datapoints", "sum()");
    Assert.assertEquals(160d, func2.calculate());

    func2 = new JsonExpressionFunctionForTesting2(
        "dummy5", "http://localhost/_cluster/health?format=smile", false, "$.datapoints", "max()");
    Assert.assertEquals(99, func2.calculate());

    func2 = new JsonExpressionFunctionForTesting2(
        "dummy5", "http://localhost/_cluster/health?format=smile", false, "$.datapoints[0:3]", "max()");
    Assert.assertEquals(99, func2.calculate());

    func2 = new JsonExpressionFunctionForTesting2(
        "dummy5", "http://localhost/_cluster/health?format=smile", false, "$.datapoints[0:3]", "length()");
    Assert.assertEquals(3, func2.calculate());
    
    func2 = new JsonExpressionFunctionForTesting2(
        "dummy5", "http://localhost/_cluster/health?format=smile", false, "$.datapoints", "long(avg())");
    Assert.assertEquals(32l, func2.calculate());
    
  }
}

class JsonExpressionFunctionForTesting extends JsonExpressionFunction {
  public JsonExpressionFunctionForTesting(String fullPlaceholderResolvedExpression, String url, boolean smile,
                                          String jsonDataNodePath, String returnExpression) {
    super(fullPlaceholderResolvedExpression, url, smile, jsonDataNodePath, returnExpression, null);
  }

  @Override
  protected CachableReliableDataSourceBase<Object, JsonDataProvider> getDataSource(String url, boolean smile) {
    return null;
  }

  @Override
  protected Map<String, Object> getData() {
    InputStream response = getClass().getResourceAsStream("es-clusterHealth.json");
    TypeReference<UnifiedMap<String, Object>> typeRef = new TypeReference<UnifiedMap<String, Object>>() {
    };
    try {
      return new ObjectMapper(new JsonFactory()).readValue(response, typeRef);
    } catch (Throwable thr) {
      throw new RuntimeException(thr);
    }
  }
}

class JsonExpressionFunctionForTesting2 extends JsonExpressionFunction {
  public JsonExpressionFunctionForTesting2(String fullPlaceholderResolvedExpression, String url, boolean smile,
                                          String jsonDataNodePath, String returnExpression) {
    super(fullPlaceholderResolvedExpression, url, smile, jsonDataNodePath, returnExpression, null);
  }

  @Override
  protected CachableReliableDataSourceBase<Object, JsonDataProvider> getDataSource(String url, boolean smile) {
    return null;
  }

  @Override
  protected Map<String, Object> getData() {
    InputStream response = getClass().getResourceAsStream("json-array.json");
    TypeReference<UnifiedMap<String, Object>> typeRef = new TypeReference<UnifiedMap<String, Object>>() {
    };
    try {
      return new ObjectMapper(new JsonFactory()).readValue(response, typeRef);
    } catch (Throwable thr) {
      throw new RuntimeException(thr);
    }
  }

}