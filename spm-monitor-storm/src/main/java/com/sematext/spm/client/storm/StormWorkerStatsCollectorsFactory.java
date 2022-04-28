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
package com.sematext.spm.client.storm;

import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.sematext.spm.client.HeartbeatStatsCollector;
import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.Serializer;
import com.sematext.spm.client.StatsCollector;
import com.sematext.spm.client.StatsCollectorBadConfigurationException;
import com.sematext.spm.client.StatsCollectorsFactory;
import com.sematext.spm.client.jmx.configurator.JvmJmxBasedMonitorConfigurator;
import com.sematext.spm.client.util.CollectionUtils.FunctionT;

public class StormWorkerStatsCollectorsFactory extends StatsCollectorsFactory<StatsCollector<?>> {
  private static final Log LOG = LogFactory.getLog(StormWorkerStatsCollectorsFactory.class);
  private final JvmJmxBasedMonitorConfigurator jvmJmxConf = new JvmJmxBasedMonitorConfigurator();

  public Collection<? extends StatsCollector<?>> create(Properties monitorProperties,
                                                        List<? extends StatsCollector<?>> currentCollectors,
                                                        MonitorConfig monitorConfig)
      throws StatsCollectorBadConfigurationException {
    try {
      final String jvmName = monitorConfig.getJvmName();
      final String appToken = monitorConfig.getAppToken();
      final String subType = monitorConfig.getSubType();

      List<StatsCollector<?>> collectors = new FastList<StatsCollector<?>>();

      // first read available JVM data from Jmx
      jvmJmxConf.configure(Collections.EMPTY_MAP, monitorConfig, currentCollectors, collectors);

      // as last collector add HeartbeatCollector
      updateCollector(currentCollectors, collectors, HeartbeatStatsCollector.class, jvmName,
          new FunctionT<String, HeartbeatStatsCollector, StatsCollectorBadConfigurationException>() {
            @Override
            public HeartbeatStatsCollector apply(String id) {
              return new HeartbeatStatsCollector(Serializer.INFLUX, appToken, jvmName, subType);
            }
          });      

      return collectors;
    } catch (StatsCollectorBadConfigurationException e) {
      throw e;
    } catch (Exception ex) {
      throw new StatsCollectorBadConfigurationException("Error while creating collectors!", ex);
    }
  }
}

