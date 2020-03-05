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
package com.sematext.spm.client.storm;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.MultipleStatsCollector;
import com.sematext.spm.client.Serializer;
import com.sematext.spm.client.StatValues;
import com.sematext.spm.client.StatValuesHelper;
import com.sematext.spm.client.StatsCollectionFailedException;
import com.sematext.spm.client.util.Tuple;
import com.sematext.spm.client.util.Utils;

public class StormTopologyStatsCollector extends MultipleStatsCollector<Tuple<String, List<Object>>>
    implements UpdatableStormStatsCollector {
  private StormInfoSource stormInfoSource;
  private final String appToken;
  private final String jvmName;
  private final String subType;
  private final File propsFile;
  private final Map<String, List<StormInfoMetricExtractor<Object>>> extractors = new UnifiedMap<String, List<StormInfoMetricExtractor<Object>>>();

  @Override
  public void updateSource(StormInfoSource infoSource) {
    this.stormInfoSource = infoSource;
  }

  //ordering matters
  private static enum StormTopologyInfoMetric {
    TOPOLOGY_STATUS(StormInfoMetric.TOPOLOGY_STATUS),
    WORKERS_COUNT(StormInfoMetric.WORKERS_COUNT),
    EXECUTORS_COUNT(StormInfoMetric.EXECUTORS_COUNT),
    TASKS_COUNT(StormInfoMetric.TASKS_COUNT),
    BOLTS_COUNT(StormInfoMetric.BOLTS_COUNT),
    SPOUTS_COUNT(StormInfoMetric.SPOUTS_COUNT),
    STATE_SPOUTS_COUNT(StormInfoMetric.STATE_SPOUTS_COUNT);

    private StormInfoMetric stormMetric;

    private StormTopologyInfoMetric(StormInfoMetric stormMetric) {
      this.stormMetric = stormMetric;
    }
  }

  public StormTopologyStatsCollector(StormInfoSource infoSource, String appToken, String jvmName, String subType) {
    // serializer should arrive from the config
    super(Serializer.INFLUX);

    this.stormInfoSource = infoSource;
    this.appToken = appToken;
    this.jvmName = jvmName;
    this.subType = subType;
    this.propsFile = MonitorUtil.fetchSpmMonitorPropertiesFileObject(appToken, jvmName, subType);
  }

  @Override
  protected Collection<Tuple<String, List<Object>>> getSlice(Map<String, Object> outerMetrics)
      throws StatsCollectionFailedException {
    StormInfo info;
    try {
      info = stormInfoSource.fetchInfo();
    } catch (Exception e) {
      throw new StatsCollectionFailedException("Can't gather stats for stmtplinfo.", e);
    }

    final List<Tuple<String, Object>> slice = new FastList<Tuple<String, Object>>();

    for (String topology : info.getTopologies()) {
      for (StormInfoMetricExtractor<Object> extractor : getExtractors(topology)) {
        slice.addAll(extractor.extractForTopologies(topology, info));
      }
    }

    return Utils.groupByFirst(slice);
  }

  private List<StormInfoMetricExtractor<Object>> getExtractors(String topology) {
    List<StormInfoMetricExtractor<Object>> topologyExtractors = new FastList<StormInfoMetricExtractor<Object>>();
    if (!extractors.containsKey(topology)) {
      for (StormTopologyInfoMetric metric : StormTopologyInfoMetric.values()) {
        topologyExtractors.add(metric.stormMetric.createMetricExtractor());
      }

      extractors.put(topology, topologyExtractors);
    }
    return extractors.get(topology);
  }

  @Override
  protected void appendStats(Tuple<String, List<Object>> stats, StatValues statValues) {
    statValues.setMetrics(new UnifiedMap<String, Object>());
    statValues.getMetrics().put("workers", stats.getSecond().get(1));
    statValues.getMetrics().put("executors", stats.getSecond().get(2));
    statValues.getMetrics().put("tasks", stats.getSecond().get(3));
    statValues.getMetrics().put("bolts", stats.getSecond().get(4));
    statValues.getMetrics().put("spouts", stats.getSecond().get(5));
    statValues.getMetrics().put("spouts.state", stats.getSecond().get(6));
    statValues.getMetrics().put("bolts.tuples.emitted", stats.getSecond().get(7));
    statValues.getMetrics().put("bolts.tuples.transferred", stats.getSecond().get(8));
    statValues.getMetrics().put("bolts.acked.count", stats.getSecond().get(9));
    statValues.getMetrics().put("bolts.tuples.executed", stats.getSecond().get(10));
    statValues.getMetrics().put("bolts.failed.count", stats.getSecond().get(11));
    statValues.getMetrics().put("spouts.acked", stats.getSecond().get(12));
    statValues.getMetrics().put("spouts.failed", stats.getSecond().get(13));
    statValues.getMetrics().put("spouts.tuples.emitted", stats.getSecond().get(14));
    statValues.getMetrics().put("spouts.tuples.transferred", stats.getSecond().get(15));
    statValues.getMetrics().put("bolts.tuples.executed.time", stats.getSecond().get(16));
    statValues.getMetrics().put("bolts.tuples.processed.time", stats.getSecond().get(17));
    statValues.getMetrics().put("spouts.complete.time", stats.getSecond().get(18));

    statValues.setTags(new UnifiedMap<String, String>());
    statValues.getTags().put("storm.topology", stats.getFirst());
    statValues.getTags().put("storm.topology.status", String.valueOf(stats.getSecond().get(0)));

    StatValuesHelper.fillEnvTags(statValues, propsFile);
    StatValuesHelper.fillConfigTags(statValues, MonitorUtil.loadMonitorProperties(propsFile));
    statValues.setTimestamp(System.currentTimeMillis());
    statValues.setAppToken(appToken);
    statValues.setMetricNamespace("storm");
  }

  @Override
  public String getName() {
    return "stmshrttplinfo";
  }

  @Override
  public String getCollectorIdentifier() {
    return "StormTopologyStatsCollector";
  }
}
