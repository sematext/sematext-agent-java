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

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class StormInfo {
  private final Map<String, String> stats;
  private final Map<String, Map<String, String>> supervisorStats;
  private final Map<String, Map<String, String>> topologyStats;
  private final Map<String, Map<ExecutorOutputStatsKey, Map<String, String>>> boltOutputStats;
  private final Map<String, Map<ExecutorInputStatsKey, Map<String, String>>> boltInputStats;
  private final Map<String, Map<ExecutorOutputStatsKey, Map<String, String>>> spoutOutputStats;

  private StormInfo() {
    stats = new UnifiedMap<String, String>();
    topologyStats = new UnifiedMap<String, Map<String, String>>();
    supervisorStats = new UnifiedMap<String, Map<String, String>>();
    boltOutputStats = new UnifiedMap<String, Map<ExecutorOutputStatsKey, Map<String, String>>>();
    boltInputStats = new UnifiedMap<String, Map<ExecutorInputStatsKey, Map<String, String>>>();
    spoutOutputStats = new UnifiedMap<String, Map<ExecutorOutputStatsKey, Map<String, String>>>();
  }

  public String get(String metricName) {
    return stats.get(metricName);
  }

  public void set(String metricName, String metricValue) {
    stats.put(metricName, metricValue);
  }

  public String getTopology(String topology, String metricName) {
    Map<String, String> topologyMetrics = topologyStats.get(topology);
    if (topologyMetrics == null) {
      return null;
    }
    return topologyMetrics.get(metricName);
  }

  public void setTopology(String topology, String metricName, String metricValue) {
    Map<String, String> topologyMetrics = topologyStats.get(topology);
    if (topologyMetrics == null) {
      topologyStats.put(topology, new UnifiedMap<String, String>());
    }
    topologyStats.get(topology).put(metricName, metricValue);
  }

  public Set<String> getTopologies() {
    return topologyStats.keySet();
  }

  public Set<ExecutorOutputStatsKey> getBoltOutputStats(String topology) {
    if (!boltOutputStats.containsKey(topology)) {
      return new HashSet<ExecutorOutputStatsKey>();
    }

    return boltOutputStats.get(topology).keySet();
  }

  public Set<ExecutorOutputStatsKey> getSpoutOutputStats(String topology) {
    if (!spoutOutputStats.containsKey(topology)) {
      return new HashSet<ExecutorOutputStatsKey>();
    }

    return spoutOutputStats.get(topology).keySet();
  }

  public Set<ExecutorInputStatsKey> getBoltInputStats(String topology) {
    if (!boltInputStats.containsKey(topology)) {
      return new HashSet<ExecutorInputStatsKey>();
    }

    return boltInputStats.get(topology).keySet();
  }

  public String getSupervisor(String supervisor, String metricName) {
    Map<String, String> supervisorMetrics = supervisorStats.get(supervisor);
    if (supervisorMetrics == null) {
      return null;
    }
    return supervisorMetrics.get(metricName);
  }

  public void setSupervisor(String supervisor, String metricName, String metricValue) {
    Map<String, String> supervisorMetrics = supervisorStats.get(supervisor);
    if (supervisorMetrics == null) {
      supervisorStats.put(supervisor, new UnifiedMap<String, String>());
    }
    supervisorStats.get(supervisor).put(metricName, metricValue);
  }

  public Set<String> getSupervisors() {
    return supervisorStats.keySet();
  }

  public String getBoltInputStats(String topology, ExecutorInputStatsKey statsKey, String metricName) {
    Map<ExecutorInputStatsKey, Map<String, String>> topologyMetrics = boltInputStats.get(topology);
    if (topologyMetrics == null) {
      return null;
    }

    Map<String, String> boltMetrics = topologyMetrics.get(statsKey);
    if (boltMetrics == null) {
      return null;
    }

    return boltMetrics.get(metricName);
  }

  public void setBoltInputStats(String topology, String executorId, String componentId, String metricName,
                                Map<StormInfoSource.HashedGlobalStreamId, ? extends Number> metricValue) {
    Map<ExecutorInputStatsKey, Map<String, String>> topologyMetrics = boltInputStats.get(topology);
    if (topologyMetrics == null) {
      boltInputStats.put(topology, new UnifiedMap<ExecutorInputStatsKey, Map<String, String>>());
    }

    for (StormInfoSource.HashedGlobalStreamId stream : metricValue.keySet()) {
      ExecutorInputStatsKey statsKey = new ExecutorInputStatsKey(executorId, componentId, stream
          .getComponentId(), stream.getStream());

      Map<String, String> boltMetrics = boltInputStats.get(topology).get(statsKey);

      if (boltMetrics == null) {
        boltInputStats.get(topology).put(statsKey, new UnifiedMap<String, String>());
      }

      boltInputStats.get(topology).get(statsKey).put(metricName, toString(metricValue.get(stream)));
    }
  }

  private String toString(Object value) {
    if (value == null) {
      return null;
    }
    return String.valueOf(value);
  }

  public String getSpoutOutputStats(String topology, ExecutorOutputStatsKey statsKey, String metricName) {
    Map<ExecutorOutputStatsKey, Map<String, String>> topologyMetrics = spoutOutputStats.get(topology);
    if (topologyMetrics == null) {
      return null;
    }

    Map<String, String> spoutMetrics = topologyMetrics.get(statsKey);
    if (spoutMetrics == null) {
      return null;
    }

    return spoutMetrics.get(metricName);
  }

  public void setSpoutOutputStats(String topology, String executorId, String componentId, String metricName,
                                  Map<String, ? extends Number> metricValue) {
    Map<ExecutorOutputStatsKey, Map<String, String>> topologyMetrics = spoutOutputStats.get(topology);
    if (topologyMetrics == null) {
      spoutOutputStats.put(topology, new UnifiedMap<ExecutorOutputStatsKey, Map<String, String>>());
    }

    for (String stream : metricValue.keySet()) {
      ExecutorOutputStatsKey statsKey = new ExecutorOutputStatsKey(executorId, componentId, stream);

      Map<String, String> spoutMetrics = spoutOutputStats.get(topology).get(statsKey);

      if (spoutMetrics == null) {
        spoutOutputStats.get(topology).put(statsKey, new UnifiedMap<String, String>());
      }

      spoutOutputStats.get(topology).get(statsKey).put(metricName, toString(metricValue.get(stream)));
    }
  }

  public String getBoltOutputStats(String topology, ExecutorOutputStatsKey statsKey, String metricName) {
    Map<ExecutorOutputStatsKey, Map<String, String>> topologyMetrics = boltOutputStats.get(topology);
    if (topologyMetrics == null) {
      return null;
    }

    Map<String, String> boltMetrics = topologyMetrics.get(statsKey);
    if (boltMetrics == null) {
      return null;
    }

    return boltMetrics.get(metricName);
  }

  public void setBoltOutputStats(String topology, String executorId, String componentId, String metricName,
                                 Map<String, ? extends Number> metricValue) {
    Map<ExecutorOutputStatsKey, Map<String, String>> topologyMetrics = boltOutputStats.get(topology);
    if (topologyMetrics == null) {
      boltOutputStats.put(topology, new UnifiedMap<ExecutorOutputStatsKey, Map<String, String>>());
    }

    for (String stream : metricValue.keySet()) {
      ExecutorOutputStatsKey statsKey = new ExecutorOutputStatsKey(executorId, componentId, stream);

      Map<String, String> boltMetrics = boltOutputStats.get(topology).get(statsKey);

      if (boltMetrics == null) {
        boltOutputStats.get(topology).put(statsKey, new UnifiedMap<String, String>());
      }

      boltOutputStats.get(topology).get(statsKey).put(metricName, toString(metricValue.get(stream)));
    }
  }

  public boolean isEmpty() {
    return stats.isEmpty();
  }

  public static StormInfo empty() {
    return new StormInfo();
  }
}
