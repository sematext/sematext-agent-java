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
import java.util.Map;

import com.sematext.spm.client.*;

public class JvmThreadStatsCollector extends SingleStatsCollector {
  private static final Log LOG = LogFactory.getLog(JvmThreadStatsCollector.class);

  private JvmThreadStatsExtractor threadStatsExtractor;
  private final String appToken;
  private final String jvmName;
  private final String subType;
  private final File propsFile;
  private final String finalJvmName;

  public JvmThreadStatsCollector(String appToken, String jvmName, String subType, MonitorConfig monitorConfig)
      throws StatsCollectorBadConfigurationException {
    super(Serializer.INFLUX);
    this.threadStatsExtractor = new JvmThreadStatsExtractor(monitorConfig);
    this.appToken = appToken;
    this.jvmName = jvmName;
    this.subType = subType;
    if (subType == null || subType.trim().equals("")) {
      this.finalJvmName = jvmName;
    } else {
      this.finalJvmName = jvmName + "-" + subType;
    }

    this.propsFile = MonitorUtil.fetchSpmMonitorPropertiesFileObject(appToken, jvmName, subType);
    LOG.info("Thread stats collector initialized");
  }

  @Override
  public void appendStats(StatValues statValues, Map<String, Object> outerMetrics)
      throws StatsCollectionFailedException {
    JvmThreadStatsExtractor.ThreadStats threadStats = threadStatsExtractor.getStats();

    statValues.setMetrics(new UnifiedMap<String, Object>());
    statValues.getMetrics().put("threads", threadStats.getThreadCount());
    statValues.getMetrics().put("threads.peak", threadStats.getPeakThreadCount());
    statValues.getMetrics().put("threads.deamon", threadStats.getDaemonThreadCount());
    statValues.getMetrics().put("threads.started.total", threadStats.getTotalStartedThreadCount());
    statValues.setTags(new UnifiedMap<String, String>());
    statValues.getTags().put(GenericExtractor.JVM_NAME_TAG, finalJvmName);

    StatValuesHelper.fillEnvTags(statValues, propsFile);
    StatValuesHelper.fillConfigTags(statValues, MonitorUtil.loadMonitorProperties(propsFile));
    statValues.setTimestamp(System.currentTimeMillis());
    statValues.setAppToken(appToken);
    statValues.setMetricNamespace("jvm");
  }

  @Override
  public String getName() {
    return "jvmt";
  }

  @Override
  public void cleanup() {
    super.cleanup();
    if (threadStatsExtractor != null) {
      threadStatsExtractor.cleanup();
      threadStatsExtractor = null;
    }
  }

  @Override
  public String getCollectorIdentifier() {
    return jvmName;
  }
}
