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
package com.sematext.spm.client.jvm;

import com.sun.management.GcInfo;

import org.eclipse.collections.impl.list.mutable.FastList;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.jmx.JmxMBeanServerConnectionWrapper;
import com.sematext.spm.client.jmx.JmxServiceContext;

import sun.management.GarbageCollectionNotifInfoCompositeData;

/*CHECKSTYLE:OFF*/
/*CHECKSTYLE:ON*/

public class JvmNotifBasedGcStatsExtractor {
  private List<GcAdditionalStats> gcStats = new FastList<GcAdditionalStats>();

  private static final Log LOG = LogFactory.getLog(JvmNotifBasedGcStatsExtractor.class);

  private Set<GarbageCollectorMXBean> getGCMXBeans(JmxServiceContext ctx) {
    JmxMBeanServerConnectionWrapper connection = JmxMBeanServerConnectionWrapper.getInstance(ctx);

    if (connection != null) {
      try {
        Set<ObjectName> gcnames = connection.getMbeanServerConnection()
            .queryNames(new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",name=*"), null);
        Set<GarbageCollectorMXBean> gcBeans = new HashSet<GarbageCollectorMXBean>(gcnames.size());
        for (ObjectName on : gcnames) {

          gcBeans.add(ManagementFactory.newPlatformMXBeanProxy(connection.getMbeanServerConnection(), on
              .toString(), GarbageCollectorMXBean.class));

        }
        return gcBeans;
      } catch (IOException e) {
        LOG.warn("Can't get GarbageCollectorMXBeans", e);
        return null;
      } catch (MalformedObjectNameException e) {
        LOG.warn("Can't get GarbageCollectorMXBeans", e);
        return null;
      }

    } else {
      LOG.warn("Can't connect to JMX server");
      return null;
    }
  }

  public JvmNotifBasedGcStatsExtractor(JmxServiceContext ctx) {
    Set<GarbageCollectorMXBean> collectors = getGCMXBeans(ctx);

    if (collectors == null) {
      return;
    }

    LOG.info("Number of Garbage Collectors: " + collectors.size());
    for (GarbageCollectorMXBean collector : collectors) {
      if (collector instanceof NotificationEmitter) {
        LOG.info("GC " + collector.getName() + " is NotificationEmitter, additional gc info can be obtained");
        NotificationEmitter garbageCollectorImpl = (NotificationEmitter) collector;

        garbageCollectorImpl.addNotificationListener(new GCNotificationListener(gcStats), null, null);

        LOG.info("GCNotificationListener was added for GC " + collector.getName() + ".");
      } else {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        LOG.warn("GC " + collector.getName() + " isn't NotificationEmitter. Jvm vendor: " +
                     runtimeMXBean.getVmVendor() + " jvm name: " + runtimeMXBean.getVmName() + " jvm version: " +
                     runtimeMXBean.getVmVersion());
      }
    }
  }

  private static final class GCNotificationListener implements NotificationListener {
    private List<GcAdditionalStats> gcStats;

    private GCNotificationListener(List<GcAdditionalStats> gcStats) {
      this.gcStats = gcStats;
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
      CompositeData data = (CompositeData) notification.getUserData();

      GcInfo gcInfo = GarbageCollectionNotifInfoCompositeData.getGcInfo(data);
      String gcName = GarbageCollectionNotifInfoCompositeData.getGcName(data);

      GcAdditionalStats gcStat = new GcAdditionalStats(gcName, gcInfo, System.currentTimeMillis());

      if (gcStat.isValid()) {
        gcStats.add(gcStat);
        if (System.currentTimeMillis() % 1000 == 0) {
          LOG.info("GC stats with duration: " + gcStat.getDuration() + " was added.");
        }
      } else {
        LOG.warn("GC stats has invalid duration: " + gcStat.getDuration());
      }
    }
  }

  public List<GcAdditionalStats> getStats() {
    List<GcAdditionalStats> currentStats = new FastList<GcAdditionalStats>(gcStats);
    gcStats.clear();
    return currentStats;
  }

  public static class GcAdditionalStats {
    //10h
    public static final int MAX_DURATION = 10 * 60 * 60 * 1000;

    private final Long duration;
    private final Long collectSize;
    private final String gcName;
    private final Long timestamp;

    GcAdditionalStats(String gcName, GcInfo gcInfo, Long timestamp) {
      this.gcName = gcName;
      this.timestamp = timestamp;
      this.duration = calculateDuration(gcInfo);
      this.collectSize = calculateCollectSize(gcInfo);
    }

    private Long calculateDuration(GcInfo gcInfo) {
      return gcInfo.getDuration();
    }

    private Long calculateSize(Map<String, MemoryUsage> memoryUsags) {
      Long size = 0L;
      for (MemoryUsage memoryUsage : memoryUsags.values()) {
        size += memoryUsage.getUsed();
      }
      return size;
    }

    private Long calculateCollectSize(GcInfo gcInfo) {

      Long beforeSize = calculateSize(gcInfo.getMemoryUsageBeforeGc());
      Long afterSize = calculateSize(gcInfo.getMemoryUsageAfterGc());

      Long collectSize = beforeSize - afterSize;

      return collectSize > 0 ? collectSize : 0;
    }

    private boolean sanityDuration(Long duration) {
      return duration > 0 && duration < MAX_DURATION;
    }

    public boolean isValid() {
      return sanityDuration(duration);
    }

    public Long getDuration() {
      return duration;
    }

    public Long getCollectSize() {
      return collectSize;
    }

    public String getGcName() {
      return gcName;
    }

    public Long getTimestamp() {
      return timestamp;
    }
  }
}
