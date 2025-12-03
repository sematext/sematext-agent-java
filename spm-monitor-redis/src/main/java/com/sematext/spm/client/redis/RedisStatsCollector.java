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
package com.sematext.spm.client.redis;

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

public class RedisStatsCollector extends SingleStatsCollector {
  private final RedisInfoSource infoSource;
  private final List<RedisInfoMetricExtractor<Object>> extractors = new FastList<RedisInfoMetricExtractor<Object>>();
  private final String id;
  private final String appToken;
  private final String jvmName;
  private final String subType;
  private final File propsFile;

  //ordering matters
  private static enum RedisInfoMetric {
    USED_MEMORY(com.sematext.spm.client.redis.RedisInfoMetric.USED_MEMORY),
    USED_MEMORY_PEAK(com.sematext.spm.client.redis.RedisInfoMetric.USED_MEMORY_PEAK),
    USED_MEMORY_RSS(com.sematext.spm.client.redis.RedisInfoMetric.USED_MEMORY_RSS),
    CONNECTED_CLIENTS(com.sematext.spm.client.redis.RedisInfoMetric.CONNECTED_CLIENTS),
    CONNECTED_SLAVES(com.sematext.spm.client.redis.RedisInfoMetric.CONNECTED_SLAVES),
    MASTER_LAST_IO_SECONDS_AGO(com.sematext.spm.client.redis.RedisInfoMetric.MASTER_LAST_IO_SECONDS_AGO),
    KEYSPACE_HITS(com.sematext.spm.client.redis.RedisInfoMetric.KEYSPACE_HITS),
    KEYSPACE_MISSES(com.sematext.spm.client.redis.RedisInfoMetric.KEYSPACE_MISSES),
    EVICTED_KEYS(com.sematext.spm.client.redis.RedisInfoMetric.EVICTED_KEYS),
    EXPIRED_KEYS(com.sematext.spm.client.redis.RedisInfoMetric.EXPIRED_KEYS),
    TOTAL_COMMANDS_PROCESSED(com.sematext.spm.client.redis.RedisInfoMetric.TOTAL_COMMANDS_PROCESSED);

    private com.sematext.spm.client.redis.RedisInfoMetric redisMetric;

    private RedisInfoMetric(com.sematext.spm.client.redis.RedisInfoMetric metric) {
      this.redisMetric = metric;
    }
  }

  public RedisStatsCollector(RedisInfoSource infoSource, String id, String appToken, String jvmName, String subType) {
    // serializer should arrive from the config
    super(Serializer.INFLUX);

    this.id = id;
    this.infoSource = infoSource;
    for (RedisInfoMetric metric : RedisStatsCollector.RedisInfoMetric.values()) {
      extractors.add(metric.redisMetric.createMetricExtractor());
    }
    this.appToken = appToken;
    this.jvmName = jvmName;
    this.subType = subType;
    this.propsFile = MonitorUtil.fetchSpmMonitorPropertiesFileObject(appToken, jvmName, subType);
  }

  @Override
  protected void appendStats(StatValues statValues, Map<String, Object> outerMetrics)
      throws StatsCollectionFailedException {
    RedisInfo info;
    try {
      info = infoSource.fetchInfo();
    } catch (Exception e) {
      throw new StatsCollectionFailedException("Can't grab stats for rdsinfo.", e);
    }

    statValues.setMetrics(new UnifiedMap<String, Object>());
    statValues.getMetrics().put("memory.used", extractors.get(0).extract(info));
    statValues.getMetrics().put("memory.used.max", extractors.get(1).extract(info));
    statValues.getMetrics().put("memory.used.rss", extractors.get(2).extract(info));
    statValues.getMetrics().put("clients.connected", extractors.get(3).extract(info));
    statValues.getMetrics().put("replication.slaves.connected", extractors.get(4).extract(info));
    statValues.getMetrics().put("replication.master.last.io.seconds.ago", extractors.get(5).extract(info));
    statValues.getMetrics().put("keyspace.hits", extractors.get(6).extract(info));
    statValues.getMetrics().put("keyspace.misses", extractors.get(7).extract(info));
    statValues.getMetrics().put("keyspace.evicted", extractors.get(8).extract(info));
    statValues.getMetrics().put("keyspace.expired", extractors.get(9).extract(info));
    statValues.getMetrics().put("commands.processed", extractors.get(10).extract(info));

    statValues.setTags(new UnifiedMap<String, String>());

    StatValuesHelper.fillEnvTags(statValues, propsFile);
    StatValuesHelper.fillConfigTags(statValues, MonitorUtil.loadMonitorProperties(propsFile));
    statValues.setTimestamp(System.currentTimeMillis());
    statValues.setAppToken(appToken);
    statValues.setMetricNamespace("redis");
  }

  @Override
  public String getName() {
    return "rdsinfo";
  }

  @Override
  public String getCollectorIdentifier() {
    return id;
  }
}
