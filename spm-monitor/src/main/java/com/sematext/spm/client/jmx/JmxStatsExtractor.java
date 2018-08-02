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

import java.io.InputStream;
import java.util.Set;

import com.sematext.spm.client.MonitoredServiceUnavailableException;
import com.sematext.spm.client.StatsCollectionFailedException;
import com.sematext.spm.client.StatsCollectorBadConfigurationException;
import com.sematext.spm.client.StatsExtractor;
import com.sematext.spm.client.StatsExtractorConfig;
import com.sematext.spm.client.observation.ObservationBeanDump;

/**
 * Collects data from JMX
 */
public class JmxStatsExtractor extends StatsExtractor<JmxStatsExtractorConfig, MBeanObservation> {
  private final ContextEvaluator contextEvaluator;

  public JmxStatsExtractor(JmxStatsExtractorConfig config) {
    this(config, ContextEvaluator.EMPTY);
  }

  public JmxStatsExtractor(JmxStatsExtractorConfig config, ContextEvaluator contextEvaluator) {
    super(config);
    this.contextEvaluator = contextEvaluator;
  }

  @Override
  public void close() {
    JmxMBeanServerConnectionWrapper wrapper = JmxMBeanServerConnectionWrapper
        .getInstance(getConfig().getJmxServiceContext());

    if (wrapper != null) {
      wrapper.closeConnection();
    }
  }

  @Override
  protected Set<ObservationBeanDump> collectObservationStats(MBeanObservation observation)
      throws StatsCollectionFailedException {
    JmxMBeanServerConnectionWrapper connection = JmxMBeanServerConnectionWrapper
        .getInstance(getConfig().getJmxServiceContext());

    if (connection != null) {
      return observation.collectStats(MBeanObservationContext.make(connection.getMbeanServerConnection(),
                                                                   contextEvaluator.evaluate()));
    } else {
      // TODO ideally we would return empty set, but that requires a bit more extensive refactoring, so for now
      // throw the exception
      // return EMPTY_STATS_SET;

      throw new MonitoredServiceUnavailableException("Can't connect to JMX server");
    }
  }

  @Override
  protected boolean checkBeanShouldBeCollected(String observationName, MBeanObservation obsv) {
    return obsv.getName().equals(observationName);
  }

  public static JmxStatsExtractor make(InputStream config) throws StatsCollectorBadConfigurationException {
    return new JmxStatsExtractor(StatsExtractorConfig.make(JmxStatsExtractorConfig.class, config));
  }

  public static JmxStatsExtractor make(InputStream config, ContextEvaluator context)
      throws StatsCollectorBadConfigurationException {
    return new JmxStatsExtractor(StatsExtractorConfig.make(JmxStatsExtractorConfig.class, config), context);
  }
}
