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
package com.sematext.spm.client.sender.config;

import com.sematext.spm.client.sender.SenderUtil;
import com.sematext.spm.client.util.FileUtil;

public final class Configuration {
  private final String baseSenderDir;
  private final String baseMonitorDir;
  private final String applicationsBaseDir;
  private final String senderApplicationsConfTemplateDir;
  private final String defaultReceiverUrl = "https://spm-receiver.sematext.com";
  private final String defaultMetricsEndpoint = SenderUtil.DEFAULT_SAAS_PROD_RECEIVER_METRICS_ENDPOINT;
  private final String defaultTagAliasEndpoint = SenderUtil.DEFAULT_SAAS_PROD_RECEIVER_TAG_ALIASES_ENDPOINT;
  private final String defaultMetainfoEndpoint = SenderUtil.DEFAULT_SAAS_PROD_RECEIVER_METAINFO_ENDPOINT;
  private final String configurationDir;
  private final String propertiesDir;
  private final String monitorConfigDir;
  private final String tracingConfigDir;
  private final String tracingPropertiesFile;

  public Configuration(String spmHome, String sematextHome) {
    this.baseSenderDir = FileUtil.path(spmHome, "spm-sender");
    this.baseMonitorDir = FileUtil.path(spmHome, "spm-monitor");
    this.applicationsBaseDir = FileUtil.path(baseSenderDir, "logs", "applications");
    this.senderApplicationsConfTemplateDir = FileUtil.path(baseSenderDir, "templates");
    this.configurationDir = FileUtil.path(baseSenderDir, "conf");
    this.propertiesDir = FileUtil.path(spmHome, "properties");
    this.tracingConfigDir = FileUtil.path(sematextHome, "tracing", "conf");
    this.monitorConfigDir = FileUtil.path(spmHome, "spm-monitor", "conf");
    this.tracingPropertiesFile = FileUtil.path(propertiesDir, "tracing.properties");
  }

  public String getPropertiesDir() {
    return propertiesDir;
  }

  public String getBaseSenderDir() {
    return baseSenderDir;
  }

  public String getBaseMonitorDir() {
    return baseMonitorDir;
  }

  public String getConfigurationDir() {
    return configurationDir;
  }

  public String getApplicationsBaseDir() {
    return applicationsBaseDir;
  }

  public String getSenderApplicationsConfTemplateDir() {
    return senderApplicationsConfTemplateDir;
  }

  public String getDefaultReceiverUrl() {
    return defaultReceiverUrl;
  }

  public String getDefaultMetricsEndpoint() {
    return defaultMetricsEndpoint;
  }

  public String getDefaultTagAliasEndpoint() {
    return defaultTagAliasEndpoint;
  }

  public String getDefaultMetainfoEndpoint() {
    return defaultMetainfoEndpoint;
  }

  public String getMonitorConfigDir() {
    return monitorConfigDir;
  }

  public String getTracingConfigDir() {
    return tracingConfigDir;
  }

  public String getTracingPropertiesFile() {
    return tracingPropertiesFile;
  }

  public static Configuration defaultConfig() {
    final String homeDir = System.getProperty("spm.home", "/opt/spm");
    final String sematextHomeDir = System.getProperty("sematext.home", "/opt/sematext");
    return new Configuration(homeDir, sematextHomeDir);
  }

  public static Configuration configuration(String spmHome) {
    return new Configuration(spmHome, null);
  }
}
