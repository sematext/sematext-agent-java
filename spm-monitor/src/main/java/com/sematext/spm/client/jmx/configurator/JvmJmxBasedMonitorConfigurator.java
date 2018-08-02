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
package com.sematext.spm.client.jmx.configurator;

import static com.sematext.spm.client.StatsCollectorsFactory.updateCollector;
import static com.sematext.spm.client.StatsCollectorsFactory.updateCollectors;

import org.eclipse.collections.impl.list.mutable.FastList;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.Serializer;
import com.sematext.spm.client.StatsCollector;
import com.sematext.spm.client.StatsCollectorBadConfigurationException;
import com.sematext.spm.client.jmx.JmxServiceContext;
import com.sematext.spm.client.jvm.JvmGcStatsCollector;
import com.sematext.spm.client.jvm.JvmMemoryPoolStatsCollector;
import com.sematext.spm.client.jvm.JvmMemoryStatsCollector;
import com.sematext.spm.client.jvm.JvmNotifBasedGcStatsCollector;
import com.sematext.spm.client.jvm.JvmOsStatsCollector;
import com.sematext.spm.client.jvm.JvmThreadStatsCollector;
import com.sematext.spm.client.util.CollectionUtils.FunctionT;

public class JvmJmxBasedMonitorConfigurator extends JmxBasedMonitorConfigurator {
  private static final Log LOG = LogFactory.getLog(JvmJmxBasedMonitorConfigurator.class);
  private static final String GC_MBEAN_NAME = "GarbageCollector";
  private static final String POOL_MBEAN_NAME = "MemoryPool";

  private List<String> garbageCollectors = new FastList<String>();
  private List<String> poolNames = new FastList<String>();

  @Override
  public void readConfiguration(JmxServiceContext ctx) throws IOException {
    MBeanServerConnection conn = getMbeanServer(ctx);

    if (conn == null) {
      LOG.error("Configuration can't be read since jmx connection can't be created");
      return;
    }

    Set<ObjectInstance> objects = conn.queryMBeans(null, IGNORE_SOLR_FIELD_CACHE);
    Iterator<ObjectInstance> iter = objects.iterator();

    while (iter.hasNext()) {
      ObjectInstance o = iter.next();

      String objType = o.getObjectName().getKeyProperty("type");

      if (objType != null) {
        if (objType.equalsIgnoreCase(GC_MBEAN_NAME)) {
          String name = o.getObjectName().getKeyProperty("name");
          if (name != null) {
            garbageCollectors.add(name);
          } else {
            LOG.warn("Error during reading configuration, name is null for object of type GC_MBEAN_NAME, object: " +
                         o.toString());
          }
        } else if (objType.equalsIgnoreCase(POOL_MBEAN_NAME)) {
          String name = o.getObjectName().getKeyProperty("name");
          if (name != null) {
            poolNames.add(name);
          } else {
            LOG.warn("Error during reading configuration, name is null for object of type POOL_MBEAN_NAME, object: " +
                         o.toString());
          }
        }
      }
      /*
       * This is actually a regular situation, so we should not report this warning else {
       * LOG.warn("Error during reading configuration, type is null, object: " + o.toString()); }
       */
    }
  }

  public List<String> getGarbageCollectors() {
    return garbageCollectors;
  }

  public List<String> getPoolNames() {
    return poolNames;
  }

  @Override
  public void configure(Map<String, String> paramsMap, final MonitorConfig monitorConfig,
                        Collection<? extends StatsCollector<?>> currentCollectors,
                        Collection<? super StatsCollector<?>> newCollectors)
      throws StatsCollectorBadConfigurationException, IOException {
    this.readConfiguration(JmxServiceContext
                               .getContext(monitorConfig.getAppToken(), monitorConfig.getJvmName(), monitorConfig
                                   .getSubType()));
    final String appToken = monitorConfig.getAppToken();
    final String jvmName = monitorConfig.getJvmName();
    final String subType = monitorConfig.getSubType();
    updateCollector(currentCollectors, newCollectors, JvmMemoryStatsCollector.class, jvmName,
                    new FunctionT<String, JvmMemoryStatsCollector, StatsCollectorBadConfigurationException>() {
                      @Override
                      public JvmMemoryStatsCollector apply(String jvmName)
                          throws StatsCollectorBadConfigurationException {
                        return new JvmMemoryStatsCollector(appToken, jvmName, subType, monitorConfig);
                      }
                    });
    updateCollectors(currentCollectors, newCollectors, JvmMemoryPoolStatsCollector.class, poolNames,
                     new FunctionT<String, JvmMemoryPoolStatsCollector, StatsCollectorBadConfigurationException>() {
                       @Override
                       public JvmMemoryPoolStatsCollector apply(String poolName)
                           throws StatsCollectorBadConfigurationException {
                         return new JvmMemoryPoolStatsCollector(poolName, appToken, jvmName, subType, monitorConfig);
                       }
                     });
    updateCollectors(currentCollectors, newCollectors, JvmGcStatsCollector.class, getAcceptedGCs(paramsMap),
                     new FunctionT<String, JvmGcStatsCollector, StatsCollectorBadConfigurationException>() {
                       @Override
                       public JvmGcStatsCollector apply(String gcName) throws StatsCollectorBadConfigurationException {
                         return new JvmGcStatsCollector(gcName, appToken, jvmName, subType, monitorConfig);
                       }
                     });

    updateCollector(currentCollectors, newCollectors, JvmNotifBasedGcStatsCollector.class, jvmName,
                    new FunctionT<String, JvmNotifBasedGcStatsCollector, StatsCollectorBadConfigurationException>() {
                      @Override
                      public JvmNotifBasedGcStatsCollector apply(String jvmName) {
                        return new JvmNotifBasedGcStatsCollector(Serializer.INFLUX, appToken, jvmName, "unused");
                      }
                    });

    updateCollector(currentCollectors, newCollectors, JvmThreadStatsCollector.class, jvmName,
                    new FunctionT<String, JvmThreadStatsCollector, StatsCollectorBadConfigurationException>() {
                      @Override
                      public JvmThreadStatsCollector apply(String jvmName)
                          throws StatsCollectorBadConfigurationException {
                        return new JvmThreadStatsCollector(appToken, jvmName, subType, monitorConfig);
                      }
                    });

    updateCollector(currentCollectors, newCollectors, JvmOsStatsCollector.class, jvmName,
                    new FunctionT<String, JvmOsStatsCollector, StatsCollectorBadConfigurationException>() {
                      @Override
                      public JvmOsStatsCollector apply(String jvmName) throws StatsCollectorBadConfigurationException {
                        JvmOsStatsCollector collector = new JvmOsStatsCollector(appToken, jvmName, subType, monitorConfig);
                        return collector.isAcceptable() ? collector : null;
                      }
                    });
  }

  private List<String> getAcceptedGCs(Map<String, String> paramsMap) {
    List<String> collectors = new FastList<String>();
    List<String> garbageCollectors = getGarbageCollectors();
    String ignoredGarbageCollectors = paramsMap.get("ignoredGarbageCollectors");

    for (String gc : garbageCollectors) {
      gc = gc.trim();

      boolean ignore = false;

      if (ignoredGarbageCollectors != null) {
        for (String ignoredGc : ignoredGarbageCollectors.split(",")) {
          ignoredGc = ignoredGc.trim();

          if (ignoredGc.equals(gc)) {
            ignore = true;
          }
        }
      }
      if (!ignore) {
        collectors.add(gc);
      }
    }

    return collectors;
  }

}
