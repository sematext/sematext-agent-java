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
package com.sematext.spm.client;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * Has to be set as the last collector in list.
 *
 * @author sematext, http://www.sematext.com/
 */
public class HeartbeatStatsCollector extends MultipleStatsCollector<Integer> {
  private static final Log LOG = LogFactory.getLog(HeartbeatStatsCollector.class);
  
  private static final Collection<Integer> NO_HEARTBEAT = new ArrayList<Integer>();
  private static final Collection<Integer> HEARTBEAT = new ArrayList<Integer>();
  
  static {
    HEARTBEAT.add(1);
  }
  
  private final String appToken;
  private final String jvmName;
  private final String subType;
  private final File propsFile;
  private final String finalJvmName;
  private boolean sendJvmName;

  public HeartbeatStatsCollector(Serializer serializer, String appToken, String jvmName, String subType) {
    super(serializer);

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
      Properties monitorProperties = MonitorUtil.loadMonitorProperties(this.propsFile);
      sendJvmName = "true".equalsIgnoreCase(MonitorUtil.stripQuotes(monitorProperties
          .getProperty("SPM_MONITOR_SEND_JVM_NAME", "false")
          .trim()).trim());
    } catch (Exception e) {
      LOG.warn("Error while reading monitor properties file " + this.propsFile +
          ", sendJvmName will be set to 'false'", e);
      sendJvmName = false; // default
    }
    
    LOG.info("Heartbeat stats collector initialized");
  }
  
  @Override
  public String getName() {
    return this.getClass().getName();
  }

  @Override
  public String getCollectorIdentifier() {
    return jvmName;
  }

  @Override
  protected Collection<Integer> getSlice(Map<String, Object> outerMetrics) throws StatsCollectionFailedException {
    if (CollectionStats.CURRENT_RUN_GATHERED_LINES.get() > 0) {
      return HEARTBEAT;
    } else {
      return NO_HEARTBEAT;
    }
  }

  @Override
  protected void appendStats(Integer protoStats, StatValues statValues) {
    statValues.setMetrics(new UnifiedMap<String, Object>());
    statValues.getMetrics().put("alive", 1);
    statValues.setTags(new UnifiedMap<String, String>());
    
    // currently each agent monitors one specific MonitorConfig, but if we move to 1 agent monitoring
    // N configs, this tag will have to be removed (unless we refactor other things) because
    // CollectionStats.CURRENT_RUN_GATHERED_LINES is on the level of whole agent
    if (sendJvmName) {
      statValues.getTags().put(GenericExtractor.JVM_NAME_TAG, finalJvmName);
    }

    StatValuesHelper.fillEnvTags(statValues, propsFile);
    statValues.setTimestamp(System.currentTimeMillis());
    statValues.setAppToken(appToken);
    statValues.setMetricNamespace("heartbeat");      
  }
}
