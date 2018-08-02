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
package com.sematext.spm.client.db;

import java.util.Map;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.GenericExtractor;
import com.sematext.spm.client.StatsCollectorBadConfigurationException;
import com.sematext.spm.client.config.CollectorFileConfig.ConfigType;

public class GenericDbExtractor extends GenericExtractor<DbStatsExtractorConfig, DbStatsExtractor,
    DbObservation, DbAttributeObservation, Map<String, Object>, Map<String, Object>> {

  public GenericDbExtractor(String realMonitoredBeanPath, String configBeanName,
                            DbStatsExtractorConfig originalConfig,
                            Map<String, String> beanPathTags, boolean multiResultAllowed)
      throws StatsCollectorBadConfigurationException, ConfigurationFailedException {
    super(realMonitoredBeanPath, configBeanName, originalConfig, beanPathTags, multiResultAllowed);
  }

  @Override
  protected DbObservation createBeanObservation(DbObservation obsConfig, String configBeanName,
                                                String realMonitoredBeanPath, Map<String, String> beanPathTags) {
    return new DbObservation(obsConfig, configBeanName);
  }

  @Override
  protected DbStatsExtractor createStatsExtractor(DbStatsExtractorConfig config)
      throws StatsCollectorBadConfigurationException {
    return new DbStatsExtractor(config);
  }

  @Override
  protected DbStatsExtractorConfig createExtractorConfig(DbStatsExtractorConfig originalConfig) {
    return new DbStatsExtractorConfig(originalConfig, false);
  }

  public static boolean canBeMonitored(String configBeanName, DbStatsExtractorConfig originalConfig,
                                       Map<String, String> beanPathTags) {
    for (DbObservation obs : originalConfig.getObservations()) {
      if (obs.getName().equals(configBeanName) && !obs.shouldBeIgnored(beanPathTags)) {
        return true;
      }
    }

    return false;
  }

  @Override
  protected ConfigType getConfigType() {
    return ConfigType.DB;
  }
}
