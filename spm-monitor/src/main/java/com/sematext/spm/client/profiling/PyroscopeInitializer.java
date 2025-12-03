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
package com.sematext.spm.client.profiling;

import com.google.common.base.Splitter;
import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import io.pyroscope.http.Format;
import io.pyroscope.javaagent.PyroscopeAgent;
import io.pyroscope.javaagent.config.Config;

import java.util.HashMap;
import java.util.Map;

public final class PyroscopeInitializer {
  private static final Log LOG = LogFactory.getLog(PyroscopeInitializer.class);

  private static final String ENV_ENABLED = "PYROSCOPE_ENABLED";
  private static final String ENV_SERVER_ADDRESS = "PYROSCOPE_SERVER_ADDRESS";
  private static final String ENV_APPLICATION_NAME = "PYROSCOPE_APPLICATION_NAME";
  private static final String ENV_LABELS = "PYROSCOPE_LABELS";
  private static final String ENV_PROFILER_ALLOC = "PYROSCOPE_PROFILER_ALLOC";
  private static final String ENV_PROFILER_LOCK = "PYROSCOPE_PROFILER_LOCK";

  private static final String DEFAULT_SERVER_ADDRESS = "http://localhost:4040";
  private static final String DEFAULT_APPLICATION_NAME = "sematext-agent";

  private static boolean profilingEnabled = false;

  private PyroscopeInitializer() {
  }

  public static void initialize(Map<String, String> props) {
    try {
      String enabled = getConfigValue(props, ENV_ENABLED, "false");
      if (!"true".equalsIgnoreCase(enabled)) {
        LOG.info("Pyroscope profiling is disabled. Set " + ENV_ENABLED + "=true to enable");
        return;
      }

      String serverAddress = getConfigValue(props, ENV_SERVER_ADDRESS, DEFAULT_SERVER_ADDRESS);
      String applicationName = getConfigValue(props, ENV_APPLICATION_NAME, DEFAULT_APPLICATION_NAME);
      String labelsString = getConfigValue(props, ENV_LABELS, null);
      String allocThreshold = getConfigValue(props, ENV_PROFILER_ALLOC, null);
      String lockThreshold = getConfigValue(props, ENV_PROFILER_LOCK, null);

      LOG.info("Initializing Pyroscope profiling:");
      LOG.info("  Server Address: " + serverAddress);
      LOG.info("  Application Name: " + applicationName);

      Map<String, String> labels = parseLabels(labelsString);
      if (!labels.isEmpty()) {
        LOG.info("  Static Labels: " + labels);
      }

      Config.Builder configBuilder = new Config.Builder()
          .setApplicationName(applicationName)
          .setFormat(Format.JFR)
          .setServerAddress(serverAddress);

      if (!labels.isEmpty()) {
        configBuilder.setLabels(labels);
      }

      if (allocThreshold != null && !allocThreshold.trim().isEmpty()) {
        configBuilder.setProfilingAlloc(allocThreshold);
        LOG.info("  Allocation profiling enabled with threshold: " + allocThreshold);
      }

      if (lockThreshold != null && !lockThreshold.trim().isEmpty()) {
        configBuilder.setProfilingLock(lockThreshold);
        LOG.info("  Lock profiling enabled with threshold: " + lockThreshold);
      }

      Config config = configBuilder.build();

      PyroscopeAgent.start(config);
      profilingEnabled = true;

      LOG.info("Pyroscope profiling started successfully with JFR format (CPU, allocations, and lock profiling)");

    } catch (Exception e) {
      LOG.error("Failed to initialize Pyroscope profiling. Agent will continue without profiling.", e);
      profilingEnabled = false;
    }
  }

  private static Map<String, String> parseLabels(String labelsString) {
    if (labelsString == null || labelsString.trim().isEmpty()) {
      return new HashMap<String, String>();
    }

    try {
      return Splitter.on(',')
          .trimResults()
          .omitEmptyStrings()
          .withKeyValueSeparator('=')
          .split(labelsString);
    } catch (Exception e) {
      LOG.warn("Failed to parse Pyroscope labels from: " + labelsString, e);
      return new HashMap<String, String>();
    }
  }

  private static String getConfigValue(Map<String, String> props, String key, String defaultValue) {
    String value = System.getenv(key);

    if (value == null) {
      value = System.getProperty(key);
    }

    if (value == null && props != null) {
      value = props.get(key);
    }

    return value != null ? value : defaultValue;
  }

  public static boolean isProfilingEnabled() {
    return profilingEnabled;
  }
}
