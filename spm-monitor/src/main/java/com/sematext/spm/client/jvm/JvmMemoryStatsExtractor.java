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
import com.sematext.spm.client.config.CollectorFileConfig;
import com.sematext.spm.client.jmx.JmxStatsExtractor;
import com.sematext.spm.client.jmx.JmxStatsExtractorConfig;
import com.sematext.spm.client.util.FileUtil;
import com.sematext.spm.client.yaml.YamlConfigLoader;

public class JvmMemoryStatsExtractor {
  private static final Log LOG = LogFactory.getLog(JvmMemoryStatsExtractor.class);

  private JmxStatsExtractor jmxStatsExtractor;

  class MemoryStats {
    private Long heapUsed;
    private Long nonHeapUsed;

    public Long getHeapUsed() {
      return heapUsed;
    }

    public void setHeapUsed(Long heapUsed) {
      this.heapUsed = heapUsed;
    }

    public Long getNonHeapUsed() {
      return nonHeapUsed;
    }

    public void setNonHeapUsed(Long nonHeapUsed) {
      this.nonHeapUsed = nonHeapUsed;
    }
  }

  public JvmMemoryStatsExtractor(MonitorConfig monitorConfig) {
    this.jmxStatsExtractor = new JmxStatsExtractor(getMemoryConfig(monitorConfig));
  }

  public MemoryStats getStats() throws StatsCollectionFailedException {
    MemoryStats stats = new MemoryStats();

    Map<String, Map<String, Object>> jmxStats = jmxStatsExtractor.extractStats(Collections.EMPTY_MAP);
    Map<String, Object> jmxHandlerStats = jmxStats.get("jvmMemory");
    stats.heapUsed = (Long) jmxHandlerStats.get("heap.used");
    stats.nonHeapUsed = (Long) jmxHandlerStats.get("nonheap.used");

    return stats;
  }

  public void cleanup() {
    if (jmxStatsExtractor != null) {
      jmxStatsExtractor.close();
      jmxStatsExtractor = null;
    }
  }

  private static JmxStatsExtractorConfig getMemoryConfig(MonitorConfig monitorConfig) {
    InputStream is = JvmMemoryStatsExtractor.class.getResourceAsStream("jvm-memory-config.yml");
    String configText = FileUtil.readAsString(is);

    JmxStatsExtractorConfig config;
    try {
      CollectorFileConfig ymlConfig = YamlConfigLoader.load(configText, "jvm-memory-config.yml");
      config = new JmxStatsExtractorConfig(ymlConfig, monitorConfig);
    } catch (ConfigurationFailedException e) { // Indicates developer's mistake
      LOG.error(e);
      throw new RuntimeException(e);
    }
    return config;
  }
}
