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

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sematext.spm.client.*;
import com.sematext.spm.client.config.CollectorFileConfig;
import com.sematext.spm.client.jmx.JmxStatsExtractor;
import com.sematext.spm.client.jmx.JmxStatsExtractorConfig;
import com.sematext.spm.client.util.FileUtil;
import com.sematext.spm.client.yaml.YamlConfigLoader;

public class JvmOsStatsCollector extends SingleStatsCollector {

  private final String appToken;
  private final String jvmName;
  private final String subType;
  private final File propsFile;
  private final String finalJvmName;

  private final JmxStatsExtractor statsExtractor;

  private static final Log LOG = LogFactory.getLog(JvmOsStatsCollector.class);

  public JvmOsStatsCollector(String appToken, String jvmName, String subType, MonitorConfig monitorConfig)
      throws StatsCollectorBadConfigurationException {
    super(Serializer.INFLUX);
    this.appToken = appToken;
    this.jvmName = jvmName;
    this.subType = subType;
    if (subType == null || subType.trim().equals("")) {
      this.finalJvmName = jvmName;
    } else {
      this.finalJvmName = jvmName + "-" + subType;
    }

    this.propsFile = MonitorUtil.fetchSpmMonitorPropertiesFileObject(appToken, jvmName, subType);

    try {
      InputStream is = JvmMemoryStatsExtractor.class.getResourceAsStream("jvm-os-config.yml");
      String configText = FileUtil.readAsString(is);
      JmxStatsExtractorConfig config;
      CollectorFileConfig ymlConfig = YamlConfigLoader.load(configText, "jvm-os-config.yml");
      config = new JmxStatsExtractorConfig(ymlConfig, monitorConfig);
      this.statsExtractor = new JmxStatsExtractor(config);
    } catch (ConfigurationFailedException e) { // Indicates developer's mistake
      LOG.error(e);
      throw new StatsCollectorBadConfigurationException(
          "Error while configuring " + JvmOsStatsCollector.class.getName(), e);
    }
  }

  @Override
  protected void appendStats(StatValues statValues, Map<String, Object> outerMetrics)
      throws StatsCollectionFailedException {
    Map<String, Object> stats = getStats();
    Long openFileDescriptorsCount = (Long) stats.get("files.open");
    Long maxFileDescriptorsCount = (Long) stats.get("files.max");

    // OperatingSystem system MBean contains a dozen of additional params,
    // but we are interested only in open files handlers.
    statValues.setMetrics(new UnifiedMap<String, Object>());
    statValues.getMetrics().put("files.open", openFileDescriptorsCount);
    statValues.getMetrics().put("files.max", maxFileDescriptorsCount);
    statValues.setTags(new UnifiedMap<String, String>());
    statValues.getTags().put(GenericExtractor.JVM_NAME_TAG, finalJvmName);

    StatValuesHelper.fillHostTags(statValues, propsFile);
    statValues.setTimestamp(System.currentTimeMillis());
    statValues.setAppToken(appToken);
    statValues.setMetricNamespace("jvm");
  }

  private Map<String, Object> getStats() throws StatsCollectionFailedException {
    Map<String, Map<String, Object>> res = statsExtractor.extractStats(Collections.EMPTY_MAP);
    return res.get("jvmOS");
  }

  @Override
  public String getName() {
    return "jvmos";
  }

  @Override
  public String getCollectorIdentifier() {
    return jvmName;
  }

  @Override
  public void cleanup() {
    super.cleanup();
    statsExtractor.close();
  }

  private static AtomicBoolean ACCEPTABLE = null;
  private static final AtomicInteger COUNT_FAILED_ACCEPTABLE_TRIES = new AtomicInteger(0);
  private static final int MAX_COUNT_FAILED_ACCEPTABLE_TRIES = 20;

  public boolean isAcceptable() {
    if (ACCEPTABLE != null) {
      return ACCEPTABLE.get();
    }

    try {
      Map<String, Object> stats = getStats();
      ACCEPTABLE = new AtomicBoolean(true);
      return true;
    } catch (MonitoredServiceUnavailableException msue) {
      // no logging needed in this case
      // no increment of tries either
      return false;
    } catch (Throwable thr) {
      LOG.warn("JvmOsStatsCollector is not acceptable for this OS", thr);
    }

    // we could get here only in case of error
    if (COUNT_FAILED_ACCEPTABLE_TRIES.incrementAndGet() >= MAX_COUNT_FAILED_ACCEPTABLE_TRIES) {
      LOG.error("Permanently marking " + this.getClass().getName() + " as unavailable for this OS since " +
                    "MAX_COUNT_FAILED_ACCEPTABLE_TRIES was exceeded");
      ACCEPTABLE = new AtomicBoolean(false);
    }

    return false;
  }

}
