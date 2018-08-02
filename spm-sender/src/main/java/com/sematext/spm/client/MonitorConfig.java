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

import org.eclipse.collections.impl.list.mutable.FastList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.sematext.spm.client.util.StringUtils;

/**
 * Holds monitor configuration
 */
public class MonitorConfig extends InMemoryConfig {
  private static final Log LOG = LogFactory.getLog(MonitorConfig.class);
  public static final int DEFAULT_LOG_MAX_BACKUPS = 10;

  private static final int DEFAULT_FIRST_RELOAD_INTERVAL = 60 * 1000; // one minute
  private static final int DEFAULT_RELOAD_INTERVAL = 60 * 1000; // one minute
  public static final int DEFAULT_MONITOR_INTERVAL = 10000; //10 sec
  public static final String DEFAULT_LOGGER_CLASS = "com.sematext.spm.client.RollingFileLogger";
  public static final int MIN_ALLOWED_MONITOR_INTERVAL = 10000; //10 sec
  public static final int MAX_ALLOWED_MONITOR_INTERVAL = 60000; //60 sec
  private static final long MAX_LOG_FILE_SIZE_BYTES = 10 * 1024 * 1024;

  private long lastJmxInfoReload = 0L;
  private long configFirstReloadInterval = DEFAULT_FIRST_RELOAD_INTERVAL;
  private long configReloadInterval = DEFAULT_RELOAD_INTERVAL;

  private long monitorCollectInterval;

  public static long GLOBAL_MONITOR_COLLECT_INTERVAL_MS = DEFAULT_MONITOR_INTERVAL;

  private RollingLogWriter statsLogger;

  private boolean firstReloadIntervalPassed = false;

  private String appToken;
  private String jvmName;
  private String subType;

  private List<StatsCollector> collectors;
  private String logBasedir;
  private long logMaxFileSize;
  private int logMaxBackups;
  private String logLevel;

  private Integer processOrdinal;
  private DataFormat format;

  private StatsLogLineBuilder statsLogLineBuilder;

  private boolean hasMetainfo;
  private MetricsMetainfoSender metricsMetainfoSender;
  private String metricCollectorsConfigValue;

  private TagsSender tagsSender;

  public MonitorConfig(String appToken, File monitorPropertiesFile, DataFormat format, Integer processOrdinal)
      throws ConfigurationFailedException {
    super(null);
    this.monitorPropertiesFile = monitorPropertiesFile;
    this.appToken = appToken;
    this.jvmName = MonitorUtil.extractJvmName(monitorPropertiesFile.getAbsolutePath(), appToken);
    this.subType = MonitorUtil.extractConfSubtype(monitorPropertiesFile.getAbsolutePath());
    this.monitorPropertiesFile = monitorPropertiesFile;
    this.processOrdinal = processOrdinal;
    this.format = format;
    loadConfig();
  }

  public List<StatsCollector> getExistingCollectors() {
    return collectors;
  }

  public List<StatsCollector> getFreshCollectors() {
    // every time this method is invoked, we'll check if configuration has to be reloaded

    boolean configReloadNeeded = false;

    long currentTime = System.currentTimeMillis();

    // reload MonitorConfig on first enter, when first reload interval has passed and after every configReloadInterval
    // ms
    if (lastJmxInfoReload == 0) {
      configReloadNeeded = true;
    } else if (firstReloadIntervalPassed) {
      // adding 1000ms since it appears Java's ExecutorService sometimes fires the thread few ms too early
      if ((currentTime - lastJmxInfoReload + 1000) >= configReloadInterval) {
        configReloadNeeded = true;
      }
    } else {
      // adding 1000ms since it appears Java's ExecutorService sometimes fires the thread few ms too early
      if ((currentTime - lastJmxInfoReload + 1000) >= configFirstReloadInterval) {
        configReloadNeeded = true;
        firstReloadIntervalPassed = true;
      }
    }

    LOG.info("GetCollectors() reload needed: " + configReloadNeeded);

    if (configReloadNeeded) {
      List<StatsCollector> oldCollectors = collectors;
      String oldLogBasedDir = logBasedir;
      long oldLogMaxFileSize = logMaxFileSize;
      int oldLogMaxBackups = logMaxBackups;
      String oldLogLevel = logLevel;
      long oldConfigFirstReloadInterval = configFirstReloadInterval;
      long oldConfigReloadInterval = configReloadInterval;

      try {
        reloadConfig();

        // only if reload succeeded
        if (oldCollectors != collectors) {
          oldCollectors.clear();
        }
      } catch (ConfigurationFailedException e) {
        // in case reloading didn't succeed, print the error message and restore old data from the conf
        LOG.error("Configuration reloading failed, path : " + monitorPropertiesFile, e);
        collectors = oldCollectors;
        logBasedir = oldLogBasedDir;
        logMaxFileSize = oldLogMaxFileSize;
        logMaxBackups = oldLogMaxBackups;
        logLevel = oldLogLevel;
        configFirstReloadInterval = oldConfigFirstReloadInterval;
        configReloadInterval = oldConfigReloadInterval;
      }

      lastJmxInfoReload = currentTime;
    }

    return collectors;
  }

  public String getLogBasedir() {
    return logBasedir;
  }

  public long getLogMaxFileSize() {
    return logMaxFileSize;
  }

  public int getLogMaxBackups() {
    return logMaxBackups;
  }

  public String getLogLevel() {
    return logLevel;
  }

  public long getMonitorCollectInterval() {
    return monitorCollectInterval;
  }

  @Override
  protected void readFields(Properties monitorProperties) throws ConfigurationFailedException {
    readMonitorInterval(monitorProperties);
    readReloadingInterval();
    readLoggingInfo(monitorProperties);
    readCollectors(monitorProperties);
    readStatsLogLineBuilder();
    readMetainfoSender(monitorProperties);
    readStatsLogger(getLogBasedir(), getLogMaxFileSize(), getLogMaxBackups(), format, processOrdinal);

    if (tagsSender == null) {
      tagsSender = new TagsSender();
    }
  }

  public void collectAndWriteData() {
    Object data = gatherStatsData();
    if (data != null) {
      long t1 = System.currentTimeMillis();
      statsLogger.write(data);
      LOG.info("Stats writing time: " + (System.currentTimeMillis() - t1));
    }
  }

  private Object gatherStatsData() {
    return statsLogLineBuilder.build(getFreshCollectors());
  }

  public void processMetainfoUpdates() {
    if (metricsMetainfoSender != null) {
      metricsMetainfoSender.findAndSendUpdates();
    }
  }

  public void processTags(List<StatValues> tagsStats) {
    if (tagsSender != null) {
      LOG.info("Sending tags: " + tagsStats);
      tagsSender.sendTags(tagsStats);
    }
  }

  private void readMonitorInterval(Properties monitorProperties) throws ConfigurationFailedException {
    String monitorInterval = StringUtils.removeQuotes(monitorProperties.getProperty("SPM_MONITOR_COLLECT_INTERVAL", ""))
        .trim();

    if (!monitorInterval.equals("")) {
      try {
        int newMonitorInterval = Integer.parseInt(monitorInterval);

        if (newMonitorInterval < MIN_ALLOWED_MONITOR_INTERVAL) {
          newMonitorInterval = MIN_ALLOWED_MONITOR_INTERVAL;
          LOG.info("Monitor interval can't be smaller than " + MIN_ALLOWED_MONITOR_INTERVAL +
                       ", setting to: " + monitorInterval);
        }
        if (newMonitorInterval > MAX_ALLOWED_MONITOR_INTERVAL) {
          newMonitorInterval = MAX_ALLOWED_MONITOR_INTERVAL;
          LOG.info("Monitor interval can't be bigger than " + MAX_ALLOWED_MONITOR_INTERVAL +
                       ", setting to: " + monitorInterval);
        }

        if (this.monitorCollectInterval != newMonitorInterval) {
          LOG.info("Monitor interval set to: " + monitorInterval);
        }

        this.monitorCollectInterval = newMonitorInterval;
      } catch (Throwable thr) {
        LOG.error("Monitor interval can't be parsed : " + monitorInterval, thr);
        throw new ConfigurationFailedException("Parsing SPM_MONITOR_COLLECT_INTERVAL failed", thr);
      }
    } else {
      this.monitorCollectInterval = DEFAULT_MONITOR_INTERVAL;
    }

    GLOBAL_MONITOR_COLLECT_INTERVAL_MS = this.monitorCollectInterval;
  }

  private void readReloadingInterval() {
    if (!firstReloadIntervalPassed) {
      LOG.info("First reloading interval set to: " + DEFAULT_FIRST_RELOAD_INTERVAL);

      int newFirstReloadingInterval = DEFAULT_FIRST_RELOAD_INTERVAL;
      configFirstReloadInterval = newFirstReloadingInterval;
    }

    LOG.info("Reloading interval set to: " + DEFAULT_RELOAD_INTERVAL);
    int newReloadingInterval = DEFAULT_RELOAD_INTERVAL;
    configReloadInterval = newReloadingInterval;
  }

  private void readCollectors(Properties monitorProperties) {
    List<StatsCollector> oldCollectors = collectors;
    collectors = new FastList<StatsCollector>();

    String className = "com.sematext.spm.client.GenericStatsCollectorsFactory";
    hasMetainfo = true;

    // The below code uses workarounds to initialize CollectorsFactory for non-generic agents.
    // Will be removed when these non-generic agents are supported by generic agent.
    String jarName = monitorProperties.getProperty("SPM_MONITOR_JAR", null);
    if (jarName != null) {
      if (jarName.endsWith("haproxy.jar")) {
        hasMetainfo = false;
        className = "com.sematext.spm.client.haproxy.factory.HAProxyStatsFactory";
      } else if (jarName.endsWith("storm.jar")) {
        hasMetainfo = false;
        if ("nimbus".equalsIgnoreCase(subType)) {
          className = "com.sematext.spm.client.storm.StormNimbusStatsCollectorsFactory";
        } else if ("supervisor".equalsIgnoreCase(subType)) {
          className = "com.sematext.spm.client.storm.StormSupervisorStatsCollectorsFactory";
        } else if ("worker".equalsIgnoreCase(subType)) {
          className = "com.sematext.spm.client.storm.StormWorkerStatsCollectorsFactory";
        }
      } else if (jarName.endsWith("redis.jar")) {
        hasMetainfo = false;
        className = "com.sematext.spm.client.redis.RedisStatsCollectorsFactory";
      }
    }

    try {
      StatsCollectorsFactory collectorsFactory = createInstance(className);
      collectors.addAll(collectorsFactory.createCollectors(monitorProperties, oldCollectors, this));
    } catch (Throwable thr) {
      // don't propagate the exception, other collectors factories may be functional, we shouldn't stop them from working
      LOG.error("Error creating collectors from factory: " + className, thr);
    }
  }

  private void readMetainfoSender(Properties monitorProperties) {
    if (!hasMetainfo) {
      LOG.warn("Monitor doesn't support metainfo, will not send any");
      return;
    }

    String configCollectors = MonitorUtil
        .stripQuotes(monitorProperties.getProperty("SPM_MONITOR_COLLECTORS", "").trim()).trim();

    if ("".equals(configCollectors)) {
      metricsMetainfoSender = null;
      metricCollectorsConfigValue = null;
      LOG.warn("No metainfo configured since no collectors are defined, it will not be periodically refreshed and updated.");
      return;
    }

    String oldMetricCollectorsConfigValue = metricCollectorsConfigValue;
    metricCollectorsConfigValue = configCollectors;

    if (metricCollectorsConfigValue.equals(oldMetricCollectorsConfigValue)) {
      // no change, do nothing
    } else {
      List<File> collectorsConfigDirs = new ArrayList<File>();
      for (String c : metricCollectorsConfigValue.split(",")) {
        if (c.trim().equals("")) {
          continue;
        }
        File configDir = new File(MonitorUtil.SPM_MONITOR_COLLECTORS_CONFIG_BASE_DIR, c);
        collectorsConfigDirs.add(configDir);
      }
      metricsMetainfoSender = new MetricsMetainfoSender(appToken, collectorsConfigDirs);
    }
  }

  private void readLoggingInfo(Properties monitorProperties) {
    String basedir = MonitorUtil.getMonitorLogDirPath(null, appToken, jvmName, subType);
    this.logBasedir = basedir == null || "".equals(basedir) ? "." : basedir;
    this.logMaxFileSize = MAX_LOG_FILE_SIZE_BYTES;
    this.logMaxBackups = DEFAULT_LOG_MAX_BACKUPS;
    this.logLevel = StringUtils.removeQuotes(monitorProperties.getProperty("SPM_MONITOR_LOGGING_LEVEL", "standard"))
        .trim();
    LogFactory.setLoggingLevel(logLevel);
  }

  private void readStatsLogger(String basedir, long maxFileSize, int maxBackups,
                               DataFormat format, Integer processOrdinal) throws ConfigurationFailedException {
    if (this.statsLogger == null || this.statsLogger
        .isDifferent(basedir, maxFileSize, maxBackups, format, processOrdinal)) {
      RollingLogWriter logger;
      try {
        logger = createInstance(DEFAULT_LOGGER_CLASS);
        logger.init(basedir, MonitorUtil
            .getMonitorStatsLogFileName(processOrdinal, format), maxFileSize, maxBackups, format, processOrdinal);
      } catch (Exception e) {
        LOG.error("Error reading config: Logger can't be instantiated, className: " + DEFAULT_LOGGER_CLASS, e);
        throw new ConfigurationFailedException(
            "Error reading config: Logger can't be instantiated, className: " + DEFAULT_LOGGER_CLASS, e);
      }

      this.statsLogger = logger;
    }
  }

  private void readStatsLogLineBuilder() {
    if (this.statsLogLineBuilder == null) {
      this.statsLogLineBuilder = new StatsMetricsLogLineSender();
    }
  }

  public String getAppToken() {
    return appToken;
  }

  public String getJvmName() {
    return jvmName;
  }

  public String getSubType() {
    return subType;
  }
}
