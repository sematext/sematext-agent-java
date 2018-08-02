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
package com.sematext.spm.client.tracing.agent.config;

import java.io.File;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.monitor.MonitorFileNames;
import com.sematext.spm.client.util.AgentArgsParser;
import com.sematext.spm.client.util.FileUtil;
import com.sematext.spm.client.util.PropertiesReader;

public class Config {
  private static final Log LOG = LogFactory.getLog(Config.class);

  private static final int MAX_CUSTOM_PARAMETERS_COUNT = 10;
  private static final int MAX_CUSTOM_PARAMETER_LENGTH = 256;

  private static final long THRESHOLD_LOWER_BOUND = 0;
  private static final int STACK_THRESHOLD_LOWER_BOUND = 1000;
  private static final String THRESHOLD_KEY = "MIN_TRANSACTION_DURATION_RECORD_THRESHOLD";
  private static final String STACK_SIZE_THRESHOLD_KEY = "CALL_STACK_SIZE_THRESHOLD";
  private static final String THREAD_INSTRUMENTATION_ENABLED_KEY = "THREAD_INSTRUMENTATION_ENABLED";

  private long durationThresholdMillis;
  private int stackSizeThreshold = STACK_THRESHOLD_LOWER_BOUND;
  private String token;
  private String jvm;
  private String subType;
  private String spmMonitorHome;
  private String logPath;
  private String confPath;
  private String extensionsPath;
  private boolean threadInstrumentationEnabled;
  private int maxCustomParameterKeyLength = MAX_CUSTOM_PARAMETER_LENGTH;
  private int maxCustomParameterValueLength = MAX_CUSTOM_PARAMETER_LENGTH;
  private int maxCustomTransactionParametersCount = MAX_CUSTOM_PARAMETERS_COUNT;
  private int maxCustomMethodParametersCount = MAX_CUSTOM_PARAMETERS_COUNT;

  public Config() {
  }

  public long getDurationThresholdMillis() {
    return durationThresholdMillis;
  }

  public int getStackSizeThreshold() {
    return stackSizeThreshold;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getJvm() {
    return jvm;
  }

  public void setJvm(String jvm) {
    this.jvm = jvm;
  }

  public String getSpmMonitorHome() {
    return spmMonitorHome;
  }

  public void setSpmMonitorHome(String spmMonitorHome) {
    this.spmMonitorHome = spmMonitorHome;
  }

  public String getLogPath() {
    return logPath;
  }

  public void setLogPath(String logPath) {
    this.logPath = logPath;
  }

  public String getConfPath() {
    return confPath;
  }

  public void setConfPath(String confPath) {
    this.confPath = confPath;
  }

  public String getExtensionsPath() {
    return extensionsPath;
  }

  public boolean isThreadInstrumentationEnabled() {
    return threadInstrumentationEnabled;
  }

  public void setThreadInstrumentationEnabled(boolean threadInstrumentationEnabled) {
    this.threadInstrumentationEnabled = threadInstrumentationEnabled;
  }

  public void setDurationThresholdMillis(long durationThresholdMillis) {
    this.durationThresholdMillis = durationThresholdMillis;
  }

  public void setStackSizeThreshold(int stackSizeThreshold) {
    this.stackSizeThreshold = stackSizeThreshold;
  }

  public void setExtensionsPath(String extensionsPath) {
    this.extensionsPath = extensionsPath;
  }

  public int getMaxCustomParameterKeyLength() {
    return maxCustomParameterKeyLength;
  }

  public void setMaxCustomParameterKeyLength(int maxCustomParameterKeyLength) {
    this.maxCustomParameterKeyLength = maxCustomParameterKeyLength;
  }

  public int getMaxCustomParameterValueLength() {
    return maxCustomParameterValueLength;
  }

  public void setMaxCustomParameterValueLength(int maxCustomParameterValueLength) {
    this.maxCustomParameterValueLength = maxCustomParameterValueLength;
  }

  public int getMaxCustomTransactionParametersCount() {
    return maxCustomTransactionParametersCount;
  }

  public void setMaxCustomTransactionParametersCount(int maxCustomTransactionParametersCount) {
    this.maxCustomTransactionParametersCount = maxCustomTransactionParametersCount;
  }

  public int getMaxCustomMethodParametersCount() {
    return maxCustomMethodParametersCount;
  }

  public void setMaxCustomMethodParametersCount(int maxCustomMethodParametersCount) {
    this.maxCustomMethodParametersCount = maxCustomMethodParametersCount;
  }

  public static Config getConfig(String args) {
    Config config = new Config();
    config.durationThresholdMillis = 0;
    final Map<String, String> agentArgs = AgentArgsParser.parseColonSeparated(args);
    config.token = agentArgs.get("token");
    config.jvm = agentArgs.get("jvm-name");
    if (config.jvm == null) {
      config.jvm = "default";
    }
    config.spmMonitorHome = FileUtil.path(System.getProperty("spm.home", "/opt/spm"), "spm-monitor");
    if (config.subType != null) {
      config.logPath = FileUtil
          .path(config.spmMonitorHome, "logs", "applications", config.token, config.jvm, config.subType);
    } else {
      config.logPath = FileUtil.path(config.spmMonitorHome, "logs", "applications", config.token, config.jvm);
    }
    config.confPath = FileUtil.path(config.spmMonitorHome, "conf");
    return config;
  }

  public static Config embeddedAgentConfig(String args) {
    final Config config = new Config();
    final Map<String, String> agentArgs = AgentArgsParser.parseColonSeparated(args);
    config.token = agentArgs.get("token");
    config.jvm = agentArgs.get("jvm-name");
    if (config.jvm == null) {
      config.jvm = "default";
    }
    config.subType = agentArgs.get("sub-type");
    config.spmMonitorHome = FileUtil.path(System.getProperty("spm.home", "/opt/spm"), "spm-monitor");
    if (config.subType == null) {
      config.logPath = FileUtil.path(config.spmMonitorHome, "logs", "applications", config.token, config.jvm);
    } else {
      config.logPath = FileUtil
          .path(config.spmMonitorHome, "logs", "applications", config.token, config.jvm, config.subType);
    }
    config.confPath = FileUtil.path(config.spmMonitorHome, "conf");
    config.extensionsPath = FileUtil.path(config.spmMonitorHome, "ext", "tracing");

    final String configFilename = MonitorFileNames.config(null, config.token, config.jvm);
    final Map<String, String> properties = PropertiesReader
        .tryRead(new File(FileUtil.path(config.confPath, configFilename)));

    final String durationThresholdMillis = properties.get(THRESHOLD_KEY);
    final String stackSizeThreshold = properties.get(STACK_SIZE_THRESHOLD_KEY);

    if (durationThresholdMillis != null) {
      try {
        config.durationThresholdMillis = Integer.parseInt(durationThresholdMillis);
      } catch (NumberFormatException e) {
        /* */
      }

      if (config.durationThresholdMillis < THRESHOLD_LOWER_BOUND) {
        LOG.warn(THRESHOLD_KEY + " property has wrong value: " + durationThresholdMillis + ", using default value: "
                     + THRESHOLD_LOWER_BOUND + ".");
        config.durationThresholdMillis = THRESHOLD_LOWER_BOUND;
      }
    } else {
      config.durationThresholdMillis = THRESHOLD_LOWER_BOUND;
      LOG.warn(THRESHOLD_KEY + " property is not set, using default value: " + THRESHOLD_LOWER_BOUND + ".");
    }

    if (stackSizeThreshold != null) {
      try {
        config.stackSizeThreshold = Integer.parseInt(stackSizeThreshold);
      } catch (NumberFormatException e) {
        /* */
      }
    } else {
      config.stackSizeThreshold = STACK_THRESHOLD_LOWER_BOUND;
    }

    final String isThreadInstrumentationEnabled = properties.get(THREAD_INSTRUMENTATION_ENABLED_KEY);
    config.threadInstrumentationEnabled =
        isThreadInstrumentationEnabled == null || Boolean.valueOf(isThreadInstrumentationEnabled);

    if (!config.threadInstrumentationEnabled) {
      LOG.info("Thread instrumentation is disabled.");
    }

    return config;
  }
}
