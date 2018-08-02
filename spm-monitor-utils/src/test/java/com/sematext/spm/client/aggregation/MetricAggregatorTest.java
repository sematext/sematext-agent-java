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
package com.sematext.spm.client.aggregation;

import org.junit.Assert;
import org.junit.Test;

public class MetricAggregatorTest {
  @Test
  public void test_aggregate() {
    Long l0 = new Long(0);
    Long l1 = new Long(1);

    Assert.assertEquals(l1, MetricAggregator.aggregate(l0, l1, "l", AgentAggregationFunction.SUM));
    // we check we got exact object (and also exact type), not just exact value
    Assert.assertEquals(true, l1 == MetricAggregator.aggregate(l0, l1, "l", AgentAggregationFunction.SUM));
    Assert.assertEquals(true, MetricAggregator.aggregate(l0, l1, "l", AgentAggregationFunction.SUM) instanceof Long);

    Assert.assertEquals(5l, MetricAggregator.aggregate(4l, l1, "l", AgentAggregationFunction.SUM));

    Object tmpStoredValue = MetricAggregator.aggregate(4l, l1, "l", AgentAggregationFunction.AVG);
    Assert.assertEquals(3, ((LongAvgAggregationHolder) tmpStoredValue).getAverage().longValue());
    Object finalStoredValue = MetricAggregator.aggregate(tmpStoredValue, 2l, "l", AgentAggregationFunction.AVG);
    Assert.assertEquals(tmpStoredValue == finalStoredValue, true);
    Assert.assertEquals(2, ((LongAvgAggregationHolder) finalStoredValue).getAverage().longValue());

    try {
      MetricAggregator.aggregate(tmpStoredValue, 2l, "l", AgentAggregationFunction.SUM);
      Assert.fail();
    } catch (IllegalArgumentException iae) {
      // this is ok
    }

    Double d1 = 1.1;
    Double d2 = 1.3;
    Double d3 = 2.2;

    tmpStoredValue = MetricAggregator.aggregate(d1, d2, "d", AgentAggregationFunction.AVG);
    Assert.assertEquals(1.2, ((DoubleAvgAggregationHolder) tmpStoredValue).getAverage().doubleValue(), 0.1);
    tmpStoredValue = MetricAggregator.aggregate(d1, d3, "d", AgentAggregationFunction.AVG);
    Assert.assertEquals(1.65, ((DoubleAvgAggregationHolder) tmpStoredValue).getAverage().doubleValue(), 0.1);
    finalStoredValue = MetricAggregator.aggregate(tmpStoredValue, d2, "d", AgentAggregationFunction.AVG);
    Assert.assertEquals(tmpStoredValue == finalStoredValue, true);
    Assert.assertEquals(1.5, ((DoubleAvgAggregationHolder) finalStoredValue).getAverage().doubleValue(), 0.1);
  }
}
