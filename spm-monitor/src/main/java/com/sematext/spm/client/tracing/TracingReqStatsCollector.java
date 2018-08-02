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
package com.sematext.spm.client.tracing;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import com.sematext.spm.client.GenericExtractor;
import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.MultipleStatsCollector;
import com.sematext.spm.client.Serializer;
import com.sematext.spm.client.StatValues;
import com.sematext.spm.client.StatValuesHelper;
import com.sematext.spm.client.attributes.MetricType;
import com.sematext.spm.client.metrics.DecideFlushMetricsProcessor;
import com.sematext.spm.client.metrics.MetricsProcessorContext;
import com.sematext.spm.client.metrics.PercentilesMetricsProcessor;
import com.sematext.spm.client.observation.PercentilesDefinition;
import com.sematext.spm.client.tracing.agent.stats.RequestMetric;

public class TracingReqStatsCollector extends MultipleStatsCollector<RequestMetric> {
  private static final Log LOG = LogFactory.getLog(TracingReqStatsCollector.class);

  private final String appToken;
  private final String jvmName;
  private final String subType;
  private final File propsFile;
  private final String finalJvmName;

  private final MetricsProcessorContext context;
  private final DecideFlushMetricsProcessor decideFlushProcessor;
  private final PercentilesMetricsProcessor pctlsProcessor;

  public TracingReqStatsCollector(Serializer serializer, String appToken, String jvmName, String subType,
                                  MonitorConfig monitorConfig) {
    super(serializer);
    this.appToken = appToken;
    this.jvmName = jvmName;
    this.subType = subType;
    if (subType == null || subType.trim().equals("")) {
      this.finalJvmName = jvmName;
    } else {
      this.finalJvmName = jvmName + "-" + subType;
    }

    this.propsFile = MonitorUtil.fetchSpmMonitorPropertiesFileObject(appToken, jvmName, subType);

    this.decideFlushProcessor = new DecideFlushMetricsProcessor(monitorConfig.getMonitorCollectInterval());
    this.pctlsProcessor = new PercentilesMetricsProcessor();

    context = new MetricsProcessorContext();
    context.collectorName = getCollectorIdentifier();
    context.knownMetricTypes = new UnifiedMap<String, MetricType>(2);
    context.knownMetricTypes.put("requests.count", MetricType.COUNTER);
    context.knownMetricTypes.put("requests.time", MetricType.COUNTER);
    context.percentilesDefinitions = new UnifiedMap<String, PercentilesDefinition>();
    PercentilesDefinition latencyPctl = new PercentilesDefinition("99, 95, 50", "requests.latency.avg");
    context.percentilesDefinitions.put("requests.latency.avg", latencyPctl);
  }

  @Override
  protected Collection<RequestMetric> getSlice(Map<String, Object> outerMetrics) {
    return TracingStatsExtractors.callStatisticsView().requestMetrics();
  }

  @Override
  protected void appendStats(RequestMetric s, StatValues statValues) {
    Long count = s.getCount();
    Long duration = s.getDuration();

    statValues.setMetrics(new UnifiedMap<String, Object>());
    statValues.getMetrics().put("requests.count", count);
    statValues.getMetrics().put("requests.time", duration);
    statValues.setTags(new UnifiedMap<String, String>());
    statValues.getTags().put(GenericExtractor.JVM_NAME_TAG, finalJvmName);
    statValues.getTags().put("tracing.request", s.getId());

    StatValuesHelper.fillHostTags(statValues, propsFile);
    statValues.setTimestamp(System.currentTimeMillis());
    statValues.setAppToken(appToken);
    statValues.setMetricNamespace("tracing");

    context.reset();
    context.statValues = statValues;
    if (count != null && duration != null) {
      if (count > 0) {
        statValues.getMetrics().put("requests.latency.avg", (duration.doubleValue()) / count);
      }
    }
    decideFlushProcessor.process(context);
    pctlsProcessor.process(context);
  }

  @Override
  public String getName() {
    return "tracing-req";
  }

  @Override
  public String getCollectorIdentifier() {
    return getClass().getName();
  }
}
