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
package com.sematext.spm.client.haproxy.factory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.StatsCollector;
import com.sematext.spm.client.StatsCollectorsFactory;
import com.sematext.spm.client.haproxy.collector.HAProxyStatsCollector;
import com.sematext.spm.client.http.HttpDataSourceAuthentication;
import com.sematext.spm.client.http.HttpDataSourceBasicAuthentication;
import com.sematext.spm.client.http.ServerInfo;

public class HAProxyStatsFactory extends StatsCollectorsFactory<StatsCollector<String>> {

  private List<HAProxyStatsCollector> collectors;

  @Override
  public Collection<? extends StatsCollector<String>> create(Properties monitorProperties,
                                                             List<? extends StatsCollector<String>> currentCollectors,
                                                             MonitorConfig monitorConfig) {
    if (collectors == null) {
      String statsUrl = MonitorUtil.stripQuotes(monitorProperties
                                                    .getProperty("SPM_MONITOR_HAPROXY_STATS_URL", "http://localhost/haproxy_stats;csv")
                                                    .trim()).trim();
      String user = MonitorUtil.stripQuotes(monitorProperties.getProperty("SPM_MONITOR_HAPROXY_USER", "").trim())
          .trim();
      String password = MonitorUtil
          .stripQuotesIfEnclosed(monitorProperties.getProperty("SPM_MONITOR_HAPROXY_PASSWORD", ""));

      HttpDataSourceAuthentication auth = null;
      if (user != null && password != null) {
        auth = new HttpDataSourceBasicAuthentication(user, password);
      }

      ServerInfo serverInfo = new ServerInfo(statsUrl, null, null, user, password);

      collectors = Lists
          .newArrayList(new HAProxyStatsCollector(serverInfo, statsUrl, auth, monitorConfig.getAppToken(), monitorConfig
              .getJvmName(),
                                                  monitorConfig.getSubType()));
    }

    return collectors;
  }

  private String getParam(Map<String, String> paramsMap, String name, String defaultValue) {
    String value = paramsMap.get(name);
    if (Strings.isNullOrEmpty(value)) {
      return defaultValue;
    }
    return value.trim();
  }
}
