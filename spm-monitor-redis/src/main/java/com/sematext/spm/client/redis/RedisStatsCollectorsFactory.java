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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.sematext.spm.client.HeartbeatStatsCollector;
import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.Serializer;
import com.sematext.spm.client.StatsCollector;
import com.sematext.spm.client.StatsCollectorBadConfigurationException;
import com.sematext.spm.client.StatsCollectorsFactory;
import com.sematext.spm.client.util.CollectionUtils.FunctionT;

public class RedisStatsCollectorsFactory extends StatsCollectorsFactory<StatsCollector> {
  private static String makeId(String host, String port, String password) {
    return host + ":" + port + "@" + password;
  }

  private static Set<String> makeCollectorIds(String collectorIdentifier) {
    Set<String> ids = new HashSet<String>();
    ids.add(StatsCollector.calculateIdForCollector(RedisStatsCollector.class, collectorIdentifier));
    ids.add(StatsCollector.calculateIdForCollector(RedisDbStatsCollector.class, collectorIdentifier));
    return ids;
  }

  @Override
  public Collection<? extends StatsCollector> create(Properties monitorProperties,
                                                     List<? extends StatsCollector> existingCollectors,
                                                     MonitorConfig monitorConfig)
      throws StatsCollectorBadConfigurationException {

    Integer redisPort = null;
    String hostParam = MonitorUtil.stripQuotes(monitorProperties.getProperty("REDIS_HOST", "localhost").trim()).trim();
    String passwordParam = MonitorUtil.stripQuotesIfEnclosed(monitorProperties.getProperty("REDIS_PASSWORD", ""));
    String portParam = MonitorUtil.stripQuotes(monitorProperties.getProperty("REDIS_PORT", "6379").trim()).trim();

    if (portParam != null && !portParam.isEmpty()) {
      try {
        redisPort = Integer.parseInt(portParam);
      } catch (NumberFormatException e) {
        throw new StatsCollectorBadConfigurationException("Can't parse redisPort parameter " + portParam + ".");
      }
    }

    final String id = makeId(hostParam, portParam, passwordParam);
    final Set<String> collectorIds = makeCollectorIds(id);
    if (existingCollectors != null) {
      for (StatsCollector collector : existingCollectors) {
        collectorIds.remove(collector.getId());
      }
    }

    if (collectorIds.isEmpty()) {
      return existingCollectors;
    } else {
      final RedisInfoSource infoSource = RedisInfoSource.collector(hostParam, redisPort, passwordParam);
      return Arrays
          .asList(new RedisStatsCollector(infoSource, id, monitorConfig.getAppToken(), monitorConfig.getJvmName(),
                                          monitorConfig.getSubType()),
                  new RedisDbStatsCollector(infoSource, id, monitorConfig.getAppToken(),monitorConfig.getJvmName(),
                                            monitorConfig.getSubType()),
                  // as last collector add HeartbeatCollector
                  new HeartbeatStatsCollector(
                      Serializer.INFLUX, monitorConfig.getAppToken(), monitorConfig.getJvmName(),
                      monitorConfig.getSubType()));
    }
  }
}
