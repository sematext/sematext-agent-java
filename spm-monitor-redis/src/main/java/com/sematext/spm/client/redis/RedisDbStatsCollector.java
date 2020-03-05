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

public class RedisDbStatsCollector extends MultipleStatsCollector<Tuple<String, List<Object>>> {
  private final RedisInfoSource redisInfoSource;
  private final List<RedisInfoMetricExtractor<Object>> extractors = new FastList<RedisInfoMetricExtractor<Object>>();
  private final String id;
  private final String appToken;
  private final String jvmName;
  private final String subType;
  private final File propsFile;

  //ordering matters
  private static enum RedisDbInfoMetric {
    KEYS(RedisInfoMetric.DB_KEYS),
    KEYS_EXPIRES(RedisInfoMetric.DB_KEYS_EXPIRES);

    private RedisInfoMetric redisMetric;

    private RedisDbInfoMetric(RedisInfoMetric redisMetric) {
      this.redisMetric = redisMetric;
    }
  }

  public RedisDbStatsCollector(RedisInfoSource infoSource, String id, String appToken, String jvmName, String subType) {
    // serializer should arrive from the config
    super(Serializer.INFLUX);

    this.id = id;
    this.redisInfoSource = infoSource;
    for (RedisDbInfoMetric metric : RedisDbInfoMetric.values()) {
      extractors.add(metric.redisMetric.createMetricExtractor());
    }
    this.appToken = appToken;
    this.jvmName = jvmName;
    this.subType = subType;
    this.propsFile = MonitorUtil.fetchSpmMonitorPropertiesFileObject(appToken, jvmName, subType);
  }

  @Override
  protected Collection<Tuple<String, List<Object>>> getSlice(Map<String, Object> outerMetrics)
      throws StatsCollectionFailedException {
    RedisInfo info;
    try {
      info = redisInfoSource.fetchInfo();
    } catch (Exception e) {
      throw new StatsCollectionFailedException("Can't gather stats for rdsdbinfo.", e);
    }

    final List<Tuple<String, Object>> slice = new FastList<Tuple<String, Object>>();
    for (RedisInfoMetricExtractor<Object> extractor : extractors) {
      @SuppressWarnings("unchecked")
      final List<Tuple<String, Object>> extracted = (List<Tuple<String, Object>>) extractor.extract(info);
      slice.addAll(extracted);
    }
    return Utils.groupByFirst(slice);
  }

  @Override
  protected void appendStats(Tuple<String, List<Object>> stats, StatValues statValues) {
    statValues.setMetrics(new UnifiedMap<String, Object>());
    statValues.getMetrics().put("keyspace.keys", stats.getSecond().get(0));
    statValues.getMetrics().put("keyspace.keys.expiring", stats.getSecond().get(1));

    statValues.setTags(new UnifiedMap<String, String>());
    statValues.getTags().put("redis.db", stats.getFirst());

    StatValuesHelper.fillEnvTags(statValues, propsFile);
    StatValuesHelper.fillConfigTags(statValues, MonitorUtil.loadMonitorProperties(propsFile));
    statValues.setTimestamp(System.currentTimeMillis());
    statValues.setAppToken(appToken);
    statValues.setMetricNamespace("redis");
  }

  @Override
  public String getName() {
    return "rdsdbinfo";
  }

  @Override
  public String getCollectorIdentifier() {
    return id;
  }
}
