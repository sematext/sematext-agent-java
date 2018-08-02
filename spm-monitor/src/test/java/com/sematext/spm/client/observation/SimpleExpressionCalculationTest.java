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

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SimpleExpressionCalculationTest {
  @Test
  public void test_two_metrics() {
    SimpleExpressionCalculation sec = new SimpleExpressionCalculation("eval:mean*ops");
    Map<String, Object> metricValues = new HashMap<String, Object>();
    metricValues.put("mean", 1.5);
    metricValues.put("ops", 3L);
    Number res = sec.calculateAttribute(metricValues, null);

    Assert.assertEquals(res, 4.5);
  }

  @Test
  public void test_const_metric() {
    SimpleExpressionCalculation sec = new SimpleExpressionCalculation("eval:MemNonHeapUsedM*1048576");
    Map<String, Object> metricValues = new HashMap<String, Object>();
    metricValues.put("MemNonHeapUsedM", 1L);
    Number res = sec.calculateAttribute(metricValues, null);

    Assert.assertEquals(res, 1048576L);
  }
}
