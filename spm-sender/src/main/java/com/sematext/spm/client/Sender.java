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

import org.apache.flume.agent.embedded.EmbeddedSource;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.sematext.spm.client.sender.SenderUtil;
import com.sematext.spm.client.sender.bootstrap.SenderFlumeAgentFactory;
import com.sematext.spm.client.sender.config.BaseSenderConfigFactory;
import com.sematext.spm.client.sender.config.ChangeWatcher;
import com.sematext.spm.client.sender.config.Configuration;
import com.sematext.spm.client.sender.config.InstallationProperties;
import com.sematext.spm.client.sender.config.RuntimeConfigSenderConfigFactory;
import com.sematext.spm.client.sender.config.SenderConfig;
import com.sematext.spm.client.sender.config.SenderConfigRef;
import com.sematext.spm.client.sender.config.SenderConfigSource;
import com.sematext.spm.client.sender.flume.SenderEmbeddedAgent;
import com.sematext.spm.client.sender.log.Log4jSPMClientConfigurator;

// token (and monitorType) must be initialized during Agent startup -> that is what initialize()
// method would do
// later we create specific SenderType Sources at the moment when they are first requested
// for all monitors, we create File Watcher on SPM-HOME/properties files
public final class Sender {
  static {
    Log4jSPMClientConfigurator.configure();
  }

  public enum SenderType {
    STATS,
    METRICS_METAINFO,
    TAG_ALIAS,
    TRACING,
    SNAPSHOT
  }

  public enum MonitorType {
    APPLICATION,
  }

  private static final Log LOG = LogFactory.getLog(Sender.class);
  private static final Map<SenderType, SenderEmbeddedAgent> AGENTS = new UnifiedMap<SenderType, SenderEmbeddedAgent>();
  private static final Map<SenderType, EmbeddedSource> SOURCES = new UnifiedMap<SenderType, EmbeddedSource>();
  private static final Map<SenderType, SenderConfigRef> CONFIG_REFS = new UnifiedMap<SenderType, SenderConfigRef>();

  private static String TOKEN;
  private static String JVMNAME;
  private static String CONFIG_SUBTYPE;

  private Sender() {
  }

  public static void initialize(String token, String jvmName, String configSubtype, MonitorType monitorType) {
    LOG.info("Initializing MonitorType = " + monitorType + ", token = " + token + ", jvmName = " + jvmName +
                 ", configSubtype = " + configSubtype);

    if (token == null) {
      throw new IllegalArgumentException("Monitor type: " + monitorType + " can't be defined using null token!");
    }

    if (token != null) {
      TOKEN = token;
      JVMNAME = jvmName;
      CONFIG_SUBTYPE = configSubtype;
    }
  }

  private synchronized static void initialize(SenderType senderType) {
    SenderEmbeddedAgent agent = AGENTS.get(senderType);

    if (agent == null) {
      try {
        final Configuration globalConfig = Configuration.defaultConfig();

        final InstallationProperties agentPropsFile =
            InstallationProperties.loadSpmSenderInstallationProperties(globalConfig)
                .fallbackTo(InstallationProperties.fromResource("/agent.default.properties"));

        final InstallationProperties tracingProperties =
            InstallationProperties.fromFile(new File(globalConfig.getTracingPropertiesFile()))
                .fallbackTo(InstallationProperties.fromResource("/tracing.default.properties"))
                .fallbackTo(agentPropsFile);

        InstallationProperties properties =
            senderType == SenderType.STATS || senderType == SenderType.METRICS_METAINFO ||
                senderType == SenderType.TAG_ALIAS ?
                agentPropsFile :
                senderType == SenderType.TRACING ? tracingProperties :
                    agentPropsFile;

        final BaseSenderConfigFactory factory;

        final SenderConfigSource source;
        final Properties monitorProperties = new Properties();

        String monitorType = SenderType.STATS == senderType || SenderType.METRICS_METAINFO == senderType
            || SenderType.TAG_ALIAS == senderType ? null :
            SenderType.TRACING == senderType ? "tracing" :
                SenderType.SNAPSHOT == senderType ? "snapshot" : senderType.toString().toLowerCase();

        List<File> files = new FastList<File>();
        // files.add(new File(MonitorUtil.getMonitorRuntimeFileName(monitorType, token, JVMNAME, CONFIG_SUBTYPE)));
        source = new SenderConfigSource(files, monitorType, TOKEN, JVMNAME, CONFIG_SUBTYPE);
        factory = new RuntimeConfigSenderConfigFactory(globalConfig, properties, source, senderType);

        File propsFile = MonitorUtil.fetchSpmMonitorPropertiesFileObject(TOKEN, JVMNAME, CONFIG_SUBTYPE);
        monitorProperties.load(new FileInputStream(propsFile));

        LOG.info("Factory " + factory + " created for sender type: " + senderType);

        final ChangeWatcher.Watch watch = ChangeWatcher.or(
            ChangeWatcher.or(properties.createWatch(null),
                             ChangeWatcher.files(source.getFiles(), null)),
            ChangeWatcher.files(Arrays.asList(SenderUtil.DOCKER_SETUP_FILE), null));

        SenderConfigRef ref = new SenderConfigRef(TOKEN, factory, watch, true);

        final SenderConfig senderConfig = ref.getConfig();

        agent = SenderFlumeAgentFactory.createAgent(senderConfig, globalConfig, propsFile,
                                                    monitorProperties,
                                                    senderType == SenderType.METRICS_METAINFO,
                                                    senderType == SenderType.TAG_ALIAS);
        agent.start();
        LOG.info("Created and started flume agent for : " + ref.getConfig().getTokens());

        agent.setAppToken(TOKEN);

        AGENTS.put(senderType, agent);
        SOURCES.put(senderType, (EmbeddedSource) agent.createAndStartSource());
        CONFIG_REFS.put(senderType, ref);
      } catch (Exception e) {
        LOG.error("Error while getting sender config for :" + TOKEN + ", type: " + senderType, e);
      }
    }
  }

  public static EmbeddedSource getSource(SenderType senderType) {
    checkConfigsUpdated(senderType);

    EmbeddedSource source = SOURCES.get(senderType);

    if (source == null) {
      initialize(senderType);
      source = SOURCES.get(senderType);
    }

    return source;
  }

  private static void checkConfigsUpdated(SenderType senderType) {
    SenderConfigRef ref = CONFIG_REFS.get(senderType);

    if (ref != null) {
      if (ref.updated()) {
        LOG.info("Config update happened... ");
        final SenderEmbeddedAgent agent = AGENTS.get(senderType);

        if (agent != null) {
          LOG.info("Stopping SenderEmbeddedAgent");
          agent.stop();
          AGENTS.remove(senderType);
          SOURCES.remove(senderType);
          LOG.info("Stopped SenderEmbeddedAgent");
        }
      }
    }
  }
}
