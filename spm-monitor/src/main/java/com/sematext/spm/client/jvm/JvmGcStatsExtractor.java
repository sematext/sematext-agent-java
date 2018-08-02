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

public class JvmGcStatsExtractor {
  private static final Log LOG = LogFactory.getLog(JvmGcStatsExtractor.class);

  private JmxStatsExtractor jmxStatsExtractor;
  private String gcName;

  class GcStats {
    private Long collectionCount;
    private Long collectionTime;
    private String gcName;

    public Long getCollectionCount() {
      return collectionCount;
    }

    public void setCollectionCount(Long collectionCount) {
      this.collectionCount = collectionCount;
    }

    public Long getCollectionTime() {
      return collectionTime;
    }

    public void setCollectionTime(Long collectionTime) {
      this.collectionTime = collectionTime;
    }

    public String getGcName() {
      return gcName;
    }

    public void setGcName(String gcName) {
      this.gcName = gcName;
    }
  }

  public JvmGcStatsExtractor(String gcName, MonitorConfig monitorConfig)
      throws StatsCollectorBadConfigurationException {
    this.jmxStatsExtractor = new JmxStatsExtractor(getGcConfig(gcName, monitorConfig));
    this.gcName = gcName;
  }

  public GcStats getStats() throws StatsCollectionFailedException {
    GcStats stats = new GcStats();

    Map<String, Map<String, Object>> jmxStats = jmxStatsExtractor.extractStats(Collections.EMPTY_MAP);
    Map<String, Object> jmxHandlerStats = jmxStats.get("jvmGC");
    stats.collectionCount = (Long) jmxHandlerStats.get("gc.collection.count");
    stats.collectionTime = (Long) jmxHandlerStats.get("gc.collection.time");
    stats.gcName = gcName;

    return stats;
  }

  public void cleanup() {
    if (jmxStatsExtractor != null) {
      jmxStatsExtractor.close();
      jmxStatsExtractor = null;
    }
  }

  private static JmxStatsExtractorConfig getGcConfig(String gcName, MonitorConfig monitorConfig) {
    InputStream is = JvmGcStatsExtractor.class.getResourceAsStream("jvm-gc-config.yml");
    String configText = FileUtil.readAsString(is);
    configText = configText.replace("${gcName}", StringEscapeUtils.escapeXml(gcName));

    JmxStatsExtractorConfig config;
    try {
      CollectorFileConfig ymlConfig = YamlConfigLoader.load(configText, "jvm-gc-config.yml");
      config = new JmxStatsExtractorConfig(ymlConfig, monitorConfig);
    } catch (ConfigurationFailedException e) { // Indicates developer's mistake
      LOG.error(e);
      throw new RuntimeException(e);
    }
    return config;
  }

  public String toString() {
    return "JvmGcStatsExtractor: gc: " + gcName;
  }

  public String getGc() {
    return gcName;
  }

}

