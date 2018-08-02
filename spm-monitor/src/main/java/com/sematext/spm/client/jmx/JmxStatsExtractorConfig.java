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

import org.eclipse.collections.impl.list.mutable.FastList;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.StatsExtractorConfig;
import com.sematext.spm.client.config.CollectorFileConfig;
import com.sematext.spm.client.config.ObservationDefinitionConfig;

/**
 * Configuration of {@link com.sematext.spm.client.jmx.JmxStatsExtractor}
 */
public class JmxStatsExtractorConfig extends StatsExtractorConfig<MBeanObservation> {
  private JmxServiceContext jmxServiceContext;

  public JmxStatsExtractorConfig(JmxStatsExtractorConfig orig, boolean createObservationDuplicates) {
    super(orig, createObservationDuplicates);
  }

  public JmxStatsExtractorConfig(CollectorFileConfig config, MonitorConfig monitorConfig)
      throws ConfigurationFailedException {
    super(config, monitorConfig);
  }

  @Override
  protected void readFields(CollectorFileConfig config) throws ConfigurationFailedException {
    readMBeanObservations(config);

    readConditions(config);
  }

  @Override
  protected void updateStateAfterConstruction(MonitorConfig monitorConfig) {
    jmxServiceContext = JmxServiceContext.getContext(monitorConfig.getAppToken(),
                                                     monitorConfig.getJvmName(), monitorConfig.getSubType());
  }

  /**
   * <p>
   * Returns JMXServiceURL (to be used in remote connections), if it exists in configuration file. Else returns null.
   * </p>
   *
   * @return URL string.
   */
  public String getJmxServiceURL() {
    return jmxServiceContext != null ? jmxServiceContext.getUrl() : "";
  }

  /**
   * <p>
   * Returns user name (to be used in authentication).
   * </p>
   * If user name attribute does not exist in configuration file, it returns null.
   *
   * @return user name value
   */
  public String getUserName() {
    return jmxServiceContext != null ? jmxServiceContext.getUsername() : "";
  }

  /**
   * <p>
   * Returns password (to be used in authentication).
   * </p>
   * If password attribute does not exist in configuration file, it returns null.
   *
   * @return password value
   */
  public String getPassword() {
    return jmxServiceContext != null ? jmxServiceContext.getPassword() : "";
  }

  private void readMBeanObservations(CollectorFileConfig config) throws ConfigurationFailedException {
    setObservations(new FastList<MBeanObservation>());
    for (ObservationDefinitionConfig observationDefinition : config.getObservation()) {
      getObservations().add(new MBeanObservation(observationDefinition));
    }
  }

  @Override
  protected void copyFrom(StatsExtractorConfig<MBeanObservation> origConfig) {
    // copy properties, mbean observations were already copied
    this.jmxServiceContext = ((JmxStatsExtractorConfig) origConfig).jmxServiceContext;
  }

  public JmxServiceContext getJmxServiceContext() {
    return jmxServiceContext;
  }
}
