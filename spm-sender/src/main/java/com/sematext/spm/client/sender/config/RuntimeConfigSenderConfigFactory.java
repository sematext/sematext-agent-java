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

import com.google.common.collect.Maps;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.Sender.SenderType;
import com.sematext.spm.client.sender.flume.influx.InfluxSink;
import com.sematext.spm.client.sender.util.HttpBinaryPostSink;
import com.sematext.spm.client.util.StringUtils;

public class RuntimeConfigSenderConfigFactory extends BaseSenderConfigFactory {
  private static final Log LOG = LogFactory.getLog(RuntimeConfigSenderConfigFactory.class);

  private final SenderConfigSource source;
  private final InstallationProperties installationProperties;
  private final SenderType senderType;

  public RuntimeConfigSenderConfigFactory(Configuration config, InstallationProperties installationProperties,
                                          SenderConfigSource source, SenderType senderType) {
    super(config);
    this.source = source;
    this.installationProperties = installationProperties;
    this.senderType = senderType;
  }

  @Override
  protected Map<String, String> getProperties() {
    final Map<String, String> properties = Maps.newHashMap(installationProperties.getProperties());
    try {
      for (final Properties p : source.getProperties()) {
        for (final String key : p.stringPropertyNames()) {
          properties.put(key, p.getProperty(key));
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Can't load properties.", e);
    }

    final String senderSinkClass;

    if (senderType != null) {
      if (senderType == SenderType.TRACING || senderType == SenderType.SNAPSHOT) {
        senderSinkClass = HttpBinaryPostSink.class.getCanonicalName();
      } else if (senderType == SenderType.STATS || senderType == SenderType.METRICS_METAINFO
          || senderType == SenderType.TAGS) {
        // senderSinkClass = CustomElasticSearchSink.class.getCanonicalName();
        senderSinkClass = InfluxSink.class.getCanonicalName();
      } else {
        throw new IllegalArgumentException("Unsupported senderType: " + senderType);
      }
    } else {
      // old logic, probably can be removed after sender is merged with monitor
      if ("binary".equals(properties.get("SPM_MONITOR_LOG_FORMAT"))) {
        senderSinkClass = HttpBinaryPostSink.class.getCanonicalName();
      } else {
        // senderSinkClass = CustomElasticSearchSink.class.getCanonicalName();
        senderSinkClass = InfluxSink.class.getCanonicalName();
      }
    }
    properties.put("spm_sender_sink_class", senderSinkClass);

    LOG.info("Loaded properties: " + properties + ".");
    return properties;
  }

  @Override
  protected String getContentType() {
    return "APP";
  }

  @Override
  protected String getFlumeSubdir() {
    LOG.info("Calling getFlumeSubdir for " + senderType);

    return senderType == SenderType.TRACING ? "" : (StringUtils.isEmpty(this.source.getMonitorType()) ?
        "" : this.source.getMonitorType() + File.separator) + this.source.getToken() + File.separator + this.source
        .getJvmName() +
        File.separator + this.source.getConfSubtype() +
        ((senderType == SenderType.METRICS_METAINFO ? File.separator + "metainfo" : "") +
            (senderType == SenderType.TAGS ? File.separator + "tags" : "")) +
        File.separator + MonitorUtil.MONITOR_PROCESS_ORDINAL;
  }

  @Override
  protected boolean postProcess(SenderConfig.Builder builder) {
    builder.setToken(source.getToken());
    return true;
  }

}
