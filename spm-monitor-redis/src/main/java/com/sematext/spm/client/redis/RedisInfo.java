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

import org.apache.commons.lang.StringUtils;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;
import java.util.Set;

public final class RedisInfo {
  private final Map<String, String> stats;
  private final Map<String, Map<String, String>> dbStats;

  private RedisInfo() {
    stats = new UnifiedMap<String, String>();
    dbStats = new UnifiedMap<String, Map<String, String>>();
  }

  public String get(String metricName) {
    return stats.get(metricName);
  }

  public String get(String db, String metricName) {
    Map<String, String> dbMetrics = dbStats.get(db);
    if (dbMetrics == null) {
      return null;
    }
    return dbMetrics.get(metricName);
  }

  public Set<String> getDatabases() {
    return dbStats.keySet();
  }

  public boolean isEmpty() {
    return stats.isEmpty();
  }

  @SuppressWarnings("unused")
  public static RedisInfo empty() {
    return new RedisInfo();
  }

  public static RedisInfo parse(String info) {
    final RedisInfo redisStats = new RedisInfo();

    String[] lines = info.split("\n");
    for (String line : lines) {
      if (!StringUtils.chomp(line, " ").startsWith("#") && !line.trim().isEmpty()) {
        String[] parts = line.split(":");
        if (parts.length == 2) {
          if (parts[0].matches("^db[0-9]+")) {
            String[] dbPropertiesParts = parts[1].split("[,=]");
            for (int i = 0; i < dbPropertiesParts.length; i += 2) {
              Map<String, String> dbStats = redisStats.dbStats.get(parts[0]);
              if (dbStats == null) {
                dbStats = new UnifiedMap<String, String>();
              }
              dbStats.put(dbPropertiesParts[i], dbPropertiesParts[i + 1].trim());
              redisStats.dbStats.put(parts[0], dbStats);
            }
          } else {
            redisStats.stats.put(parts[0], parts[1].trim());
          }
        }
      }
    }
    return redisStats;
  }
}
