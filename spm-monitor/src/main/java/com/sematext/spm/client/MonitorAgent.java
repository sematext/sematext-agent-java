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

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.Sender.MonitorType;
import com.sematext.spm.client.command.BasicCommandPollingSetup.CommandPollingRunner;
import com.sematext.spm.client.jmx.JmxServiceContext;
import com.sematext.spm.client.monitor.SourceConfigProperties;
import com.sematext.spm.client.tracing.agent.impl.AgentInitializer;
import com.sematext.spm.client.util.PropertiesReader;

/**
 * Monitoring agent
 */
public final class MonitorAgent {
  private static Log log;
  public static final int MIN_INTERVAL_TIME_BEFORE_SKIP = 1000;
  public static final int SANITARY_START_INTERVAL = 7000;

  public static final String SPM_MONITOR_ENABLED_PROPERTY_NAME = "SPM_MONITOR_ENABLED";

  static {
    log = LogFactory.getLog(MonitorAgent.class);
  }

  private MonitorAgent() {
  }

  public static void main(String[] args) {
    premain(args[0], null);
  }

  static class MonitorTask implements Runnable {
    private MonitorConfig config;
    private long lastEndTime = 0L;

    public MonitorTask(MonitorConfig config) {
      this.config = config;
    }

    @Override
    public void run() {
      try {
        // We skip this check if it's too soon since the last one ended (in case they are backing up).
        if (lastEndTime + MIN_INTERVAL_TIME_BEFORE_SKIP > System.currentTimeMillis()) {
          log.warn("Too soon since last check, skipping");
          return;
        }

        config.collectAndWriteData();

        lastEndTime = System.currentTimeMillis();
      } catch (Throwable thr) {
        try {
          log.error("ERROR", thr);
        } catch (Throwable thr1) {
          System.err.println(MonitorAgent.class + " " + config.getMonitorPropertiesFile().getName() +
                                 " ERROR: error while writing following error: '" + thr.getMessage() +
                                 "' into monitor log. Error produced by writing was: " + thr1.getMessage());
        }
      }
    }
  }

  static class MetricsMetainfoTask implements Runnable {
    private MonitorConfig config;

    public MetricsMetainfoTask(MonitorConfig config) {
      this.config = config;
    }

    @Override
    public void run() {
      try {
        config.processMetainfoUpdates();
      } catch (Throwable thr) {
        try {
          log.error("ERROR", thr);
        } catch (Throwable thr1) {
          System.err.println(MonitorAgent.class + " " + config.getMonitorPropertiesFile().getName() +
                                 " ERROR: error while processing metrics metainfo: '" + thr.getMessage() +
                                 "' . Error was: " + thr1.getMessage());
        }
      }
    }
  }

  public static void premain(String agentArgs, Instrumentation inst) {
    try {
      MonitorUtil.MonitorArgs monitorArgs = MonitorUtil.extractMonitorArgs(agentArgs);
      prepareJmxServiceContext(monitorArgs.getToken(), monitorArgs.getJvmName(), monitorArgs.getSubType());

      MonitorUtil.MONITOR_RUNTIME_SETUP_JAVAAGENT.set(true);

      log.info("Starting with JVM args : " + System.getProperties().toString());

      startMonitoring(agentArgs, inst);
    } catch (Throwable thr) {
      String msg = "Error while starting SPM monitor. SPM monitor will shut down.";
      log.error(msg, thr);
      System.err.println(msg + " " + thr.getMessage());
      thr.printStackTrace();
    }
  }

  private static MonitorConfig getMonitorConfig(String monitorType, MonitorUtil.MonitorArgs monitorArgs,
                                                Instrumentation inst, DataFormat dataFormat, Integer processOrdinal)
      throws ConfigurationFailedException {
    final MonitorConfig config;

    try {
      MonitorUtil.checkRuntimeSetup(monitorArgs.getToken(), monitorArgs.getJvmName(), monitorArgs.getSubType());

      // create any subdir not present and adjust permissions
      MonitorUtil
          .createMonitorLogDirAllSubpaths(monitorType, monitorArgs.getToken(), monitorArgs.getJvmName(), monitorArgs
              .getSubType());

      File monitorProperties = MonitorUtil
          .fetchSpmMonitorPropertiesFileObject(monitorArgs.getToken(), monitorArgs.getJvmName(), monitorArgs
              .getSubType());

      final MonitorConfigLoader configLoader = new MonitorConfigLoader(monitorType, monitorArgs.getToken(), monitorArgs
          .getJvmName(), monitorArgs.getSubType());
      config = configLoader.loadConfig(monitorProperties, dataFormat, processOrdinal);

      if (config == null) {
        return null;
      }

      try {
        if (MonitorUtil.MONITOR_RUNTIME_SETUP_JAVAAGENT.get()) {
          setupInstrumentation(inst, config);
        }
      } catch (Throwable thr) {
        log.error("Error while setting up instrumentation", thr);
        thr.printStackTrace();
        // do the cleanup of collectors if error happens before we start monitoring
        if (config != null) {
          for (StatsCollector<?> collector : config.getExistingCollectors()) {
            collector.cleanup();
          }
        }

        throw new ConfigurationFailedException("Error while registering logs and monitor", thr);
      }
    } catch (ConfigurationFailedException cfe) {
      log.error("Configuration error while starting SPM monitor. Please fix the problem and restart your service");
      cfe.printStackTrace();

      // in case of error with in-process setups, don't propagate exception to avoid stopping the monitored service
      if (MonitorUtil.MONITOR_RUNTIME_SETUP_JAVAAGENT.get()) {
        log.error("Configuration error while starting SPM monitor. Please fix the problem and restart your service", cfe);
        return null;
      } else {
        // no problem if we stop standalone monitor process
        throw cfe;
      }
    }
    return config;
  }

  public static void startMonitoring(String agentArgs, Instrumentation inst) throws ConfigurationFailedException {
    MonitorUtil.MonitorArgs monitorArgs = MonitorUtil.extractMonitorArgs(agentArgs);
    Integer processOrdinal = MonitorUtil
        .obtainMonitorLock(monitorArgs.getToken(), monitorArgs.getJvmName(), monitorArgs.getSubType());
    File propsFile = MonitorUtil
        .fetchSpmMonitorPropertiesFileObject(monitorArgs.getToken(), monitorArgs.getJvmName(), monitorArgs
            .getSubType());

    final Map<String, String> props = PropertiesReader.tryRead(propsFile);

    log.info("Loaded monitor config properties for " + propsFile.getName() + " : " + props);

    if ("false".equalsIgnoreCase(props.get(SPM_MONITOR_ENABLED_PROPERTY_NAME))) {
      System.out.println(
          "For token " + monitorArgs.getToken() + " found " + SPM_MONITOR_ENABLED_PROPERTY_NAME + "=" + props
              .get(SPM_MONITOR_ENABLED_PROPERTY_NAME) + ", disabling the monitor");
      return;
    }

    Sender.initialize(monitorArgs.getToken(), monitorArgs.getJvmName(), monitorArgs
        .getSubType(), MonitorType.APPLICATION);

    try {
      boolean tracingEnabled = "true".equalsIgnoreCase(props.get(SourceConfigProperties.SPM_MONITOR_TRACING_ENABLED));
      if (tracingEnabled) {
        log.info("Tracing enabled for " + propsFile.getName());
      } else {
        log.info("Tracing disabled for " + propsFile.getName());
      }

      // can't initialize tracing for any kind of standalone monitor
      if (inst == null) {
        log.info("Tracing permanently disabled for standalone monitor for " + propsFile.getName());
      } else {
        AgentInitializer.init(agentArgs, inst, tracingEnabled, false);
      }
    } catch (Exception e) {
      throw new ConfigurationFailedException("Can't initialize tracing agent for " + propsFile.getName(), e);
    }

    final MonitorConfig metricsConfig = getMonitorConfig(null, monitorArgs, inst, DataFormat.PLAIN_TEXT, processOrdinal);
    if (metricsConfig == null) {
      return;
    }

    startMonitorThread(metricsConfig);
    startMetricsMetainfoSenderThread(metricsConfig);

    //we init logs only for applications, we want to see all messages 1 place
    LogFactory.init(metricsConfig.getLogBasedir(), metricsConfig.getLogMaxFileSize(), metricsConfig
        .getLogMaxBackups(), metricsConfig.getLogLevel(), DataFormat.PLAIN_TEXT, processOrdinal);

    log.info("Monitor threads started for " + propsFile.getName());

    if (!"true".equalsIgnoreCase(props.get(SourceConfigProperties.SPM_MONITOR_PROFILER_DISABLED))) {
      try {
        CommandPollingRunner commandPollingRunner = new CommandPollingRunner(agentArgs, metricsConfig);
        commandPollingRunner.setupAndRun(agentArgs);
        log.info("Command polling successfully started for " + propsFile.getName());
      } catch (Exception e) {
        log.error("Can't start command polling for " + propsFile.getName(), e);
      }
    } else {
      log.info("Command polling disabled for " + propsFile.getName());
    }
  }

  private static void startMonitorThread(final MonitorConfig config) {
    Thread monitorThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          ThreadFactory threadFactory = new PriorityThreadFactory(Executors.defaultThreadFactory(), String
              .format("metrics-%s", MonitorUtil.getMonitorId(config.getMonitorPropertiesFile())),
                                                                  Thread.MIN_PRIORITY);
          ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
          MonitorTask task = new MonitorTask(config);
          executorService.scheduleAtFixedRate(task, SANITARY_START_INTERVAL, config
              .getMonitorCollectInterval(), TimeUnit.MILLISECONDS);
          executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (Throwable thr) {
          log.error("Error while starting up monitor thread", thr);
          System.out.println("Error while starting up monitor thread");
          thr.printStackTrace();
        } finally {
          if (config != null) {
            for (StatsCollector<?> collector : config.getExistingCollectors()) {
              collector.cleanup();
            }
          }
        }
      }
    }, String.format("metrics-%s", MonitorUtil.getMonitorId(config.getMonitorPropertiesFile())));

    // in-process setup shouldn't interfere with regular start/stop procedure of host process, so we have to
    // mark monitor threads as daemon threads; on the other hand, that presents a problem for standalone monitor,
    // since JVM automatically exits if only daemon threads are left running. So, we need different setting
    if (MonitorUtil.MONITOR_RUNTIME_SETUP_JAVAAGENT.get()) {
      monitorThread.setDaemon(true);
    } else {
      monitorThread.setDaemon(false);
    }

    monitorThread.start();
  }

  private static void startMetricsMetainfoSenderThread(final MonitorConfig config) {
    Thread metainfoThread = new Thread(new Runnable() {
      @Override
      public void run() {
        ScheduledExecutorService executorService = null;
        try {
          ThreadFactory threadFactory = new PriorityThreadFactory(Executors.defaultThreadFactory(), String
              .format("metainfo-%s", MonitorUtil.getMonitorId(config.getMonitorPropertiesFile())),
                                                                  Thread.MIN_PRIORITY);
          executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
          MetricsMetainfoTask task = new MetricsMetainfoTask(config);
          executorService.scheduleAtFixedRate(task, SANITARY_START_INTERVAL, 60 * 1000, TimeUnit.MILLISECONDS);
          executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
          e.printStackTrace();
        } finally {
          if (executorService != null && !executorService.isTerminated()) {
            executorService.shutdownNow();
          }
        }
      }
    }, String.format("metainfo-%s", MonitorUtil.getMonitorId(config.getMonitorPropertiesFile())));

    // in-process setup shouldn't interfere with regular start/stop procedure of host process, so we have to
    // mark monitor threads as daemon threads; on the other hand, that presents a problem for standalone monitor,
    // since JVM automatically exits if only daemon threads are left running. So, we need different setting
    if (MonitorUtil.MONITOR_RUNTIME_SETUP_JAVAAGENT.get()) {
      metainfoThread.setDaemon(true);
    } else {
      metainfoThread.setDaemon(false);
    }

    metainfoThread.start();
  }

  private static JmxServiceContext prepareJmxServiceContext(String token, String jvmName, String subtype) {
    return JmxServiceContext.initLocal(token, jvmName, subtype);
  }

  private static void setupInstrumentation(Instrumentation inst, MonitorConfig config) {
    if (config != null) {
      for (StatsCollector<?> metricsStatsCollector : config.getExistingCollectors()) {
        metricsStatsCollector.init(inst);
      }
    }
  }
}
