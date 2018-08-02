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
package com.sematext.spm.client.metrics;

import java.util.HashMap;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.StatValues;
import com.sematext.spm.client.aggregation.AvgAggregationHolder;
import com.sematext.spm.client.attributes.MetricType;

/**
 * Stateful processor. Should be run at the end of the processor chain since it prepares data before eventual flush (chooses
 * what is and what isn't sent).
 * <p>
 * Compresses metrics:
 * - counters are sent only once a minute, at its end
 * - pctls the same as counters
 * - gauges are sent when the minute starts and after they change their value
 * - the rest is always sent as-is
 */
public class CompressingMetricsProcessor implements MetricsProcessor {
  private static final Log LOG = LogFactory.getLog(CompressingMetricsProcessor.class);

  private Map<String, Object> lastRecordedValues = new HashMap<String, Object>(100);

  @Override
  public void process(MetricsProcessorContext context) {
    StatValues statValues = context.statValues;
    Map<String, MetricType> knownMetricTypes = context.knownMetricTypes;
    Map<String, Object> aggregatedMetrics = statValues.getMetrics();

    for (Map.Entry<String, Object> metric : aggregatedMetrics.entrySet()) {
      String metricName = metric.getKey();
      Object metricValue = metric.getValue();
      if (metricValue != null && metricValue instanceof AvgAggregationHolder) {
        metricValue = ((AvgAggregationHolder) metricValue).getAverage();
      }
      MetricType metricType = knownMetricTypes.get(metricName);
      Object lastRecordedValue = lastRecordedValues.get(metricName);

      if (metricType == MetricType.COUNTER) {
        processCounter(context.shouldFlush, metric, metricName, metricValue, metricType, lastRecordedValue);
      } else if (metricType == MetricType.GAUGE || metricType == MetricType.TEXT) {
        processGauge(metric, metricName, metricValue, lastRecordedValue);
      } else if (metricType == MetricType.OTHER) {
        lastRecordedValues.put(metricName, metricValue);
      } else if (metricType == MetricType.PERCENTILE) {
        lastRecordedValues.put(metricName, metricValue);
      } else {
        throw new UnsupportedOperationException(
            "Currently unsupported metric type " + metricType + " for metric " + metricName);
      }
    }

    if (context.shouldFlush) {
      // check if there are any counter metrics in lastRecordedValue that are not present in current aggregatedMetrics
      // if yes, then those metrics should be added to statValues now since flush is happening; we don't care about gauges and
      // others since they were already sent before (they don't accumulate, we don't hold off sending them until the minute ends)
      Map<String, Object> currentMetrics = aggregatedMetrics;
      for (Map.Entry<String, Object> metric : lastRecordedValues.entrySet()) {
        String metricName = metric.getKey();
        MetricType metricType = knownMetricTypes.get(metricName);
        if ((metricType == MetricType.COUNTER || metricType == MetricType.PERCENTILE) && !currentMetrics
            .containsKey(metricName)) {
          currentMetrics.put(metricName, metric.getValue());
        }
      }

      // also clear all recorded stats (it is either last measurement in some minute or first after it which got delayed a bit)
      lastRecordedValues.clear();
    }
  }

  public void processGauge(Map.Entry<String, Object> metric, String metricName, Object metricValue,
                           Object lastRecordedValue) {
    // if changed from last measurement, return it as result, otherwise set to null
    if (metricValue != null && !metricValue.equals(lastRecordedValue)) {
      // all ok, we'll record it
    } else {
      // otherwise set it to null in current metrics response
      metric.setValue(null);
    }

    lastRecordedValues.put(metricName, metricValue);
  }

  public void processCounter(boolean flushMetrics, Map.Entry<String, Object> metric, String metricName,
                             Object metricValue, MetricType metricType, Object lastRecordedValue) {
    Object newValue = null;

    if (lastRecordedValue == null && metricValue == null) {
      // do nothing;
    } else if (lastRecordedValue == null) {
      newValue = metricValue;
    } else if (metricValue == null) {
      newValue = lastRecordedValue;
    } else {
      if ((metricValue instanceof Long || metricValue instanceof Integer) &&
          (lastRecordedValue instanceof Long || lastRecordedValue instanceof Integer)) {
        newValue = ((Number) metricValue).longValue() + ((Number) lastRecordedValue).longValue();
      } else if ((metricValue instanceof Double || metricValue instanceof Float) &&
          (lastRecordedValue instanceof Double || lastRecordedValue instanceof Float)) {
        newValue = ((Number) metricValue).doubleValue() + ((Number) lastRecordedValue).doubleValue();
      } else {
        LOG.error("Unsupported data type of new measurement " + metricValue.getClass() +
                      " in combination with previous measurement type " + lastRecordedValue.getClass() +
                      " for metric of type " + metricType + ", metric name was: " + metricName);
        newValue = lastRecordedValue;
      }
    }

    lastRecordedValues.put(metricName, newValue);

    if (flushMetrics) {
      metric.setValue(newValue);
    } else {
      metric.setValue(null);
    }
  }
}
