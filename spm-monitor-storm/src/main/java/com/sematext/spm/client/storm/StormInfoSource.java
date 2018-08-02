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

import org.apache.thrift7.TException;
import org.apache.thrift7.protocol.TBinaryProtocol;
import org.apache.thrift7.transport.TFramedTransport;
import org.apache.thrift7.transport.TSocket;
import org.apache.thrift7.transport.TTransportException;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

import backtype.storm.generated.*;

public final class StormInfoSource {
  private static final Log LOG = LogFactory.getLog(StormInfoSource.class);
  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_PORT = 6627;
  private static final int CACHE_TTL = 3000;
  private long lastQueryTime = System.currentTimeMillis();
  private StormInfo cachedInfo = StormInfo.empty();
  private Nimbus.Client nimbus;
  private TFramedTransport tTransport;

  private StormInfoSource(final Nimbus.Client nimbus, final TFramedTransport tTransport) {
    this.nimbus = nimbus;
    this.tTransport = tTransport;
  }

  public StormInfo fetchInfo() {
    boolean cacheExpired = System.currentTimeMillis() - lastQueryTime > CACHE_TTL;

    if (cachedInfo.isEmpty() || cacheExpired) {
      try {
        tTransport.open();

        StormInfo info = StormInfo.empty();

        ClusterSummary clusterInfo = nimbus.getClusterInfo();
        info.set("supervisors_count", String.valueOf(clusterInfo.get_supervisors_size()));
        info.set("topologies_count", String.valueOf(clusterInfo.get_topologies_size()));

        for (SupervisorSummary supervisorSummary : clusterInfo.get_supervisors()) {
          info.setSupervisor(supervisorSummary.get_supervisor_id(), "slots_count", String
              .valueOf(supervisorSummary.get_num_workers()));
          info.setSupervisor(supervisorSummary.get_supervisor_id(), "used_slots_count", String
              .valueOf(supervisorSummary.get_num_used_workers()));
          info.setSupervisor(supervisorSummary.get_supervisor_id(), "supervisor_host", supervisorSummary.get_host());
        }

        for (TopologySummary topologySummary : clusterInfo.get_topologies()) {
          TopologyInfo topologyInfo = nimbus.getTopologyInfo(topologySummary.get_id());
          StormTopology topology = nimbus.getTopology(topologySummary.get_id());

          info.setTopology(topologySummary.get_name(), "workers_count", String
              .valueOf(topologySummary.get_num_workers()));
          info.setTopology(topologySummary.get_name(), "executors_count", String
              .valueOf(topologySummary.get_num_executors()));
          info.setTopology(topologySummary.get_name(), "tasks_count", String.valueOf(topologySummary.get_num_tasks()));
          info.setTopology(topologySummary.get_name(), "topology_status", topologySummary.get_status());

          info.setTopology(topologySummary.get_name(), "bolts_count", String.valueOf(topology.get_bolts_size()));
          info.setTopology(topologySummary.get_name(), "spouts_count", String.valueOf(topology.get_spouts_size()));
          info.setTopology(topologySummary.get_name(), "state_spouts_count", String
              .valueOf(topology.get_state_spouts_size()));

          for (ExecutorSummary executor : topologyInfo.get_executors()) {
            ExecutorStats executorStats = executor.get_stats();

            //0.8.2 version doesn't have executorStats property
            if (executorStats == null) {
              continue;
            }

            //we want to skip "acker" executor
            String component_id = executor.get_component_id();
            if (component_id.equals("__acker")) {
              continue;
            }

            ExecutorSpecificStats specific = executorStats.get_specific();

            int taskStart = executor.get_executor_info().get_task_start();
            int taskEnd = executor.get_executor_info().get_task_end();

            String executorId = taskStart + "-" + taskEnd;

            if (specific.is_set_bolt()) {
              BoltStats bolt = specific.get_bolt();

              Map<String, Long> boltsEmitted = getAllTimeValues(executorStats.get_emitted());
              Map<String, Long> boltsTransferred = getAllTimeValues(executorStats.get_transferred());

              Set<String> streams = new HashSet<String>();
              streams.addAll(boltsEmitted.keySet());
              streams.addAll(boltsTransferred.keySet());

              boltsEmitted = fillMissedValues(streams, boltsEmitted);
              boltsTransferred = fillMissedValues(streams, boltsTransferred);

              Map<HashedGlobalStreamId, Long> executed = convertToHashedKeyMap(getAllTimeValues(bolt.get_executed()));
              Map<HashedGlobalStreamId, Long> asked = convertToHashedKeyMap(getAllTimeValues(bolt.get_acked()));
              Map<HashedGlobalStreamId, Long> failed = convertToHashedKeyMap(getAllTimeValues(bolt.get_failed()));
              Map<HashedGlobalStreamId, Double> executeMsAvg = convertToHashedKeyMap(getAllTimeValues(bolt.get_execute_ms_avg()));
              Map<HashedGlobalStreamId, Double> processMsAvg = convertToHashedKeyMap(getAllTimeValues(bolt.get_process_ms_avg()));

              Set<HashedGlobalStreamId> hashedGlobalStreamIds = new HashSet<HashedGlobalStreamId>();
              hashedGlobalStreamIds.addAll(executed.keySet());
              hashedGlobalStreamIds.addAll(asked.keySet());
              hashedGlobalStreamIds.addAll(failed.keySet());
              hashedGlobalStreamIds.addAll(executeMsAvg.keySet());
              hashedGlobalStreamIds.addAll(processMsAvg.keySet());

              executed = fillMissedValues(hashedGlobalStreamIds, executed);
              asked = fillMissedValues(hashedGlobalStreamIds, asked);
              failed = fillMissedValues(hashedGlobalStreamIds, failed);
              executeMsAvg = fillMissedValues(hashedGlobalStreamIds, executeMsAvg);
              processMsAvg = fillMissedValues(hashedGlobalStreamIds, processMsAvg);

              //execute latency = timestamp when execute function ends - timestamp when execute is passed tuple
              Map<HashedGlobalStreamId, Double> boltsExecutedLatency = multiply(executed, executeMsAvg);

              //processing latency = timestamp when ack is called - timestamp when execute is passed tuple
              Map<HashedGlobalStreamId, Double> boltsProcessedLatency = multiply(sum(asked, failed), processMsAvg);

              //bolt output stats per stream + executor id
              info.setBoltOutputStats(topologySummary
                                          .get_name(), executorId, component_id, "bolts_emitted", boltsEmitted);
              info.setBoltOutputStats(topologySummary
                                          .get_name(), executorId, component_id, "bolts_transferred", boltsTransferred);

              //bolt input stats per component + stream + executor id
              info.setBoltInputStats(topologySummary.get_name(), executorId, component_id, "bolts_asked", asked);
              info.setBoltInputStats(topologySummary.get_name(), executorId, component_id, "bolts_executed", executed);
              info.setBoltInputStats(topologySummary.get_name(), executorId, component_id, "bolts_failed", failed);

              info.setBoltInputStats(topologySummary
                                         .get_name(), executorId, component_id, "bolts_executed_latency", boltsExecutedLatency);
              info.setBoltInputStats(topologySummary
                                         .get_name(), executorId, component_id, "bolts_processed_latency", boltsProcessedLatency);

            } else if (specific.is_set_spout()) {
              SpoutStats spout = specific.get_spout();

              Map<String, Long> spoutEmitted = getAllTimeValues(executorStats.get_emitted());
              Map<String, Long> spoutTransferred = getAllTimeValues(executorStats.get_transferred());
              Map<String, Long> spoutsAsked = getAllTimeValues(spout.get_acked());
              Map<String, Long> spoutsFailed = getAllTimeValues(spout.get_failed());
              Map<String, Double> completesMsAvg = getAllTimeValues(spout.get_complete_ms_avg());

              Set<String> streams = new HashSet<String>();
              streams.addAll(spoutEmitted.keySet());
              streams.addAll(spoutTransferred.keySet());
              streams.addAll(spoutsAsked.keySet());
              streams.addAll(spoutsFailed.keySet());
              streams.addAll(completesMsAvg.keySet());

              spoutEmitted = fillMissedValues(streams, spoutEmitted);
              spoutTransferred = fillMissedValues(streams, spoutTransferred);
              spoutsAsked = fillMissedValues(streams, spoutsAsked);
              spoutsFailed = fillMissedValues(streams, spoutsFailed);
              completesMsAvg = fillMissedValues(streams, completesMsAvg);

              //complete latency = timestamp when spout emits tuple - timestamp of completed ack tree
              Map<String, Double> spoutsCompleteLatency = multiply(sum(spoutsAsked, spoutsFailed), completesMsAvg);

              //spout output stats per stream + executor id
              info.setSpoutOutputStats(topologySummary
                                           .get_name(), executorId, component_id, "spouts_emitted", spoutEmitted);
              info.setSpoutOutputStats(topologySummary
                                           .get_name(), executorId, component_id, "spouts_transferred", spoutTransferred);

              //spout input stats per component + stream + executor id
              info.setSpoutOutputStats(topologySummary
                                           .get_name(), executorId, component_id, "spouts_asked", spoutsAsked);
              info.setSpoutOutputStats(topologySummary
                                           .get_name(), executorId, component_id, "spouts_failed", spoutsFailed);

              info.setSpoutOutputStats(topologySummary
                                           .get_name(), executorId, component_id, "spouts_complete_latency", spoutsCompleteLatency);
            }
          }

        }

        cachedInfo = info;

      } catch (TTransportException e) {
        LOG.error("Can't get info from Nimbus", e);
      } catch (TException e) {
        LOG.error("Can't get info from Nimbus", e);
      } catch (NotAliveException e) {
        LOG.error("Can't get info from Nimbus", e);
      } finally {
        tTransport.close();
      }
    }
    return cachedInfo;
  }

  private <K, V> Map<K, V> fillMissedValues(Set<K> requiredKeys, Map<K, V> values) {
    for (K requiredKey : requiredKeys) {
      if (!values.containsKey(requiredKey)) {
        values.put(requiredKey, null);
      }
    }

    return values;
  }

  private <T, V> Map<T, V> getAllTimeValues(Map<String, Map<T, V>> values) {
    Map<T, V> map = values.get(":all-time");

    if (map != null) {
      return map;
    }

    return new HashMap<T, V>();
  }

  private <T, V> Map<HashedGlobalStreamId, V> convertToHashedKeyMap(Map<GlobalStreamId, V> values) {
    Map<HashedGlobalStreamId, V> result = new UnifiedMap<HashedGlobalStreamId, V>();

    for (Map.Entry<GlobalStreamId, V> entry : values.entrySet()) {
      GlobalStreamId key = entry.getKey();
      result.put(new HashedGlobalStreamId(key.get_componentId(), key.get_streamId()), entry.getValue());
    }

    return result;
  }

  public static final class HashedGlobalStreamId {
    private String componentId;
    private String stream;

    public HashedGlobalStreamId(String componentId, String stream) {
      this.componentId = componentId;
      this.stream = stream;
    }

    public String getComponentId() {
      return componentId;
    }

    public void setComponentId(String componentId) {
      this.componentId = componentId;
    }

    public String getStream() {
      return stream;
    }

    public void setStream(String stream) {
      this.stream = stream;
    }

    /*CHECKSTYLE:OFF*/
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      HashedGlobalStreamId that = (HashedGlobalStreamId) o;

      if (componentId != null ? !componentId.equals(that.componentId) : that.componentId != null)
        return false;
      if (stream != null ? !stream.equals(that.stream) : that.stream != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = componentId != null ? componentId.hashCode() : 0;
      result = 31 * result + (stream != null ? stream.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return "HashedGlobalStreamId{" +
          "componentId='" + componentId + '\'' +
          ", stream='" + stream + '\'' +
          '}';
    }

    /*CHECKSTYLE:ON*/
  }

  private <T, V extends Number> Map<T, Double> multiply(Map<T, ? extends V> argMap1, Map<T, ? extends V> argMap2) {
    Map<T, Double> result = new UnifiedMap<T, Double>();

    for (T key : argMap1.keySet()) {
      Number arg1 = argMap1.get(key);
      Number arg2 = argMap2.get(key);

      if (arg1 == null || arg2 == null) {
        result.put(key, null);
        continue;
      }

      result.put(key, arg1.doubleValue() * arg2.doubleValue());
    }

    return result;
  }

  private <T, V extends Number> Map<T, Long> sum(Map<T, ? extends V> argMap1, Map<T, ? extends V> argMap2) {
    Map<T, Long> result = new UnifiedMap<T, Long>();

    for (T key : argMap1.keySet()) {
      Number arg1 = argMap1.get(key);
      Number arg2 = argMap2.get(key);

      if (arg1 == null || arg2 == null) {
        result.put(key, null);
        continue;
      }

      result.put(key, arg1.longValue() + arg2.longValue());
    }

    return result;
  }

  public static StormInfoSource collector(String host, Integer port) {
    final String fixedHost = host == null ? DEFAULT_HOST : host;
    final Integer fixedPort = port == null ? DEFAULT_PORT : port;

    TSocket tsocket = new TSocket(fixedHost, fixedPort);
    TFramedTransport tTransport = new TFramedTransport(tsocket);
    TBinaryProtocol tBinaryProtocol = new TBinaryProtocol(tTransport);
    Nimbus.Client client = new Nimbus.Client(tBinaryProtocol);

    return new StormInfoSource(client, tTransport);

  }
}
