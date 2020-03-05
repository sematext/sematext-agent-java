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

public class StormSupervisorStatsCollector extends MultipleStatsCollector<Tuple<String, List<Object>>>
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
  private static enum StormSupervisorInfoMetric {
    SUPERVISOR_HOST(StormInfoMetric.SUPERVISOR_HOST),
    SLOTS_COUNT(StormInfoMetric.SLOTS_COUNT),
    USED_SLOTS_COUNT(StormInfoMetric.USED_SLOTS_COUNT);

    private StormInfoMetric stormMetric;

    private StormSupervisorInfoMetric(StormInfoMetric stormMetric) {
      this.stormMetric = stormMetric;
    }
  }

  public StormSupervisorStatsCollector(StormInfoSource infoSource, String appToken, String jvmName, String subType) {
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
      throw new StatsCollectionFailedException("Can't gather stats for stmspvinfo.", e);
    }

    final List<Tuple<String, Object>> slice = new FastList<Tuple<String, Object>>();

    for (String supervisor : info.getSupervisors()) {
      List<StormInfoMetricExtractor<Object>> stormInfoMetricExtractors = getExtractors(supervisor);
      for (StormInfoMetricExtractor<Object> extractor : stormInfoMetricExtractors) {
        slice.addAll(extractor.extractForSupervisors(supervisor, info));
      }
    }

    return Utils.groupByFirst(slice);
  }

  private List<StormInfoMetricExtractor<Object>> getExtractors(String supervisor) {
    List<StormInfoMetricExtractor<Object>> supervisorExtractors = new FastList<StormInfoMetricExtractor<Object>>();
    if (!extractors.containsKey(supervisor)) {
      for (StormSupervisorInfoMetric metric : StormSupervisorInfoMetric.values()) {
        supervisorExtractors.add(metric.stormMetric.createMetricExtractor());
      }

      extractors.put(supervisor, supervisorExtractors);
    }
    return extractors.get(supervisor);
  }

  @Override
  protected void appendStats(Tuple<String, List<Object>> stats, StatValues statValues) {
    statValues.add(stats.getFirst());
    for (Object value : stats.getSecond()) {
      statValues.add(value);
    }
    statValues.setMetrics(new UnifiedMap<String, Object>());
    statValues.getMetrics().put("slots", stats.getSecond().get(1));
    statValues.getMetrics().put("slots.used", stats.getSecond().get(2));

    statValues.setTags(new UnifiedMap<String, String>());
    statValues.getTags().put("storm.supervisor.id", stats.getFirst());
    statValues.getTags().put("storm.supervisor.host", String.valueOf(stats.getSecond().get(0)));

    StatValuesHelper.fillEnvTags(statValues, propsFile);
    StatValuesHelper.fillConfigTags(statValues, MonitorUtil.loadMonitorProperties(propsFile));
    statValues.setTimestamp(System.currentTimeMillis());
    statValues.setAppToken(appToken);
    statValues.setMetricNamespace("storm");
  }

  @Override
  public String getName() {
    return "stmsprinfo";
  }

  @Override
  public String getCollectorIdentifier() {
    return "StormSupervisorStatsCollector";
  }
}
