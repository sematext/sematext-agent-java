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
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.Serializer;
import com.sematext.spm.client.SingleStatsCollector;
import com.sematext.spm.client.StatValues;
import com.sematext.spm.client.StatValuesHelper;
import com.sematext.spm.client.StatsCollectionFailedException;

public class StormClusterStatsCollector extends SingleStatsCollector implements UpdatableStormStatsCollector {
  private StormInfoSource infoSource;
  private final String appToken;
  private final String jvmName;
  private final String subType;
  private final File propsFile;
  private final List<StormInfoMetricExtractor<Object>> extractors = new FastList<StormInfoMetricExtractor<Object>>();

  @Override
  public void updateSource(StormInfoSource infoSource) {
    this.infoSource = infoSource;
  }

  //ordering matters
  private static enum StormInfoMetric {
    SUPERVISORS_COUNT(com.sematext.spm.client.storm.StormInfoMetric.SUPERVISORS_COUNT),
    TOPOLOGIES_COUNT(com.sematext.spm.client.storm.StormInfoMetric.TOPOLOGIES_COUNT);
    private com.sematext.spm.client.storm.StormInfoMetric stormMetric;

    private StormInfoMetric(com.sematext.spm.client.storm.StormInfoMetric metric) {
      this.stormMetric = metric;
    }
  }

  public StormClusterStatsCollector(StormInfoSource infoSource, String appToken, String jvmName, String subType) {
    // serializer should arrive from the config
    super(Serializer.INFLUX);

    this.infoSource = infoSource;
    this.appToken = appToken;
    this.jvmName = jvmName;
    this.subType = subType;
    this.propsFile = MonitorUtil.fetchSpmMonitorPropertiesFileObject(appToken, jvmName, subType);
    for (StormInfoMetric metric : StormInfoMetric.values()) {
      extractors.add(metric.stormMetric.createMetricExtractor());
    }
  }

  @Override
  protected void appendStats(StatValues statValues, Map<String, Object> outerMetrics)
      throws StatsCollectionFailedException {
    StormInfo info;
    try {
      info = infoSource.fetchInfo();
    } catch (Exception e) {
      throw new StatsCollectionFailedException("Can't grab stats for stminfo.", e);
    }

    statValues.setMetrics(new UnifiedMap<String, Object>());
    statValues.getMetrics().put("supervisors", extractors.get(0).extract(info));
    statValues.getMetrics().put("topologies", extractors.get(1).extract(info));

    statValues.setTags(new UnifiedMap<String, String>());

    StatValuesHelper.fillEnvTags(statValues, propsFile);
    statValues.setTimestamp(System.currentTimeMillis());
    statValues.setAppToken(appToken);
    statValues.setMetricNamespace("storm");
  }

  @Override
  public String getName() {
    return "stmclrinfo";
  }

  @Override
  public String getCollectorIdentifier() {
    return "StormStatsCollector";
  }
}
