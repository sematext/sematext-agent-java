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

public class StormBoltInputStatsCollector extends MultipleStatsCollector<Tuple<List<String>, List<Object>>>
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
  private static enum StormBoltInfoMetric {
    BOLTS_ASKED(StormInfoMetric.BOLTS_ASKED),
    BOLTS_EXECUTED(StormInfoMetric.BOLTS_EXECUTED),
    BOLTS_FAILED(StormInfoMetric.BOLTS_FAILED),
    BOLTS_EXECUTED_LATENCY(StormInfoMetric.BOLTS_EXECUTED_LATENCY),
    BOLTS_PROCESSED_LATENCY(StormInfoMetric.BOLTS_PROCESSED_LATENCY);

    private StormInfoMetric stormMetric;

    private StormBoltInfoMetric(StormInfoMetric stormMetric) {
      this.stormMetric = stormMetric;
    }
  }

  public StormBoltInputStatsCollector(StormInfoSource infoSource, String appToken, String jvmName, String subType) {
    // serializer should arrive from the config
    super(Serializer.INFLUX);

    this.stormInfoSource = infoSource;
    this.appToken = appToken;
    this.jvmName = jvmName;
    this.subType = subType;
    this.propsFile = MonitorUtil.fetchSpmMonitorPropertiesFileObject(appToken, jvmName, subType);
  }

  @Override
  protected Collection<Tuple<List<String>, List<Object>>> getSlice(Map<String, Object> outerMetrics)
      throws StatsCollectionFailedException {
    StormInfo info;
    try {
      info = stormInfoSource.fetchInfo();
    } catch (Exception e) {
      throw new StatsCollectionFailedException("Can't gather stats for stmbltinfo.", e);
    }

    final List<Tuple<List<String>, Object>> slice = new FastList<Tuple<List<String>, Object>>();

    for (String topology : info.getTopologies()) {
      for (ExecutorInputStatsKey statsKey : info.getBoltInputStats(topology)) {
        for (StormInfoMetricExtractor<Object> extractor : getExtractors(topology, statsKey)) {
          slice.addAll(extractor.extractForBoltsInputStats(topology, statsKey, info));
        }
      }
    }

    return Utils.groupByFirst(slice);
  }

  private List<StormInfoMetricExtractor<Object>> getExtractors(String topology, ExecutorInputStatsKey statsKey) {
    List<StormInfoMetricExtractor<Object>> topologyExtractors = new FastList<StormInfoMetricExtractor<Object>>();
    if (!extractors.containsKey(topology + ":" + statsKey.toKey())) {
      for (StormBoltInfoMetric metric : StormBoltInfoMetric.values()) {
        topologyExtractors.add(metric.stormMetric.createMetricExtractor());
      }

      extractors.put(topology + ":" + statsKey.toKey(), topologyExtractors);
    }
    return extractors.get(topology + ":" + statsKey.toKey());
  }

  @Override
  protected void appendStats(Tuple<List<String>, List<Object>> stats, StatValues statValues) {
    statValues.setMetrics(new UnifiedMap<String, Object>());
    statValues.getMetrics().put("bolts.acked", stats.getSecond().get(0));
    statValues.getMetrics().put("bolts.tuples.executed", stats.getSecond().get(1));
    statValues.getMetrics().put("bolts.failed.count", stats.getSecond().get(2));
    statValues.getMetrics().put("bolts.tuples.executed.time", stats.getSecond().get(3));
    statValues.getMetrics().put("bolts.tuples.processed.time", stats.getSecond().get(4));

    statValues.setTags(new UnifiedMap<String, String>());
    statValues.getTags().put("storm.topology", stats.getFirst().get(0));
    statValues.getTags().put("storm.bolt", stats.getFirst().get(1));
    statValues.getTags().put("storm.bolt.stream", stats.getFirst().get(2));
    statValues.getTags().put("storm.bolt.executor.id", stats.getFirst().get(3));
    statValues.getTags().put("storm.bolt.stream.component", stats.getFirst().get(4));

    StatValuesHelper.fillHostTags(statValues, propsFile);
    statValues.setTimestamp(System.currentTimeMillis());
    statValues.setAppToken(appToken);
    statValues.setMetricNamespace("storm");
  }

  @Override
  public String getName() {
    return "stmbltinstatsinfo";
  }

  @Override
  public String getCollectorIdentifier() {
    return "StormBoltInputStatsStatsCollector";
  }
}
