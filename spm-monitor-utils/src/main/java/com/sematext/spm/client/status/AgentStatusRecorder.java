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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.util.FileUtil;

public class AgentStatusRecorder {
  // static to be accessible from everywhere (since each agent monitors just one thing), but should be refactored so
  // it isn't static
  public static AgentStatusRecorder GLOBAL_INSTANCE;

  private static final Log LOG = LogFactory.getLog(AgentStatusRecorder.class);
  private static final long CONNECTION_OK_EXPIRY_TIME_MS = 5 * 60 * 1000;

  public enum ConnectionStatus {
    OK,
    CONNECTING,
    FAILED
  }
  
  public enum StatusField {
    STARTED_AT("startedAt", 1),
    LAST_UPDATE("lastUpdate", 2),
    METRICS_COLLECTED("metricsCollected", 3),
    METRICS_SENT("metricsSent", 4),
    CONNECTION_STATUS("connectionStatus", 5),
    CONNECTION_ERRORS("connectionErrors", 6);
    
    private String displayName;
    private int position;
    
    private StatusField(String displayName, int position) {
      this.displayName = displayName;
      this.position = position;
    }
    
    public int getPosition() {
      return position;
    }
    
    public String getDisplayName() {
      return displayName;
    }
  }

  private Comparator<StatusField> statusFieldComparator = new Comparator<AgentStatusRecorder.StatusField>() {
    @Override
    public int compare(StatusField o1, StatusField o2) {
      return o1.getPosition() - o2.getPosition();
    }
  };
  
  private String appToken;
  private String jvmName;
  private String subType;
  private Integer processOrdinal;
  private Map<StatusField, Object> statusValues = new TreeMap<StatusField, Object>(statusFieldComparator);
  
  private File statusFile;
  
  private long lastConnectionOkStatusTime = 0l;
  
  public AgentStatusRecorder(String appToken, File monitorPropertiesFile, Integer processOrdinal) {
    this.appToken = appToken;
    this.jvmName = MonitorUtil.extractJvmName(monitorPropertiesFile.getAbsolutePath(), appToken);
    this.subType = MonitorUtil.extractConfSubtype(monitorPropertiesFile.getAbsolutePath());
    this.processOrdinal = processOrdinal;
    
    this.statusFile = getStatusFile();
    
    statusValues.put(StatusField.STARTED_AT, new Date());
    statusValues.put(StatusField.LAST_UPDATE, new Date());
    statusValues.put(StatusField.METRICS_COLLECTED, false);
    statusValues.put(StatusField.METRICS_SENT, false);
    statusValues.put(StatusField.CONNECTION_STATUS, ConnectionStatus.CONNECTING);
    statusValues.put(StatusField.CONNECTION_ERRORS, new HashMap<String, Date>());

    GLOBAL_INSTANCE = this;
    
    record();
  }
  
  public void updateConnectionStatus(ConnectionStatus newStatus) {
    ConnectionStatus previousStatus = (ConnectionStatus) statusValues.get(StatusField.CONNECTION_STATUS);
    Date now = new Date();
    
    if (previousStatus == ConnectionStatus.CONNECTING || previousStatus == ConnectionStatus.FAILED) {
      // we can update in any case
      statusValues.put(StatusField.CONNECTION_STATUS, newStatus);  
    } else {
      // if connection was OK before, but now not anymore, we should wait for CONNECTION_OK_EXPIRY_TIME_MS before
      // setting to failed (e.g. in case of json some URLs may be ok, some not because of new version of monitored
      // service)
      if (newStatus == ConnectionStatus.FAILED) {
        if ((now.getTime() - CONNECTION_OK_EXPIRY_TIME_MS) > lastConnectionOkStatusTime) {
          statusValues.put(StatusField.CONNECTION_STATUS, newStatus);  
        }
      } else {
        statusValues.put(StatusField.CONNECTION_STATUS, newStatus);
      }
    }
    
    if (newStatus == ConnectionStatus.OK) {
      lastConnectionOkStatusTime = System.currentTimeMillis();
    }

    statusValues.put(StatusField.LAST_UPDATE, now);
    
    record();
  }

  public void updateConnectionStatus(ConnectionStatus newStatus, String newError) {
    Map<String, Date> connErrors = (Map<String, Date>) statusValues.get(StatusField.CONNECTION_ERRORS);
    connErrors.put(newError, new Date());
    updateConnectionStatus(newStatus);
  }

  public void updateMetricsCollected(boolean metricsCollected) {
    statusValues.put(StatusField.METRICS_COLLECTED, metricsCollected);
    statusValues.put(StatusField.LAST_UPDATE, new Date());
    record();
  }

  public void updateMetricsSent(boolean metricsSent) {
    statusValues.put(StatusField.METRICS_SENT, metricsSent);
    statusValues.put(StatusField.LAST_UPDATE, new Date());
    record();
  }

  public void addConnectionError(String errorMessage) {
    Map<String, Date> connErrors = (Map<String, Date>) statusValues.get(StatusField.CONNECTION_ERRORS);
    connErrors.put(errorMessage, new Date());

    statusValues.put(StatusField.LAST_UPDATE, new Date());
    
    record();
  }
  
  private void record() {
    StringBuffer content = new StringBuffer(100);
    for (StatusField key : statusValues.keySet()) {
      content.append(key.getDisplayName());
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
