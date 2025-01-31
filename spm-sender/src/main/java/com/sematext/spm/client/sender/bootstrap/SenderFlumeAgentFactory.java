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
package com.sematext.spm.client.sender.bootstrap;

import com.google.common.collect.Maps;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.sender.config.Configuration;
import com.sematext.spm.client.sender.config.SenderConfig;
import com.sematext.spm.client.sender.flume.SenderEmbeddedAgent;
import com.sematext.spm.client.sender.flume.SinkConstants;
import com.sematext.spm.client.sender.flume.influx.InfluxClient;
import com.sematext.spm.client.sender.flume.influx.InfluxSink;
import com.sematext.spm.client.util.FileUtil;

public final class SenderFlumeAgentFactory {
  private static final Log LOG = LogFactory.getLog(SenderFlumeAgentFactory.class);

  private SenderFlumeAgentFactory() {
  }

  public static SenderEmbeddedAgent createAgent(SenderConfig senderConfig, Configuration globalConfig,
                                                File monitorPropertiesFile, Properties monitorProperties,
                                                boolean metainfoSender, boolean tagsSender) {
    SenderEmbeddedAgent flumeSenderAgent = new SenderEmbeddedAgent();

    final Map<String, String> properties = Maps.newHashMap();
    populateCommonProperties(properties, senderConfig, monitorPropertiesFile, monitorProperties, metainfoSender, tagsSender);

    if (!metainfoSender && !tagsSender) {
      properties.put(SinkConstants.URL_PARAM_CONTENT_TYPE, senderConfig.getContentType());
    }

    String owner = MonitorUtil.findProcessOwner();
    String ownerAlternative = MonitorUtil.findProcessOwnerAlternative();

    LOG.info("Resolved owner: " + owner + ", ownerAlernative: " + ownerAlternative);

    if (StringUtils.isNotBlank(senderConfig.getFlumeSubdir())) {
      if (owner != null) {
        properties.put("checkpointDir", FileUtil
            .path(globalConfig.getBaseMonitorDir(), "flume", owner, "checkpointDir", senderConfig.getFlumeSubdir()));
        properties.put("dataDirs", FileUtil
            .path(globalConfig.getBaseMonitorDir(), "flume", owner, "dataDirs", senderConfig.getFlumeSubdir()));
      } else {
        properties.put("checkpointDir", FileUtil
            .path(globalConfig.getBaseMonitorDir(), "flume", "checkpointDir", senderConfig.getFlumeSubdir()));
        properties.put("dataDirs", FileUtil
            .path(globalConfig.getBaseMonitorDir(), "flume", "dataDirs", senderConfig.getFlumeSubdir()));
      }

      if (ownerAlternative != null) {
        properties.put("checkpointDirAlter", FileUtil
            .path(globalConfig.getBaseMonitorDir(), "flume", ownerAlternative, "checkpointDir", senderConfig
                .getFlumeSubdir()));
        properties.put("dataDirsAlter", FileUtil
            .path(globalConfig.getBaseMonitorDir(), "flume", ownerAlternative, "dataDirs", senderConfig
                .getFlumeSubdir()));
      }
    } else {
      properties.put("checkpointDir", null);
      properties.put("dataDirs", null);
      properties.put("checkpointDirAlter", null);
      properties.put("dataDirsAlter", null);
    }

    flumeSenderAgent.configure(properties);

    LOG.info("Created SenderEmbeddedAgent for " + senderConfig.getContentType().toLowerCase() + " stats for tokens: "
                 + StringUtils.join(senderConfig.getTokens(), ","));

    return flumeSenderAgent;
  }

  private static void populateCommonProperties(Map<String, String> properties, SenderConfig senderConfig,
                                               File monitorPropertiesFile, Properties monitorProperties,
                                               boolean metainfoSender, boolean tagsSender) {
    // channel properties
    if (metainfoSender) {
      // small capacity for metainfo
      properties.put("memoryCapacity", "1000");
      properties.put("overflowCapacity", "25000");
      properties.put("overflowTimeout", "1");
      properties.put("avgEventSize", "100");
      properties.put("transactionCapacity", "1000");

      // file properties
      properties.put("maxFileSize", "200000");
    } else if (tagsSender) {
      // even smaller capacity for tags
      properties.put("memoryCapacity", "100");
      properties.put("overflowCapacity", "2500");
      properties.put("overflowTimeout", "1");
      properties.put("avgEventSize", "100");
      properties.put("transactionCapacity", "100");

      // file properties
      properties.put("maxFileSize", "20000");
    } else {
      properties.put("memoryCapacity", "20000");
      properties.put("overflowCapacity", "500000");
      properties.put("overflowTimeout", "1");
      properties.put("avgEventSize", "100");

      // file properties
      properties.put("maxFileSize", "100000000");
    }

    String receiverUrl = senderConfig.getReceiverUrl().trim();
    String metricsPath = senderConfig.getMetricsEndpoint().trim();
    String tagsPath = senderConfig.getTagAliasesEndpoint().trim();
    String metainfoPath = senderConfig.getMetainfoEndpoint().trim();
    
    String endpointPath = metricsPath;
    if (metainfoSender) {
      endpointPath = metainfoPath;
    } else if (tagsSender) {
      endpointPath = tagsPath;
    }

    properties.put("sinkClass", senderConfig.getSinkClass());
    properties.put("http.post.sink.url", senderConfig.getReceiverUrl() + "/thrift");
    properties.put("http.post.sink.batch.size", "100");

    populateInfluxSinkProperties(properties, senderConfig, receiverUrl, endpointPath);
  }

  private static void populateInfluxSinkProperties(Map<String, String> properties, SenderConfig senderConfig,
                                                   String receiverUrl, String receiverPath) {
    properties.put(InfluxSink.INFLUX_SERVER, receiverUrl);
    properties.put(InfluxSink.INFLUX_ENDPOINT_PATH, receiverPath);

    properties
        .put(InfluxClient.URL_PARAM_VERSION, SenderFlumeAgentFactory.class.getPackage().getSpecificationVersion());

    if (senderConfig.getProxyHost() != null) {
      properties.put(InfluxSink.PROXY_HOST, senderConfig.getProxyHost());
      properties.put(InfluxSink.PROXY_PORT,
                     senderConfig.getProxyPort() != null ? String.valueOf(senderConfig.getProxyPort()) : null);
      properties.put(InfluxSink.PROXY_USERNAME, senderConfig.getProxyUser());
      properties.put(InfluxSink.PROXY_PASSWORD, senderConfig.getProxyPassword());
      properties.put(InfluxSink.PROXY_SECURE, String.valueOf(senderConfig.isProxySecure()));
    }
  }
}
