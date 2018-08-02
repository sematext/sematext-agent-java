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
package com.sematext.spm.client.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.InputStream;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.config.AcceptConfig;
import com.sematext.spm.client.config.CollectorFileConfig;
import com.sematext.spm.client.config.CollectorFileConfig.ConfigType;
import com.sematext.spm.client.config.FunctionInvokerConfig;
import com.sematext.spm.client.config.IgnoreConfig;
import com.sematext.spm.client.config.MetricConfig;
import com.sematext.spm.client.config.ObservationDefinitionConfig;
import com.sematext.spm.client.config.TagConfig;

public final class YamlConfigLoader {
  private YamlConfigLoader() {
  }

  public static CollectorFileConfig load(File configFile) throws ConfigurationFailedException {
    final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    try {
      CollectorFileConfig config = mapper.readValue(configFile, CollectorFileConfig.class);
      fillMetricNamespace(config);

      checkConfig(config, configFile.getAbsolutePath());

      return config;
    } catch (Exception e) {
      throw new ConfigurationFailedException(
          "Error while reading configuration file " + configFile + ", message: " + e.getMessage(), e);
    }
  }

  public static CollectorFileConfig load(InputStream is, String configFileName) throws ConfigurationFailedException {
    final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    try {
      CollectorFileConfig config = mapper.readValue(is, CollectorFileConfig.class);
      fillMetricNamespace(config);

      checkConfig(config, configFileName);

      return config;
    } catch (Exception e) {
      throw new ConfigurationFailedException("Error while reading configuration file, message: " + e.getMessage(), e);
    }
  }

  public static CollectorFileConfig load(String configString, String configFileName)
      throws ConfigurationFailedException {
    final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    try {
      CollectorFileConfig config = mapper.readValue(configString, CollectorFileConfig.class);
      fillMetricNamespace(config);

      checkConfig(config, configFileName);

      return config;
    } catch (Exception e) {
      throw new ConfigurationFailedException("Error while reading configuration file, message: " + e.getMessage(), e);
    }
  }

  public static void main(String[] args) throws ConfigurationFailedException {
    for (File f : (new File("/opt/spm/spm-monitor/collectors/es")).listFiles()) {
      if (f.getAbsolutePath().endsWith(".yml")) {
        CollectorFileConfig c = load(f);
        System.out.println(c);
      }
    }
  }

  public static void checkConfig(CollectorFileConfig config, String configFileName)
      throws ConfigurationFailedException {
    if (config.getType() == null) {
      throw new ConfigurationFailedException(
          "For file " + configFileName + " key 'type' is missing (should be one of : jmx, json, db)");
    }

    if (config.getType() == ConfigType.DB || config.getType() == ConfigType.JSON) {
      if (config.getData() == null) {
        throw new ConfigurationFailedException(
            "File " + configFileName + " with config of type " + config.getType() + " requires 'data' definition");
      }
    } else if (config.getType() == ConfigType.JMX && config.getData() != null) {
      throw new ConfigurationFailedException(
          "File " + configFileName + " of type " + config.getType() + " shouldn't have 'data' definition");
    }

    if (config.getObservation().size() == 0) {
      throw new ConfigurationFailedException("File " + configFileName + " has no observation definitions");
    }

    for (ObservationDefinitionConfig obs : config.getObservation()) {
      if (obs.getName() == null) {
        throw new ConfigurationFailedException(
            "In file " + configFileName + " some observation has empty or null 'name' key");
      }
      if (obs.getMetricNamespace() == null) {
        throw new ConfigurationFailedException(
            "In file " + configFileName + " some observation has empty or null 'metricNamespace' key");
      }

      if (obs.getMetric().isEmpty() && obs.getFunc().isEmpty()) {
        throw new ConfigurationFailedException("In file " + configFileName + ", observation " + obs.getName() +
                                                   " has no 'metric' nor 'func' definitions");
      }

      for (MetricConfig metric : obs.getMetric()) {
        if (metric.getType() == null) {
          throw new ConfigurationFailedException("In file " + configFileName + ", observation " + obs.getName() +
                                                     ", metric '" + metric.getName() + "' has no 'type' key");
        }
        if (metric.getSource() == null) {
          throw new ConfigurationFailedException("In file " + configFileName + ", observation " + obs.getName() +
                                                     ", metric '" + metric.getName() + "' has no 'source' key");
        }
      }

      for (FunctionInvokerConfig func : obs.getFunc()) {
        if (func.getName() == null || "".equals(func.getName().trim())) {
          throw new ConfigurationFailedException("In file " + configFileName + ", observation " + obs.getName() +
                                                     ", some func have no 'name' key");
        }
        if (func.getType() == null || "".equals(func.getType().trim())) {
          throw new ConfigurationFailedException("In file " + configFileName + ", observation " + obs.getName() +
                                                     ", some func have no 'type' key");
        }
      }

      for (TagConfig tag : obs.getTag()) {
        if (tag.getName() == null || "".equals(tag.getName().trim())) {
          throw new ConfigurationFailedException("In file " + configFileName + ", observation " + obs.getName() +
                                                     ", some tags have no 'name' key");
        }
        if (tag.getValue() == null || "".equals(tag.getValue().trim())) {
          throw new ConfigurationFailedException("In file " + configFileName + ", observation " + obs.getName() +
                                                     ", some tags have no 'value' key");
        }
      }

      for (AcceptConfig accept : obs.getAccept()) {
        if (accept.getName() == null || "".equals(accept.getName().trim())) {
          throw new ConfigurationFailedException("In file " + configFileName + ", observation " + obs.getName() +
                                                     ", some accept have no 'name' key");
        }
        if (accept.getValue() == null || "".equals(accept.getValue().trim())) {
          throw new ConfigurationFailedException("In file " + configFileName + ", observation " + obs.getName() +
                                                     ", some accept have no 'value' key");
        }
      }

      for (IgnoreConfig ignore : obs.getIgnore()) {
        if (ignore.getName() == null || "".equals(ignore.getName().trim())) {
          throw new ConfigurationFailedException("In file " + configFileName + ", observation " + obs.getName() +
                                                     ", some ignore have no 'name' key");
        }
        if (ignore.getValue() == null || "".equals(ignore.getValue().trim())) {
          throw new ConfigurationFailedException("In file " + configFileName + ", observation " + obs.getName() +
                                                     ", some ignore have no 'value' key");
        }
      }
    }
  }

  private static void fillMetricNamespace(CollectorFileConfig config) {
    for (ObservationDefinitionConfig obs : config.getObservation()) {
      for (MetricConfig m : obs.getMetric()) {
        m.setNamespace(obs.getMetricNamespace());
      }
    }
  }
}
