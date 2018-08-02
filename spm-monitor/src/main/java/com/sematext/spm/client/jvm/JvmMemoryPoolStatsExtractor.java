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

import org.apache.commons.lang.StringEscapeUtils;

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

public class JvmMemoryPoolStatsExtractor {
  private static final Log LOG = LogFactory.getLog(JvmMemoryPoolStatsExtractor.class);

  private JmxStatsExtractor jmxStatsExtractor;

  private String poolName;

  class MemoryPoolStats {
    private String poolName;
    private Long used;
    private Long max;

    public String getPoolName() {
      return poolName;
    }

    public void setPoolName(String poolName) {
      this.poolName = poolName;
    }

    public Long getUsed() {
      return used;
    }

    public void setUsed(Long used) {
      this.used = used;
    }

    public Long getMax() {
      return max;
    }

    public void setMax(Long max) {
      this.max = max;
    }

  }

  public JvmMemoryPoolStatsExtractor(String poolName, MonitorConfig monitorConfig)
      throws StatsCollectorBadConfigurationException {
    this.poolName = poolName;
    this.jmxStatsExtractor = new JmxStatsExtractor(getConfig(poolName, monitorConfig));
  }

  public MemoryPoolStats getStats() throws StatsCollectionFailedException {
    MemoryPoolStats stats = new MemoryPoolStats();

    Map<String, Map<String, Object>> jmxStats = jmxStatsExtractor.extractStats(Collections.EMPTY_MAP);
    Map<String, Object> jmxHandlerStats = jmxStats.get("jvmMemoryPool");
    stats.poolName = poolName;
    stats.used = (Long) jmxHandlerStats.get("pool.used");
    stats.max = (Long) jmxHandlerStats.get("pool.max");

    return stats;
  }

  public void cleanup() {
    if (jmxStatsExtractor != null) {
      jmxStatsExtractor.close();
      jmxStatsExtractor = null;
    }
  }

  private static JmxStatsExtractorConfig getConfig(String poolName, MonitorConfig monitorConfig) {
    InputStream is = JvmMemoryPoolStatsExtractor.class.getResourceAsStream("jvm-memorypool-config.yml");
    String configText = FileUtil.readAsString(is);
    configText = configText.replace("${poolName}", StringEscapeUtils.escapeXml(poolName));

    JmxStatsExtractorConfig config;
    try {
      CollectorFileConfig ymlConfig = YamlConfigLoader.load(configText, "jvm-memorypool-config.yml");
      config = new JmxStatsExtractorConfig(ymlConfig, monitorConfig);
    } catch (ConfigurationFailedException e) { // Indicates developer's mistake
      LOG.error(e);
      throw new RuntimeException(e);
    }
    return config;
  }

  public String toString() {
    return "JvmMemoryPoolStatsExtractor: pool: " + poolName;
  }

  public String getPoolName() {
    return poolName;
  }
}
