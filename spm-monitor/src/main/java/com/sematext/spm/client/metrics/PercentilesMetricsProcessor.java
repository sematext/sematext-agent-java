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

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.attributes.MetricType;
import com.sematext.spm.client.observation.PercentilesDefinition;

public class PercentilesMetricsProcessor implements MetricsProcessor {
  private static final Log LOG = LogFactory.getLog(PercentilesMetricsProcessor.class);

  // 6 expected measurements during 1 minute (by default) + 1 additional, just in case
  private static final int DEFAULT_PREVIOUS_VALUES_LIST_SIZE = 7;
  private static final ValueComparator VALUE_COMPARATOR = new ValueComparator();

  private Map<String, List<Object>> metricValues = new UnifiedMap<String, List<Object>>(2);

  public PercentilesMetricsProcessor() {
  }

  @Override
  public void process(MetricsProcessorContext context) {
    Map<String, PercentilesDefinition> pctls = context.percentilesDefinitions;

    if (pctls != null) {
      Map<String, Object> metrics = context.statValues.getMetrics();

      for (Map.Entry<String, PercentilesDefinition> pctlDef : pctls.entrySet()) {
        String metricName = pctlDef.getKey();

        Object val = metrics.get(metricName);
        List<Object> previousValues = metricValues.get(metricName);
        if (val != null) {
          if (previousValues == null) {
            previousValues = new ArrayList<Object>(DEFAULT_PREVIOUS_VALUES_LIST_SIZE);
            metricValues.put(metricName, previousValues);
          }
          previousValues.add(val);
        }

        if (context.shouldFlush) {
          // calculate pctls and add them to metrics
          PercentilesDefinition def = pctlDef.getValue();

          for (Map.Entry<Long, String> singlePctlDef : def.getPctlsToNames().entrySet()) {
            String pctlMetricName = singlePctlDef.getValue();
            metrics.put(pctlMetricName, calculatePctl(previousValues, singlePctlDef.getKey()));
            context.knownMetricTypes.put(pctlMetricName, MetricType.PERCENTILE);
          }

          // since we are flushing, clear previous values so we are ready for next run
          if (previousValues != null) {
            previousValues.clear();
          }
        }
      }
    }
  }

  public static Object calculatePctl(List<Object> previousValues, Long pctl) {
    if (previousValues == null || previousValues.isEmpty()) {
      return null;
    }

    Collections.sort(previousValues, VALUE_COMPARATOR);
    int position = (int) Math.ceil(((double) pctl * previousValues.size()) / 100) - 1;
    return previousValues.get(position);
  }
}

class ValueComparator implements Comparator<Object> {
  @Override
  public int compare(Object o1, Object o2) {
    if (o1 instanceof Number && o2 instanceof Number) {
      double diff = ((Number) o1).doubleValue() - ((Number) o2).doubleValue();
      return diff < 0 ? -1 : diff == 0 ? 0 : 1;
    } else {
      return String.valueOf(o1).compareTo(String.valueOf(o2));
    }
  }
}
