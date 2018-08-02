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
package com.sematext.spm.client.jvm;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.StatsCollectionFailedException;
import com.sematext.spm.client.StatsCollectorBadConfigurationException;
import com.sematext.spm.client.config.CollectorFileConfig;
import com.sematext.spm.client.jmx.JmxStatsExtractor;
import com.sematext.spm.client.jmx.JmxStatsExtractorConfig;
import com.sematext.spm.client.util.FileUtil;
import com.sematext.spm.client.yaml.YamlConfigLoader;

public class JvmThreadStatsExtractor {
  private static final Log LOG = LogFactory.getLog(JvmThreadStatsExtractor.class);

  private JmxStatsExtractor jmxStatsExtractor;

  class ThreadStats {
    private Long threadCount;
    private Long peakThreadCount;
    private Long daemonThreadCount;
    private Long totalStartedThreadCount;

    public Long getThreadCount() {
      return threadCount;
    }

    public void setThreadCount(Long threadCount) {
      this.threadCount = threadCount;
    }

    public Long getPeakThreadCount() {
      return peakThreadCount;
    }

    public void setPeakThreadCount(Long peakThreadCount) {
      this.peakThreadCount = peakThreadCount;
    }

    public Long getDaemonThreadCount() {
      return daemonThreadCount;
    }

    public void setDaemonThreadCount(Long daemonThreadCount) {
      this.daemonThreadCount = daemonThreadCount;
    }

    public Long getTotalStartedThreadCount() {
      return totalStartedThreadCount;
    }

    public void setTotalStartedThreadCount(Long totalStartedThreadCount) {
      this.totalStartedThreadCount = totalStartedThreadCount;
    }
  }

  public JvmThreadStatsExtractor(MonitorConfig monitorConfig) throws StatsCollectorBadConfigurationException {
    this.jmxStatsExtractor = new JmxStatsExtractor(getThreadConfig(monitorConfig));
  }

  public ThreadStats getStats() throws StatsCollectionFailedException {
    ThreadStats stats = new ThreadStats();

    Map<String, Map<String, Object>> jmxStats = jmxStatsExtractor.extractStats(Collections.EMPTY_MAP);
    Map<String, Object> jmxHandlerStats = jmxStats.get("jvmThread");
    stats.threadCount = (Long) jmxHandlerStats.get("threads");
    stats.peakThreadCount = (Long) jmxHandlerStats.get("threads.peak");
    stats.daemonThreadCount = (Long) jmxHandlerStats.get("threads.deamon");
    stats.totalStartedThreadCount = (Long) jmxHandlerStats.get("threads.started.total");

    return stats;
  }

  public void cleanup() {
    if (jmxStatsExtractor != null) {
      jmxStatsExtractor.close();
      jmxStatsExtractor = null;
    }
  }

  private static JmxStatsExtractorConfig getThreadConfig(MonitorConfig monitorConfig) {
    InputStream is = JvmThreadStatsExtractor.class.getResourceAsStream("jvm-thread-config.yml");
    String configText = FileUtil.readAsString(is);

    JmxStatsExtractorConfig config;
    try {
      CollectorFileConfig ymlConfig = YamlConfigLoader.load(configText, "jvm-thread-config.yml");
      config = new JmxStatsExtractorConfig(ymlConfig, monitorConfig);
    } catch (ConfigurationFailedException e) { // Indicates developer's mistake
      LOG.error(e);
      throw new RuntimeException(e);
    }
    return config;
  }
}
