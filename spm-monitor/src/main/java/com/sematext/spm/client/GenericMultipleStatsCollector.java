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

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

public abstract class GenericMultipleStatsCollector<T extends GenericExtractor<?, ?, ?, ?, ?, ?>, S extends StatsExtractorConfig<?>>
    extends MultipleStatsCollector<ExtractorResult> implements GenericCollectorInterface {
  private T genericExtractor;
  private String appToken;
  private String name;
  private String configBeanName;
  private String configName;
  private String realMonitoredBeanPath;
  private String metricsNamespace;

  public GenericMultipleStatsCollector(String metricsNamespace, Serializer serializer, MonitorConfig monitorConfig,
                                       String configName, String realMonitoredBeanPath,
                                       String configBeanName, String monitoredBeanConfigPath, S originalConfig,
                                       Map<String, String> beanPathTags)
      throws StatsCollectorBadConfigurationException, ConfigurationFailedException {
    super(serializer);
    genericExtractor = createExtractor(monitorConfig, realMonitoredBeanPath, configBeanName, monitoredBeanConfigPath, originalConfig,
                                       beanPathTags);
    this.appToken = monitorConfig.getAppToken();
    this.configBeanName = configBeanName;
    this.configName = configName;
    this.realMonitoredBeanPath = realMonitoredBeanPath;
    this.metricsNamespace = metricsNamespace;
  }

  protected abstract T createExtractor(MonitorConfig monitorConfig, String realMonitoredBeanPath, String configBeanName,
                                       String monitoredBeanConfigPath, S originalConfig,
                                       Map<String, String> beanPathTags)
      throws StatsCollectorBadConfigurationException, ConfigurationFailedException;

  @Override
  protected Collection<ExtractorResult> getSlice(Map<String, Object> outerMetrics)
      throws StatsCollectionFailedException {
    return genericExtractor.getStats(outerMetrics);
  }

  @Override
  protected void appendStats(ExtractorResult res, StatValues statValues) {
    statValues.setMetrics(res.stats);
    statValues.setAgentAggregationFunctions(genericExtractor.getAttributesToAgentAggregationFunctions());
    statValues.setMetricTypes(genericExtractor.getMetricTypes());
    statValues.setTags(res.tags);
    statValues.setTimestamp(System.currentTimeMillis());
    statValues.setAppToken(appToken);
    statValues.setMetricNamespace(metricsNamespace);
  }

  @Override
  public String getName() {
    return name;
  }

  public String toString() {
    return this.getClass().getName() + " using: " + genericExtractor + " for " + configBeanName;
  }

  @Override
  public void cleanup() {
    super.cleanup();
    if (genericExtractor != null) {
      genericExtractor.cleanup();
    }
  }

  public static String getCollectorIdentifier(String configName, String configBeanName, String realMonitoredBeanPath) {
    return configName + ":" + configBeanName + ":" + realMonitoredBeanPath;
  }

  @Override
  public String getCollectorIdentifier() {
    return getCollectorIdentifier(configName, configBeanName, realMonitoredBeanPath);
  }

  @Override
  public T getGenericExtractor() {
    return genericExtractor;
  }

  @Override
  public boolean producesMetricsAndTagsMaps() {
    return true;
  }

  public void updateEnvTags(MonitorConfig monitorConfig, Properties monitorProperties) {
    genericExtractor.updateEnvTags(monitorConfig, monitorProperties);
  }

  @Override
  public String getMetricsNamespace() {
    return metricsNamespace;
  }

  @Override
  public String getAppToken() {
    return appToken;
  }
}
