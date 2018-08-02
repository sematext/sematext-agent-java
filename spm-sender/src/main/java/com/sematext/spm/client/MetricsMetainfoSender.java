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

import org.apache.commons.io.FileUtils;
import org.apache.flume.ChannelException;
import org.apache.flume.Event;
import org.apache.flume.agent.embedded.EmbeddedSource;
import org.apache.flume.event.SimpleEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sematext.spm.client.Sender.SenderType;
import com.sematext.spm.client.config.CollectorFileConfig;
import com.sematext.spm.client.config.MetricConfig;
import com.sematext.spm.client.config.ObservationDefinitionConfig;
import com.sematext.spm.client.util.PercentileUtils;

public class MetricsMetainfoSender {
  private static final Log LOG = LogFactory.getLog(MetricsMetainfoSender.class);

  private List<File> collectorsConfigDirs;
  //contains metainfo data per collector dir
  private Map<String, CollectorMetaInfo> collectorMetaInfoMap;
  private String appToken;
  private Serializer serializer;

  public MetricsMetainfoSender(String appToken, List<File> collectorsConfigDirs) {
    this.appToken = appToken;
    this.collectorsConfigDirs = collectorsConfigDirs;
    this.collectorMetaInfoMap = new HashMap<String, CollectorMetaInfo>(collectorsConfigDirs.size());
    serializer = Serializer.INFLUX;
  }

  public void findAndSendUpdates() {
    LOG.info("MetricMetainfoSender starting...");
    try {
      EmbeddedSource source = getSource();
      if (source == null) {
        LOG.warn("Metainfo source is still null, can't write metrics metainfo");
        return;
      }
      for (File configDir : collectorsConfigDirs) {
        Map<String, MetricConfig> changedMetrics = processCollectorDir(configDir);
        if (changedMetrics.size() > 0) {
          LOG.info("Detected " + changedMetrics.size() + " changes in " + configDir);
          Map<String, MetricConfig> changedPctlMetrics = calculateChangedPercentileMetrics(changedMetrics);
          if (!changedPctlMetrics.isEmpty()) {
            LOG.info("Detected " + changedPctlMetrics.size() + " percentile changes in " + configDir);
            changedMetrics.putAll(changedPctlMetrics);
          }

          CollectorMetaInfo collectorMetaInfo = collectorMetaInfoMap.get(configDir.getAbsolutePath());
          for (MetricConfig metricMetaInfo : changedMetrics.values()) {
            Event newEvent = new SimpleEvent();
            String metricMetaInfoStr = serializer.serializeMetainfo(appToken, metricMetaInfo);
            newEvent.setBody(metricMetaInfoStr.getBytes());
            source.put(newEvent);
            //update cache
            collectorMetaInfo.getMetricConfigMap().put(metricMetaInfo.getName(), metricMetaInfo);
          }
          //update Last modified time
          collectorMetaInfo.updateLastModified();
        }
      }

      LOG.info("MetricMetainfoSender finished");
    } catch (ChannelException ce) {
      // handling channel errors, like channel-full
      // in this case we will stop further writing to the channel
      LOG.error("Failed to add metrics metainfo to flume channel", ce);
    } catch (Throwable thr) {
      LOG.error("Exception while processing metrics metainfo", thr);
    }
  }

  private Map<String, MetricConfig> calculateChangedPercentileMetrics(Map<String, MetricConfig> changedMetrics) {
    Map<String, MetricConfig> changedPctlMetrics = new HashMap<String, MetricConfig>();
    for (MetricConfig metricConfig : changedMetrics.values()) {
      if (metricConfig.getPctls() != null) {
        Map<Long, String> pctlToNames = PercentileUtils
            .getPctlToNameMap(metricConfig.getPctls(), metricConfig.getName());
        for (Map.Entry<Long, String> pctl : pctlToNames.entrySet()) {

          MetricConfig pctlMetaInfo = new MetricConfig();
          pctlMetaInfo.setName(pctl.getValue());
          pctlMetaInfo.setNamespace(metricConfig.getNamespace());
          pctlMetaInfo.setLabel(String.format("%s - %sth pctl", metricConfig.getLabel(), pctl.getKey()));
          pctlMetaInfo.setDescription(metricConfig.getDescription());
          pctlMetaInfo.setUnit(metricConfig.getUnit());
          pctlMetaInfo.setType(metricConfig.getType());

          changedPctlMetrics.put(pctl.getValue(), pctlMetaInfo);
        }
      }
    }
    return changedPctlMetrics;
  }

  private Map<String, MetricConfig> processCollectorDir(File collectorDir) {

    if (LOG.isDebugEnabled()) {
      LOG.debug("Processing metaInfo in dir " + collectorDir);
    }

    Map<String, MetricConfig> changedMetrics = new HashMap<String, MetricConfig>();
    if (!collectorDir.exists()) {
      LOG.error("Cannot find config dir " + collectorDir);
      return changedMetrics;
    }

    if (!collectorDir.canRead()) {
      LOG.error("No read permission for " + collectorDir);
      return changedMetrics;
    }

    CollectorMetaInfo collectorMetaInfo = collectorMetaInfoMap.get(collectorDir.getAbsolutePath());
    if (collectorMetaInfo == null) {
      collectorMetaInfo = new CollectorMetaInfo(collectorDir);
      collectorMetaInfoMap.put(collectorDir.getAbsolutePath(), collectorMetaInfo);
    }

    //get list of changed yml files
    List<File> changedYmlFiles = new ArrayList<File>();
    Iterator fileIterator = FileUtils.iterateFiles(collectorDir, new String[] { "yml", "yaml" }, true);
    while (fileIterator.hasNext()) {
      File file = (File) fileIterator.next();
      if (file.canRead()) {
        if (collectorMetaInfo.isFileModified(file)) {
          changedYmlFiles.add(file);
        }
      } else {
        LOG.error("Cannot read yml config file" + file);
      }
    }

    //get changes in xml file
    if (changedYmlFiles.size() > 0) {
      for (File ymlFile : changedYmlFiles) {
        findChangedMetricsFromYml(ymlFile, collectorMetaInfo, changedMetrics);
      }
    }

    return changedMetrics;
  }

  private void findChangedMetricsFromYml(File configFile, CollectorMetaInfo collectorMetaInfo,
                                         Map<String, MetricConfig> changedMetrics) {
    CollectorFileConfig yamlConfig = MonitorUtil.FILE_NAME_TO_LOADED_YAML_CONFIG.get(configFile.getAbsolutePath());

    if (yamlConfig == null) {
      LOG.warn("Yaml config file " + configFile + " not loaded yet, metainfo logic will temporarily skip it");
      return;
    }
    try {
      readMetricInfo(collectorMetaInfo, yamlConfig, changedMetrics);

    } catch (Throwable e) {
      LOG.error("Exception while reading type info from file " + configFile, e);
    }
  }

  private void readMetricInfo(CollectorMetaInfo collectorMetaInfo, CollectorFileConfig yamlConfig,
                              Map<String, MetricConfig> changedMetrics) {

    for (ObservationDefinitionConfig obs : yamlConfig.getObservation()) {
      for (MetricConfig metric : obs.getMetric()) {
        if (!metric.isSend()) {
          continue;
        }
        String key = metric.getName();
        MetricConfig cachedMetricConfig = collectorMetaInfo.getMetricConfigMap().get(key);

        if (cachedMetricConfig != null) {
          if (!cachedMetricConfig.equals(metric)) {
            changedMetrics.put(key, metric);
          }
        } else {
          changedMetrics.put(key, metric);
        }
      }
    }
  }

  private EmbeddedSource getSource() {
    EmbeddedSource source = Sender.getSource(SenderType.METRICS_METAINFO);
    return source;
  }

  private static class CollectorMetaInfo {
    private File collectorDir;
    private Map<String, Long> fileToLastModifiedMap;
    private Map<String, MetricConfig> metricConfigMap;

    public CollectorMetaInfo(File collectorDir) {
      this.collectorDir = collectorDir;
      metricConfigMap = new HashMap<String, MetricConfig>();
      fileToLastModifiedMap = new HashMap<String, Long>();
    }

    public Map<String, MetricConfig> getMetricConfigMap() {
      return metricConfigMap;
    }

    public boolean isFileModified(File file) {
      boolean fileModified = true;
      if (fileToLastModifiedMap.containsKey(file.getAbsolutePath())) {
        fileModified = fileToLastModifiedMap.get(file.getAbsolutePath()) != file.lastModified();
      } else {
        fileToLastModifiedMap.put(file.getAbsolutePath(), file.lastModified());
      }
      return fileModified;
    }

    public void updateLastModified() {
      Set<String> files = new HashSet<String>(fileToLastModifiedMap.keySet());
      for (String file : files) {
        fileToLastModifiedMap.put(file, new File(file).lastModified());
      }
    }

    public File getCollectorDir() {
      return collectorDir;
    }
  }

}
