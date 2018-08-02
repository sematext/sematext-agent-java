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

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class IfThenElseTest {

  @Test
  public void testIfThenElseLong() {
    IfThenElse func = new IfThenElse();
    Map<String, Object> metrics = new HashMap<String, Object>();
    metrics.put("a", 0x7fffffffffffffffL);
    Object[] params = { "a", "=", "0x7fffffffffffffffL", "0L", "metric:a" };
    long val = (Long) func.calculateAttribute(metrics, params);
    Assert.assertEquals(val, 0);

    metrics.put("a", 25);
    val = (Integer) func.calculateAttribute(metrics, params);
    Assert.assertEquals(val, 25);

    Object[] params2 = { "a", ">=", "42L", "0L", "metric: a" };
    val = (Integer) func.calculateAttribute(metrics, params2);
    Assert.assertEquals(val, 25);
  }

  @Test
  public void testIfThenElseDouble() {
    IfThenElse func = new IfThenElse();
    Map<String, Object> metrics = new HashMap<String, Object>();
    metrics.put("a", "25.22");
    Object[] params = { "a", "=", "25.22d", "0", "metric:a" };
    double val = (Double) func.calculateAttribute(metrics, params);
    Assert.assertEquals(val, 0.0d, .0d);

    metrics.put("a", 25);
    Assert.assertEquals(func.calculateAttribute(metrics, params), 25);

    Object[] params2 = { "a", ">=", "42.45D", "0.0", "50.5" };
    val = (Double) func.calculateAttribute(metrics, params2);
    Assert.assertEquals(val, 50.5d, .0d);

    metrics.put("a", 42.45e2);
    metrics.put("b", 42L);
    Object[] params3 = { "a", ">=", "metric:b", "0.0", "metric:a" };
    val = (Double) func.calculateAttribute(metrics, params3);
    Assert.assertEquals(val, 0.0, .0d);
  }

  @Test
  public void testIfThenElseString() {
    IfThenElse func = new IfThenElseString();
    Map<String, String> tags = new HashMap<String, String>();
    tags.put("a", "");
    tags.put("b", "hello");
    Object[] params = { "a", "", "tag:b", "tag:a" };
    String val = func.calculateTag(tags, params);
    Assert.assertEquals(val, "hello");

    tags.put("a", "docs");
    Object[] params2 = { "a", "DOCs", "/", "tag:a", true };
    val = func.calculateTag(tags, params2);
    Assert.assertEquals(val, "/");

    Map<String, Object> metrics = new HashMap<String, Object>();
    metrics.put("a", "docs");
    metrics.put("b", 30L);
    metrics.put("c", 20L);
    Object[] params3 = { "a", "docs", "metric:b", "metric:c", true };
    long mVal = (Long) func.calculateAttribute(metrics, params3);
    Assert.assertEquals(mVal, 30L);

    Object[] params4 = { "a", "docs2", "metric:b", "metric:c", true };
    mVal = (Long) func.calculateAttribute(metrics, params4);
    Assert.assertEquals(mVal, 20L);
  }
}
