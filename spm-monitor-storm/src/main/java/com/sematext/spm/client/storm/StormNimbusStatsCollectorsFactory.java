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
import com.sematext.spm.client.tracing.TracingMonitorConfigurator;
import com.sematext.spm.client.util.CollectionUtils.FunctionT;

public class StormNimbusStatsCollectorsFactory extends StatsCollectorsFactory<StatsCollector<?>> {
  private static final Log LOG = LogFactory.getLog(StormNimbusStatsCollectorsFactory.class);

  private final JvmJmxBasedMonitorConfigurator jvmJmxConf = new JvmJmxBasedMonitorConfigurator();
  private final TracingMonitorConfigurator tracingConf = new TracingMonitorConfigurator();

  @Override
  public Collection<? extends StatsCollector<?>> create(Properties monitorProperties,
                                                        List<? extends StatsCollector<?>> currentCollectors,
                                                        MonitorConfig monitorConfig)
      throws StatsCollectorBadConfigurationException {
    final String jvmName = monitorConfig.getJvmName();
    final String appToken = monitorConfig.getAppToken();
    final String subType = monitorConfig.getSubType();

    List<StatsCollector<?>> collectors = new FastList<StatsCollector<?>>();
    configureStormCollectors(monitorProperties, collectors, currentCollectors, appToken, jvmName, subType);

    try {
      jvmJmxConf.configure(Collections.EMPTY_MAP, monitorConfig, currentCollectors, collectors);

      if (MonitorUtil.MONITOR_RUNTIME_SETUP_JAVAAGENT.get()) {
        boolean tracingEnabled = "true".equalsIgnoreCase(MonitorUtil.stripQuotes(monitorProperties
                                                                                     .getProperty("SPM_MONITOR_TRACING_ENABLED", "false")
                                                                                     .trim()).trim());

        if (tracingEnabled) {
          try {
            // always configure, it collects only if right settings are present though (handles it internally)
            tracingConf
                .configure(monitorConfig, currentCollectors, collectors, Serializer.INFLUX, appToken, subType, jvmName);
          } catch (Throwable thr) {
            // don't propagate, just continue
            LOG.error("Error while configuring tracing conf", thr);
          }
        }
      }
      
      // as last collector add HeartbeatCollector
      updateCollector(currentCollectors, collectors, HeartbeatStatsCollector.class, jvmName,
          new FunctionT<String, HeartbeatStatsCollector, StatsCollectorBadConfigurationException>() {
            @Override
            public HeartbeatStatsCollector apply(String id) {
              return new HeartbeatStatsCollector(Serializer.INFLUX, appToken, jvmName, subType);
            }
          });      
    } catch (StatsCollectorBadConfigurationException e) {
      throw e;
    } catch (Exception ex) {
      throw new StatsCollectorBadConfigurationException("Error while creating collectors!", ex);
    }

    return collectors;
  }

  private void configureStormCollectors(Properties monitorProperties, List<StatsCollector<?>> collectors,
                                        List<? extends StatsCollector<?>> statsCollectors,
                                        String appToken, String jvmName, String subType)
      throws StatsCollectorBadConfigurationException {
    StormInfoSource infoSource = buildStormInfoSource(monitorProperties);

    List<StatsCollector<?>> stormStatsCollectors = new FastList<StatsCollector<?>>();
    if (statsCollectors != null) {
      for (StatsCollector collector : statsCollectors) {
        if (UpdatableStormStatsCollector.class.isAssignableFrom(collector.getClass())) {
          UpdatableStormStatsCollector stormCollector = (UpdatableStormStatsCollector) collector;
          stormCollector.updateSource(infoSource);
          stormStatsCollectors.add(collector);
        }
      }
    }

    if (stormStatsCollectors.isEmpty()) {
      stormStatsCollectors.add(new StormClusterStatsCollector(infoSource, appToken, jvmName, subType));
      stormStatsCollectors.add(new StormSupervisorStatsCollector(infoSource, appToken, jvmName, subType));
      stormStatsCollectors.add(new StormTopologyStatsCollector(infoSource, appToken, jvmName, subType));
      stormStatsCollectors.add(new StormBoltInputStatsCollector(infoSource, appToken, jvmName, subType));
      stormStatsCollectors.add(new StormBoltOutputStatsCollector(infoSource, appToken, jvmName, subType));
      stormStatsCollectors.add(new StormSpoutOutputStatsCollector(infoSource, appToken, jvmName, subType));
    }

    collectors.addAll(stormStatsCollectors);
  }

  private StormInfoSource buildStormInfoSource(Properties monitorProperties)
      throws StatsCollectorBadConfigurationException {
    Integer nimbusPort = null;
    String nimbusHost = null;
    String nimbusHostParam = MonitorUtil.stripQuotes(monitorProperties.getProperty("NIMBUS_HOST", "localhost").trim())
        .trim();
    String portParam = MonitorUtil.stripQuotes(monitorProperties.getProperty("NIMBUS_PORT", "localhost").trim()).trim();
    if (nimbusHostParam != null && !nimbusHostParam.isEmpty()) {
      nimbusHost = nimbusHostParam;
    }
    if (portParam != null && !portParam.isEmpty()) {
      try {
        nimbusPort = Integer.parseInt(portParam);
      } catch (NumberFormatException e) {
        throw new StatsCollectorBadConfigurationException("Can't parse  nimbusPort parameter " + portParam + ".");
      }
    }
    return StormInfoSource.collector(nimbusHost, nimbusPort);
  }
}
