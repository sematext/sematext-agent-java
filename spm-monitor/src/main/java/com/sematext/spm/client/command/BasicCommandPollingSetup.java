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
package com.sematext.spm.client.command;

import java.io.File;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.PriorityThreadFactory;
import com.sematext.spm.client.jmx.JmxServiceContext;
import com.sematext.spm.client.monitor.thrift.TCommandType;
import com.sematext.spm.client.sender.SenderUtil;
import com.sematext.spm.client.sender.config.ChangeWatcher;
import com.sematext.spm.client.util.AgentArgsParser;
import com.sematext.spm.client.util.FileUtil;
import com.sematext.spm.client.util.PropertiesReader;
import com.sematext.spm.client.util.StringUtils;

public final class BasicCommandPollingSetup {
  private static final Log LOG = LogFactory.getLog(BasicCommandPollingSetup.class);

  // kind of duplicate from spm-sender, but spm-sender's properties loading mixed across sender
  // and depends on various libraries which could absent in spm-monitor
  static class SenderConfiguration {

    String proxyHost;
    int proxyPort;
    String proxyUsername;
    String proxyPassword;
    boolean proxySecure;
    String hostnameAlias;
    String hostname;
    String receiverUrl;

    static SenderConfiguration load() {
      final File agentPropsFile = new File(FileUtil.path(System
                                                                 .getProperty("spm.home", "/opt/spm"), "properties", "agent.properties"));
      Map<String, String> properties = PropertiesReader.tryRead(agentPropsFile);
      SenderConfiguration config = new SenderConfiguration();
      config.proxyHost = properties.get("proxy_host");
      String proxyPort = properties.get("proxy_port");
      if (proxyPort != null && !proxyPort.trim().isEmpty()) {
        config.proxyPort = Integer.parseInt(properties.get("proxy_port"));
      }
      config.proxyUsername = properties.get("proxy_user_name");
      config.proxyPassword = properties.get("proxy_password");
      config.proxySecure = Boolean.parseBoolean(properties.get("proxy_secure"));
      config.hostnameAlias = properties.get("hostname_alias");
      config.receiverUrl = properties.get("server_base_url");
      if (config.receiverUrl == null) {
        throw new IllegalStateException("Missing 'server_base_url' property at " + agentPropsFile);
      }
      if (config.receiverUrl.endsWith("/")) {
        config.receiverUrl = config.receiverUrl.substring(0, config.receiverUrl.length() - 1);
      }
      if (!StringUtils.isEmpty(config.hostnameAlias)) {
        config.hostname = config.hostnameAlias;
      } else {
        try {
          config.hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
          throw new IllegalStateException("Can't resolve hostname: ", e);
        }
      }
      return config;
    }
  }

  public static class CommandPollingRunner implements Runnable {
    private static final Log LOG = LogFactory.getLog(CommandPollingRunner.class);

    private final String agentArgs;
    private ChangeWatcher.Watch watch;
    private CommandPolling polling;
    private MonitorConfig monitorConfig;

    public CommandPollingRunner(String agentArgs, MonitorConfig monitorConfig) {
      this.agentArgs = agentArgs;
      this.monitorConfig = monitorConfig;
    }

    private static String formatProcessName(Map<String, String> commandLineArgs) {
      String processName = commandLineArgs.get(AgentArgsParser.JVM_NAME_PARAM);
      String configSubtype = commandLineArgs.get(AgentArgsParser.SUB_TYPE_PARAM);
      if (!StringUtils.isEmpty(configSubtype)) {
        processName += "-" + configSubtype;
      }
      return processName;
    }

    public void setupAndRun(String agentArgs) {
      setupInternal(agentArgs, monitorConfig);

      watch = ChangeWatcher
          .files(Arrays.asList(SenderUtil.DATA_SENDER_PROPERTIES_FILE, SenderUtil.DOCKER_SETUP_FILE), null);

      final CommandPollingRunner thisRef = this;

      // setup checker thread
      Thread refreshThread = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            ThreadFactory threadFactory = new PriorityThreadFactory(Executors.defaultThreadFactory(),
                                                                    "command-polling-refresher-" + MonitorUtil
                                                                        .getMonitorId(monitorConfig
                                                                                          .getMonitorPropertiesFile()),
                                                                    Thread.MIN_PRIORITY);
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
            executorService.scheduleAtFixedRate(thisRef, 0, 30000, TimeUnit.MILLISECONDS);
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
          } catch (Throwable thr) {
            LOG.error("Error while setting up CommandPollingRunner", thr);
            thr.printStackTrace();
          }
        }
      }, "command-polling-refresher-" + MonitorUtil.getMonitorId(monitorConfig.getMonitorPropertiesFile()));

      // in-process setup shouldn't interfere with regular start/stop procedure of host process, so we have to
      // mark monitor threads as daemon threads; on the other hand, that presents a problem for standalone monitor,
      // since JVM automatically exits if only daemon threads are left running. So, we need different setting
      if (MonitorUtil.MONITOR_RUNTIME_SETUP_JAVAAGENT.get()) {
        refreshThread.setDaemon(true);
      } else {
        refreshThread.setDaemon(false);
      }

      refreshThread.start();
    }

    private void setupInternal(String agentArgs, MonitorConfig monitorConfig) {
      Map<String, String> args = AgentArgsParser.parseCommandLineArguments(agentArgs);
      String token = args.get("token");
      if (token == null) {
        throw new IllegalStateException("Missing token argument in agentArgs: " + agentArgs);
      }

      final JmxServiceContext ctx = JmxServiceContext.getContext(args.get(AgentArgsParser.TOKEN_PARAM),
                                                                 args.get(AgentArgsParser.JVM_NAME_PARAM), args
                                                                     .get(AgentArgsParser.SUB_TYPE_PARAM));

      final SenderConfiguration senderConfig = SenderConfiguration.load();

      final String pollEndpoint = senderConfig.receiverUrl + "/command/poll";
      final String responseEndpoint = senderConfig.receiverUrl + "/command/response";

      final String agentId = UUID.randomUUID().toString();

      LOG.info(
          "Starting command polling for agent id: " + agentId + ", token: " + token + ", host: " + senderConfig.hostname
              + ".");

      polling = CommandPolling.builder()
          .id(agentId)
          .monitorConfig(monitorConfig)
          .host(senderConfig.hostname)
          .token(token)
          .processName(formatProcessName(args))
          .pollingEndpoint(pollEndpoint)
          .responseEndpoint(responseEndpoint)
          .pollingInterval(30, TimeUnit.SECONDS)
          .retryInterval(5, TimeUnit.SECONDS)
          .addHandler(TCommandType.PING, new PingCommandHandler())
          .addHandler(TCommandType.PROFILE, new ProfileCommandHandler(ctx))
          .addHandler(TCommandType.GET_INSTRUMENTED_METHODS, new GetInstrumentedMethodsHandler())
          .addHandler(TCommandType.ENABLE_TRACING, new EnableTracingHandler())
          .addHandler(TCommandType.DISABLE_TRACING, new DisableTracingHandler())
          .addHandler(TCommandType.IS_TRACING_ENABLED, new IsTracingEnabledHandler())
          .addHandler(TCommandType.UPDATE_INSTRUMENTATION_SETTINGS, new UpdateInstrumentationSettingsHandler())
          .proxy(senderConfig.proxyHost, senderConfig.proxyPort, senderConfig.proxyUsername, senderConfig.proxyPassword, senderConfig.proxySecure)
          .build();

      polling.start();
    }

    @Override
    public void run() {
      if (polling != null && watch != null && watch.isChanged()) {
        LOG.info("Properties for CommandPolling CHANGED, stopping...");
        polling.stop();
        LOG.info("CommandPolling stopped");
        setupInternal(agentArgs, monitorConfig);
      }
    }
  }
}
