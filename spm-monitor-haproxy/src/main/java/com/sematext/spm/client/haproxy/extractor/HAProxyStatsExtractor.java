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
package com.sematext.spm.client.haproxy.extractor;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.StatsCollectionFailedException;
import com.sematext.spm.client.attributes.LongGaugeValueHolder;
import com.sematext.spm.client.attributes.RealCounterValueHolder;
import com.sematext.spm.client.http.CSVDataProvider;
import com.sematext.spm.client.http.CachableReliableDataSourceBase;
import com.sematext.spm.client.http.HttpDataSourceAuthentication;
import com.sematext.spm.client.http.ServerInfo;

public class HAProxyStatsExtractor {
  private final CachableReliableDataSourceBase<String, CSVDataProvider> source;

  private Map<HAProxyStatsKey, HAProxyStats> stats = Maps.newHashMap();
  private HAProxyStatsKey key = new HAProxyStatsKey();

  private class HAProxyStatsKey {
    private String proxyName;
    private String serviceName;

    public HAProxyStatsKey() {
    }

    public HAProxyStatsKey(HAProxyStatsKey o) {
      this.proxyName = o.proxyName;
      this.serviceName = o.serviceName;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(proxyName, serviceName);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof HAProxyStatsKey)) {
        return false;
      }

      HAProxyStatsKey o = (HAProxyStatsKey) obj;
      return Objects.equal(this.proxyName, o.proxyName) && Objects.equal(this.serviceName, o.serviceName);
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this).add("proxyName", proxyName).add("serviceName", serviceName).toString();
    }
  }

  public class HAProxyStats {
    private final RealCounterValueHolder bytesInHolder = new RealCounterValueHolder();
    private final RealCounterValueHolder bytesOutHolder = new RealCounterValueHolder();
    private final RealCounterValueHolder totalSessionsHolder = new RealCounterValueHolder();
    private final RealCounterValueHolder deniedRequestsHolder = new RealCounterValueHolder();
    private final RealCounterValueHolder deniedResponsesHolder = new RealCounterValueHolder();
    private final RealCounterValueHolder reqErrorsHolder = new RealCounterValueHolder();
    private final RealCounterValueHolder conErrorsHolder = new RealCounterValueHolder();
    private final RealCounterValueHolder resErrorsHolder = new RealCounterValueHolder();
    private final RealCounterValueHolder retriesHolder = new RealCounterValueHolder();
    private final RealCounterValueHolder redispatchesHolder = new RealCounterValueHolder();
    private final RealCounterValueHolder numFailedChecksHolder = new RealCounterValueHolder();
    private final RealCounterValueHolder numUpDownTransitionsHolder = new RealCounterValueHolder();
    private final LongGaugeValueHolder downtimeHolder = new LongGaugeValueHolder();
    private final RealCounterValueHolder numSelectedHolder = new RealCounterValueHolder();

    private String proxyName;
    private String serviceName;
    private String type;
    private Long curQueuedRequests;
    private Long maxQueuedRequests;
    private Long curSessions;
    private Long maxSessions;
    private Long sessionsLimit;
    private Long totalSessions;
    private Long bytesIn;
    private Long bytesOut;
    private Long deniedRequests;
    private Long deniedResponses;
    private Long reqErrors;
    private Long conErrors;
    private Long resErrors;
    private Long retries;
    private Long redispatches;
    private Double status;
    private Long serverWeight;
    private Long active;
    private Long backup;
    private Long numFailedChecks;
    private Long numUpDownTransitions;
    private Long lastStatusChange;
    private Long downtime;
    private Long queueLimit;
    private Long numSelected;
    private Long rate;
    private Long rateLimit;
    private Long rateMax;

    public String getProxyName() {
      return proxyName;
    }

    public String getServiceName() {
      return serviceName;
    }

    public String getType() {
      return type;
    }

    public Long getCurQueuedRequests() {
      return curQueuedRequests;
    }

    public Long getMaxQueuedRequests() {
      return maxQueuedRequests;
    }

    public Long getCurSessions() {
      return curSessions;
    }

    public Long getMaxSessions() {
      return maxSessions;
    }

    public Long getSessionsLimit() {
      return sessionsLimit;
    }

    public Long getTotalSessions() {
      return totalSessions;
    }

    public Long getBytesIn() {
      return bytesIn;
    }

    public Long getBytesOut() {
      return bytesOut;
    }

    public Long getDeniedRequests() {
      return deniedRequests;
    }

    public Long getDeniedResponses() {
      return deniedResponses;
    }

    public Long getReqErrors() {
      return reqErrors;
    }

    public Long getConErrors() {
      return conErrors;
    }

    public Long getResErrors() {
      return resErrors;
    }

    public Long getRetries() {
      return retries;
    }

    public Long getRedispatches() {
      return redispatches;
    }

    public Double getStatus() {
      return status;
    }

    public Long getServerWeight() {
      return serverWeight;
    }

    public Long getActive() {
      return active;
    }

    public Long getBackup() {
      return backup;
    }

    public Long getNumFailedChecks() {
      return numFailedChecks;
    }

    public Long getNumUpDownTransitions() {
      return numUpDownTransitions;
    }

    public Long getLastStatusChange() {
      return lastStatusChange;
    }

    public Long getDowntime() {
      return downtime;
    }

    public Long getQueueLimit() {
      return queueLimit;
    }

    public Long getNumSelected() {
      return numSelected;
    }

    public Long getRate() {
      return rate;
    }

    public Long getRateLimit() {
      return rateLimit;
    }

    public Long getRateMax() {
      return rateMax;
    }
  }

  public HAProxyStatsExtractor(ServerInfo serverInfo, String url, HttpDataSourceAuthentication auth) {
    this.source = new CachableReliableDataSourceBase<String, CSVDataProvider>(new CSVDataProvider(url, auth), false, serverInfo);

  }

  public List<HAProxyStats> getStats() throws StatsCollectionFailedException {
    List<HAProxyStats> results = Lists.newArrayList();

    String content = source.fetchData();
    CSVParser parser = null;
    try {
      parser = CSVParser.parse(content, CSVFormat.DEFAULT.withHeader());
    } catch (IOException e) {
      throw new StatsCollectionFailedException("Data parse failed", e);
    }

    try {
      for (final CSVRecord record : parser) {
        key.proxyName = record.get("# pxname");
        key.serviceName = record.get("svname");

        HAProxyStats stat = stats.get(key);
        if (stat == null) {
          stat = new HAProxyStats();
          HAProxyStatsKey statKey = new HAProxyStatsKey(key);
          stats.put(statKey, stat);
          stat.proxyName = record.get("# pxname");
          stat.serviceName = record.get("svname");
        }

        stat.curQueuedRequests = parseLong(record.get("qcur"));
        stat.maxQueuedRequests = parseLong(record.get("qmax"));
        stat.curSessions = parseLong(record.get("scur"));
        stat.maxSessions = parseLong(record.get("smax"));
        stat.sessionsLimit = parseLong(record.get("slim"));
        stat.totalSessions = parseCounter(record.get("stot"), stat.totalSessionsHolder);
        stat.bytesIn = parseCounter(record.get("bin"), stat.bytesInHolder);
        stat.bytesOut = parseCounter(record.get("bout"), stat.bytesOutHolder);
        stat.deniedRequests = parseCounter(record.get("dreq"), stat.deniedRequestsHolder);
        stat.deniedResponses = parseCounter(record.get("dresp"), stat.deniedResponsesHolder);
        stat.reqErrors = parseCounter(record.get("ereq"), stat.reqErrorsHolder);
        stat.conErrors = parseCounter(record.get("econ"), stat.conErrorsHolder);
        stat.resErrors = parseCounter(record.get("eresp"), stat.resErrorsHolder);
        stat.retries = parseCounter(record.get("wretr"), stat.retriesHolder);
        stat.redispatches = parseCounter(record.get("wredis"), stat.redispatchesHolder);
        String status = record.get("status");
        if ("UP".equals(status) || "OPEN".equals(status)) {
          stat.status = 1d;
        } else if ("DOWN".equals(status)) {
          stat.status = -1d;
        } else {
          stat.status = 0d;
        }
        stat.serverWeight = parseLong(record.get("weight"));
        String active = record.get("act");
        if ("Y".equalsIgnoreCase(active)) {
          stat.active = stat.serverWeight;
        } else {
          stat.active = 0l;
        }
        String backup = record.get("bck");
        if ("Y".equalsIgnoreCase(backup)) {
          stat.backup = stat.serverWeight;
        } else {
          stat.backup = 0l;
        }
        stat.numFailedChecks = parseCounter(record.get("chkfail"), stat.numFailedChecksHolder);
        stat.numUpDownTransitions = parseCounter(record.get("chkdown"), stat.numUpDownTransitionsHolder);
        stat.lastStatusChange = parseLong(record.get("lastchg"));
        stat.downtime = parseGauge(record.get("downtime"), stat.downtimeHolder);
        stat.queueLimit = parseLong(record.get("qlimit"));
        stat.numSelected = parseCounter(record.get("lbtot"), stat.numSelectedHolder);
        String type = record.get("type");
        if ("0".equalsIgnoreCase(type)) {
          stat.type = "FRONTEND";
        } else if ("1".equalsIgnoreCase(type)) {
          stat.type = "BACKEND";
        } else if ("2".equalsIgnoreCase(type)) {
          stat.type = "SERVER";
        } else if ("3".equalsIgnoreCase(type)) {
          stat.type = "SOCKET";
        } else {
          stat.type = "UNKNOWN";
        }
        stat.rate = parseLong(record.get("rate"));
        stat.rateLimit = parseLong(record.get("rate_lim"));
        stat.rateMax = parseLong(record.get("rate_max"));

        results.add(stat);
      }
    } finally {
      try {
        parser.close();
      } catch (IOException e) {
      }
    }

    return results;
  }

  private Long parseGauge(String str, LongGaugeValueHolder holder) {
    if (Strings.isNullOrEmpty(str)) {
      return 0l;
    }

    return holder.getValue(str);
  }

  private Long parseCounter(String str, RealCounterValueHolder holder) {
    if (Strings.isNullOrEmpty(str)) {
      return 0l;
    }

    return holder.getValue(str);
  }

  private Long parseLong(String str) {
    if (Strings.isNullOrEmpty(str)) {
      return null;
    }

    return Long.parseLong(str);
  }
}
