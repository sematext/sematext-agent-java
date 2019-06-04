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
package com.sematext.spm.client.haproxy.collector;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.MultipleStatsCollector;
import com.sematext.spm.client.Serializer;
import com.sematext.spm.client.StatValues;
import com.sematext.spm.client.StatValuesHelper;
import com.sematext.spm.client.StatsCollectionFailedException;
import com.sematext.spm.client.haproxy.extractor.HAProxyStatsExtractor;
import com.sematext.spm.client.haproxy.extractor.HAProxyStatsExtractor.HAProxyStats;
import com.sematext.spm.client.http.HttpDataSourceAuthentication;
import com.sematext.spm.client.http.ServerInfo;

public class HAProxyStatsCollector extends MultipleStatsCollector<HAProxyStats> {
  private final String appToken;
  private final String jvmName;
  private final String subType;
  private final File propsFile;

  private HAProxyStatsExtractor extractor;

  public HAProxyStatsCollector(ServerInfo serverInfo, String url, HttpDataSourceAuthentication auth, String appToken,
                               String jvmName, String subType) {
    // serializer should arrive from the config
    super(Serializer.INFLUX);

    this.appToken = appToken;
    this.jvmName = jvmName;
    this.subType = subType;
    this.propsFile = MonitorUtil.fetchSpmMonitorPropertiesFileObject(appToken, jvmName, subType);

    this.extractor = new HAProxyStatsExtractor(serverInfo, url, auth);
  }

  @Override
  protected Collection<HAProxyStats> getSlice(Map<String, Object> outerMetrics) throws StatsCollectionFailedException {
    return extractor.getStats();
  }

  @Override
  protected void appendStats(HAProxyStats stats, StatValues statValues) {
    statValues.setMetrics(new UnifiedMap<String, Object>());
    statValues.getMetrics().put("sessions", stats.getCurSessions());
    statValues.getMetrics().put("sessions.max", stats.getMaxSessions());
    statValues.getMetrics().put("sessions.limit", stats.getSessionsLimit());
    statValues.getMetrics().put("sessions.total", stats.getTotalSessions());
    statValues.getMetrics().put("io.in", stats.getBytesIn());
    statValues.getMetrics().put("io.out", stats.getBytesOut());
    statValues.getMetrics().put("requests.denied", stats.getDeniedRequests());
    statValues.getMetrics().put("responses.denied", stats.getDeniedResponses());
    statValues.getMetrics().put("requests.errors", stats.getReqErrors());
    statValues.getMetrics().put("connections.errors", stats.getConErrors());
    statValues.getMetrics().put("responses.errors", stats.getResErrors());
    statValues.getMetrics().put("retries", stats.getRetries());
    statValues.getMetrics().put("redispatches", stats.getRedispatches());
    statValues.getMetrics().put("server.status", stats.getStatus());
    statValues.getMetrics().put("server.downtime", stats.getDowntime());
    statValues.getMetrics().put("server.weight", stats.getServerWeight());
    statValues.getMetrics().put("server.backend.active", stats.getActive());
    statValues.getMetrics().put("server.backend.backup", stats.getBackup());
    statValues.getMetrics().put("sessions.rate", stats.getRate());
    statValues.getMetrics().put("sessions.rate.max", stats.getRateMax());
    statValues.getMetrics().put("sessions.rate.limit", stats.getRateLimit());
    statValues.getMetrics().put("server.selected", stats.getNumSelected());

    statValues.getMetrics().put("server.queue.requests", stats.getCurQueuedRequests());
    statValues.getMetrics().put("server.queue.requests.max", stats.getMaxQueuedRequests());
    statValues.getMetrics().put("server.checks.failed", stats.getNumFailedChecks());
    statValues.getMetrics().put("server.transitions.updown", stats.getNumUpDownTransitions());
    statValues.getMetrics().put("server.status.change", stats.getLastStatusChange());
    statValues.getMetrics().put("server.queue.requests.limit", stats.getQueueLimit());

    statValues.setTags(new UnifiedMap<String, String>());
    statValues.getTags().put("haproxy.proxy", stats.getProxyName());
    statValues.getTags().put("haproxy.type", stats.getType());
    statValues.getTags().put("haproxy.service", stats.getServiceName());

    StatValuesHelper.fillEnvTags(statValues, propsFile);
    statValues.setTimestamp(System.currentTimeMillis());
    statValues.setAppToken(appToken);
    statValues.setMetricNamespace("haproxy");
  }

  @Override
  public String getName() {
    return "hap";
  }

  @Override
  public String getCollectorIdentifier() {
    return "hap";
  }
}
