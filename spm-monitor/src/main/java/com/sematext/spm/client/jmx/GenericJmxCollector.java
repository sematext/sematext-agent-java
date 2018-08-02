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
package com.sematext.spm.client.jmx;

import java.util.Map;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.GenericCollector;
import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.Serializer;
import com.sematext.spm.client.StatsCollectorBadConfigurationException;

public class GenericJmxCollector extends GenericCollector<GenericJmxExtractor, JmxStatsExtractorConfig> {
  private static final Log LOG = LogFactory.getLog(GenericJmxCollector.class);

  public GenericJmxCollector(String metricsNamespace, Serializer serializer, MonitorConfig monitorConfig,
                             String configName, String realMonitoredBeanPath,
                             String configBeanName, String monitoredBeanConfigPath,
                             JmxStatsExtractorConfig originalConfig, Map<String, String> beanPathTags)
      throws StatsCollectorBadConfigurationException, ConfigurationFailedException {
    super(metricsNamespace, serializer, monitorConfig, configName, realMonitoredBeanPath, configBeanName, monitoredBeanConfigPath, originalConfig, beanPathTags);
  }

  @Override
  protected GenericJmxExtractor createExtractor(MonitorConfig monitorConfig, String realMonitoredBeanPath,
                                                String configBeanName, String monitoredBeanConfigPath,
                                                JmxStatsExtractorConfig originalConfig,
                                                Map<String, String> beanPathTags)
      throws StatsCollectorBadConfigurationException, ConfigurationFailedException {

    return new GenericJmxExtractor(realMonitoredBeanPath, configBeanName, originalConfig, beanPathTags, false);
  }
}
