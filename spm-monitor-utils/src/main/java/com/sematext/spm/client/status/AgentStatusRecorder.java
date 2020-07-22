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
package com.sematext.spm.client.status;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.util.FileUtil;

public class AgentStatusRecorder {
  private static final Log LOG = LogFactory.getLog(AgentStatusRecorder.class);

  public enum ConnectionStatus {
    OK,
    CONNECTING,
    FAILED
  }
  
  public static final String LAST_UPDATE = "lastUpdate";
  public static final String STARTED_AT = "startedAt";
  public static final String CONNECTION_ERRORS = "connectionErrors";
  public static final String METRICS_COLLECTED = "metricsCollected";
  public static final String METRICS_SENT = "metricsSent";

  public static final String CONNECTION_STATUS = "connectionStatus";

  private String appToken;
  private String jvmName;
  private String subType;
  private Integer processOrdinal;
  private Map<String, Object> statusValues = new TreeMap<String, Object>();
  
  private File statusFile;
  

  public AgentStatusRecorder(String appToken, File monitorPropertiesFile, Integer processOrdinal) {
    this.appToken = appToken;
    this.jvmName = MonitorUtil.extractJvmName(monitorPropertiesFile.getAbsolutePath(), appToken);
    this.subType = MonitorUtil.extractConfSubtype(monitorPropertiesFile.getAbsolutePath());
    this.processOrdinal = processOrdinal;
    
    this.statusFile = getStatusFile();
    
    statusValues.put(STARTED_AT, new Date());
    statusValues.put(LAST_UPDATE, new Date());
    statusValues.put(METRICS_COLLECTED, false);
    statusValues.put(METRICS_SENT, false);
    statusValues.put(CONNECTION_STATUS, ConnectionStatus.CONNECTING);
    statusValues.put(CONNECTION_ERRORS, new HashMap<String, Date>());
    
    record();
  }
  
  public void updateConnectionStatus(ConnectionStatus newStatus) {
    statusValues.put(CONNECTION_STATUS, newStatus);
    statusValues.put(LAST_UPDATE, new Date());

    record();
  }
  
  public void updateMetricsCollectedSent(boolean metricsCollected, boolean metricSent) {
    statusValues.put(METRICS_COLLECTED, metricsCollected);
    statusValues.put(METRICS_SENT, metricSent);
    statusValues.put(LAST_UPDATE, new Date());
  }
  
  public void addConnectionError(String errorMessage) {
    Map<String, Date> connErrors = (Map<String, Date>) statusValues.get(CONNECTION_ERRORS);
    connErrors.put(errorMessage, new Date());

    statusValues.put(LAST_UPDATE, new Date());
    
    record();
  }
  
  private void record() {
    StringBuffer content = new StringBuffer(100);
    for (String key : statusValues.keySet()) {
      content.append(key);
      content.append("=");
      // raw serialization for now, but consider serializing e.g. errors differently
      content.append(statusValues.get(key));
      content.append(MonitorUtil.LINE_SEPARATOR);
    }
    
    try {
      FileUtil.write(content.toString(), statusFile);
    } catch (IOException e) {
      LOG.error("Can't write to status file", e);
    }
  }

  private File getStatusFile() {
    return new File(MonitorUtil.MONITOR_STATUS_FILE_DIR, getStatusFileName());
  }

  private String getStatusFileName() {
    return appToken + "-" + subType + "-" + jvmName + "-monitor-" + processOrdinal + ".status";
  }
}
