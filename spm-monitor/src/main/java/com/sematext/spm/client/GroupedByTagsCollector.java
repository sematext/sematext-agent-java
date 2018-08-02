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
package com.sematext.spm.client;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.aggregation.MetricAggregator;
import com.sematext.spm.client.attributes.MetricType;
import com.sematext.spm.client.metrics.CompressingMetricsProcessor;
import com.sematext.spm.client.metrics.DecideFlushMetricsProcessor;
import com.sematext.spm.client.metrics.MetricsProcessor;
import com.sematext.spm.client.metrics.MetricsProcessorContext;
import com.sematext.spm.client.metrics.PercentilesMetricsProcessor;
import com.sematext.spm.client.observation.PercentilesDefinition;

public class GroupedByTagsCollector extends BaseSingleStatsCollector<String> {
  private static final Log LOG = LogFactory.getLog(GroupedByTagsCollector.class);

  private Map<String, String> tags;
  private String tagsAsString;
  private String appToken;
  private String metricsNamespace;

  private List<StatsCollector<?>> collectors = new FastList<StatsCollector<?>>();

  private Map<String, PercentilesDefinition> pctlsDefinitions;

  private Map<String, MetricType> knownMetricTypes = new UnifiedMap<String, MetricType>(100);

  private List<MetricsProcessor> metricsProcessors = Collections.EMPTY_LIST;
  private MetricsProcessorContext context;

  public GroupedByTagsCollector(String collectorsGroup, String appToken, Map<String, String> tags,
                                StatValuesSerializer<String> serializer,
                                MonitorConfig monitorConfig, boolean calculatePctls, boolean compressMetrics) {
    super(serializer);

    // ensure all tags values are resolved, otherwise it can't be used for grouping
    for (String value : tags.values()) {
      if (value != null) {
        value = value.trim();
        if (value.startsWith("${") && !value.startsWith("${env:")) {
          throw new IllegalArgumentException("Can't be used for grouping since some values are unresolved: " + tags);
        }
      }
    }
    this.tags = tags;
    this.tagsAsString = getTagsAsString(tags);

    this.appToken = appToken;
    this.metricsNamespace = collectorsGroup;

    if (calculatePctls || compressMetrics) {
      metricsProcessors = new FastList<MetricsProcessor>(3);

      metricsProcessors.add(new DecideFlushMetricsProcessor(monitorConfig.getMonitorCollectInterval()));

      if (calculatePctls) {
        metricsProcessors.add(new PercentilesMetricsProcessor());
      }
      if (compressMetrics) {
        metricsProcessors.add(new CompressingMetricsProcessor());
      }

      context = new MetricsProcessorContext();
      context.knownMetricTypes = knownMetricTypes;
      context.collectorName = this.tagsAsString;
    }
  }

  public static String getTagsAsString(Map<String, String> tags) {
    return tags == null ? "" : tags.toString();
  }

  public static String getGroupedKey(String appToken, String metricsNamespace, String tagsAsString) {
    return appToken + "_" + metricsNamespace + "_" + tagsAsString;
  }

  @Override
  protected void appendStats(StatValues statValues, Map<String, Object> outerMetrics) {
    long currentTime = System.currentTimeMillis();
    statValues.setTags(tags);
    statValues.setTimestamp(currentTime);
    statValues.setAppToken(appToken);
    statValues.setMetricNamespace(metricsNamespace);

    Map<String, Object> aggregatedMetrics = statValues.getMetrics();
    if (aggregatedMetrics == null) {
      aggregatedMetrics = new UnifiedMap<String, Object>(15);
      statValues.setMetrics(aggregatedMetrics);
    }

    for (StatsCollector<?> sc : collectors) {
      try {
        Iterator<StatValues> iter = sc.collectRawStatValues(aggregatedMetrics);
        while (iter.hasNext()) {
          StatValues st = iter.next();

          // TODO handle map logic more optimally? when adding more elements, first resize if we notice newly 
          // added metrics are bigger than current size...

          if (st.getMetrics() != null) {
            for (Map.Entry<String, Object> metric : st.getMetrics().entrySet()) {
              String metricKey = metric.getKey();
              knownMetricTypes.put(metricKey, st.getMetricTypes().get(metricKey));
              Object existingMetricValue = aggregatedMetrics.get(metricKey);

              if (existingMetricValue != null) {
                try {
                  aggregatedMetrics.put(metricKey,
                                        MetricAggregator.aggregate(existingMetricValue, metric.getValue(), metricKey,
                                                                   st.getAgentAggregationFunctions().get(metricKey)));
                } catch (Throwable thr) {
                  // just skip, we don't want everything to break
                  LOG.error("Error while aggregating metric " + metricKey, thr);
                }
              } else {
                aggregatedMetrics.put(metricKey, metric.getValue());
              }
            }
          }
        }
      } catch (StatsCollectionFailedException scfe) {
        LOG.error("Error while gathering stats from collector: " + sc, scfe);
        // don't throw an error, just skip to avoid breaking all collectors
      }
    }

    if (context != null) {
      context.reset();
      context.statValues = statValues;
      context.percentilesDefinitions = pctlsDefinitions;
      for (MetricsProcessor processor : metricsProcessors) {
        processor.process(context);
      }
    }
  }

  @Override
  public String getName() {
    return "grouped";
  }

  @Override
  public String getCollectorIdentifier() {
    return collectors.toString();
  }

  public boolean accept(StatsCollector<?> sc) {
    if (sc.producesMetricsAndTagsMaps()) {
      // TODO remove cast, improve the logic
      GenericCollectorInterface genCol = ((GenericCollectorInterface) sc);

      // also must have matching appToken and collectorsGroup
      if (genCol.getAppToken().equals(appToken) && genCol.getMetricsNamespace().equals(metricsNamespace) &&
          tagsAsString.equals(genCol.getGenericExtractor().getPartlyResolvedObservationConfigTagsAsString())) {
        collectors.add(sc);

        List<PercentilesDefinition> tmpPctlDefinitions = genCol.getGenericExtractor().getPercentilesDefinitions();
        if (tmpPctlDefinitions != null && tmpPctlDefinitions.size() > 0) {
          if (pctlsDefinitions == null) {
            pctlsDefinitions = new UnifiedMap<String, PercentilesDefinition>(5);
          }
          for (PercentilesDefinition pctlDef : tmpPctlDefinitions) {
            // there will sometimes be N collectors for same definition inside of the same GroupedBy. Meaning, same
            // definition would appear N times, but we just want to track unique definitions per-metric here
            pctlsDefinitions.put(pctlDef.getBaseMetricName(), pctlDef);
          }
        }

        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    String res = "[";
    boolean first = true;
    for (StatsCollector<?> sc : collectors) {
      if (!first) {
        res = res + ", ";
      }
      res = res + sc;
      first = false;
    }
    return res + "]";
  }

  public void reset() {
    collectors.clear();

    if (pctlsDefinitions != null) {
      pctlsDefinitions.clear();
    }
  }

  @Override
  public int getCollectorsCount() {
    return collectors.size();
  }
}
