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
package com.sematext.spm.client.es.info;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.es.CustomEsClusterStateJsonHandler;
import com.sematext.spm.client.es.CustomEsIndicesRootJsonHandler;
import com.sematext.spm.client.http.CachableReliableDataSourceBase;
import com.sematext.spm.client.http.ServerInfo;
import com.sematext.spm.client.json.JsonDataProvider;
import com.sematext.spm.client.json.JsonDataSourceCachedFactory;
import com.sematext.spm.client.json.JsonUtil;

/**
 * Utility class which knows how to access ES Cluster Info.
 * <p/>
 * TODO - think about optimizing some methods by caching result. Not all method can be cached, though,
 * due to dynamic nature of ES cluster.
 */
public final class EsClusterInfo {
  private static final Log LOG = LogFactory.getLog(EsClusterInfo.class);

  private static final String VERSION_INFO_URL = "$HOST:$PORT/?format=smile";
  private static final String CLUSTER_HEALTH_URL = "$HOST:$PORT/_cluster/health?format=smile";

  private static final String NODES_INFO_URL_OLD = "$HOST:$PORT/_cluster/nodes/_local?format=smile";
  private static final String INDEX_INFO_URL_OLD = "$HOST:$PORT/_stats?indexing=true&store=true&search=true&merge=true&refresh=true&flush=true&level=shards&format=smile";
  private static final String CLUSTER_ARCHITECTURE_DATA_URL_OLD = "$HOST:$PORT/_cluster/nodes/_local?format=smile";
  private static final String STATS_INFO_URL_OLD = "$HOST:$PORT/_stats?indexing=true&store=true&search=true&merge=true&refresh=true&flush=true&level=shards&format=smile";
  private static final String LIST_NODES_INFO_OLD = "$HOST:$PORT/_nodes?transport=true&format=smile";

  private static final String NODES_INFO_URL_100 = "$HOST:$PORT/_nodes/_local?format=smile";
  private static final String INDEX_INFO_URL_100 = "$HOST:$PORT/_stats/indexing,store,search,merge,refresh,flush,docs,get?level=shards&format=smile";
  private static final String CLUSTER_ARCHITECTURE_DATA_URL_100 = "$HOST:$PORT/_nodes/_local?format=smile";
  private static final String STATS_INFO_URL_100 = "$HOST:$PORT/_stats?level=shards&format=smile";
  private static final String LIST_NODES_INFO_100 = "$HOST:$PORT/_nodes/transport?format=smile";

  private static final String CLUSTER_TOPOLOGY_INFO_URL_100 = "$HOST:$PORT/_cluster/state/nodes,routing_table,routing_nodes,master_node?local=true&format=smile";
  private static final String CLUSTER_TOPOLOGY_INFO_URL_OLD = "$HOST:$PORT/_cluster/state?filter_metadata=true&filter_indices=true&filter_blocks=true&local=true&format=smile";

  public static final String SHARDS_HEALTH_INDEX_LEVEL = "$HOST:$PORT/_cluster/health?level=indices&format=smile";

  private static final String JVM_STATS_URL = "$HOST:$PORT/_nodes/_local/stats/indices,transport,http,thread_pool,jvm,process?format=smile";

  private static final String NODES_META_INFO = "$HOST:$PORT/_cat/nodes?h=id,n,r,i&format=json";

  // get name from http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/cat-thread-pool.html
  public static final List<String> POOL_NAMES = Arrays
      .asList(new String[] { "bulk", "flush", "generic", "get", "index", "management", "merge", "optimize", "percolate",
          "refresh", "search", "snapshot", "suggest", "warmer" });
  public static final List<String> FIELD_NAMES = Arrays
      .asList(new String[] { "type", "active", "size", "queue", "queueSize", "rejected", "largest", "completed", "min",
          "max", "keepAlive" });

  private static String THREAD_POOL_URL = null;
  private static final String THREAD_POOL_V5_URL = "$HOST:$PORT/_cat/thread_pool?format=json&h=node_id,host,ip,post,name,type,active,size,queue,queue_size,rejected,largest,completed,min,max,keep_alive";

  private static final StringBuilder ES_INDICES_JSON_ROOT_ELEMENT = new StringBuilder();

  private static final Map<String, String> ES_VERSION_MAP = new UnifiedMap<String, String>();


  /*
   *  References:  
   * - Node roles: https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-node.html#node-roles  
   * - Data tiers: https://www.elastic.co/guide/en/elasticsearch/reference/current/data-tiers.html  
   * - ILM overview: https://www.elastic.co/guide/en/elasticsearch/reference/current/overview-index-lifecycle-management.html  
   */
  private static final Set<Character> DATA_HOSTING_ROLES = Set.of(
    'd', // data (general data node)
    'h', // data_hot (hot tier) - stores frequently accessed data
    'w', // data_warm (warm tier) - stores less frequently accessed data    
    'c', // data_cold (cold tier) - stores infrequently accessed data  
    'f', // data_frozen (frozen tier) - stores rarely accessed data with reduced compute  
    's', // transform - can store data for transform operations  
  );

  private EsClusterInfo() {
  }

  private static String getStatsInfoUrl(ServerInfo jsonServerInfo) {
    if (getEsVersion(jsonServerInfo) == null) {
      return null;
    }

    if (getEsVersion(jsonServerInfo).startsWith("0.")) {
      return STATS_INFO_URL_OLD;
    } else {
      if (getEsVersion(jsonServerInfo) == null) {
        return null;
      }
      return STATS_INFO_URL_100;
    }
  }

  private static String getClusterArchitectureDataUrl(ServerInfo jsonServerInfo) {
    if (getEsVersion(jsonServerInfo) == null) {
      return null;
    }
    if (getEsVersion(jsonServerInfo).startsWith("0.")) {
      return CLUSTER_ARCHITECTURE_DATA_URL_OLD;
    } else {
      return CLUSTER_ARCHITECTURE_DATA_URL_100;
    }
  }

  public static String getNodesInfoUrl(ServerInfo jsonServerInfo) {
    if (getEsVersion(jsonServerInfo) == null) {
      return null;
    }
    if (getEsVersion(jsonServerInfo).startsWith("0.")) {
      return NODES_INFO_URL_OLD;
    } else {
      return NODES_INFO_URL_100;
    }
  }

  public static String getListNodesInfoUrl(ServerInfo jsonServerInfo) {
    if (getEsVersion(jsonServerInfo) == null) {
      return null;
    }
    if (getEsVersion(jsonServerInfo).startsWith("0.")) {
      return LIST_NODES_INFO_OLD;
    } else {
      return LIST_NODES_INFO_100;
    }
  }

  public static String getIndexInfoUrl(ServerInfo jsonServerInfo) {
    if (getEsVersion(jsonServerInfo) == null) {
      return null;
    }
    if (getEsVersion(jsonServerInfo).startsWith("0.")) {
      return INDEX_INFO_URL_OLD;
    } else {
      return INDEX_INFO_URL_100;
    }
  }

  public static String getClusterHealthUrl() {
    return CLUSTER_HEALTH_URL;
  }

  public static String getClusterTopologyUrl(ServerInfo jsonServerInfo) {
    if (getEsVersion(jsonServerInfo) == null) {
      return null;
    }
    if (getEsVersion(jsonServerInfo).startsWith("0.")) {
      return CLUSTER_TOPOLOGY_INFO_URL_OLD;
    } else {
      return CLUSTER_TOPOLOGY_INFO_URL_100;
    }
  }

  public static String getThreadPoolUrl(ServerInfo jsonServerInfo) {
    if (THREAD_POOL_URL != null) {
      return THREAD_POOL_URL;
    }

    if (getEsVersion(jsonServerInfo) == null) {
      return null;
    }

    if (getEsVersion(jsonServerInfo).startsWith("0.") || getEsVersion(jsonServerInfo).startsWith("1.")
        || getEsVersion(jsonServerInfo).startsWith("2.")) {
      StringBuilder sb = new StringBuilder();
      // SMILE not used here, keep format json
      sb.append("$HOST:$PORT/_cat/thread_pool?format=json&h=nodeId,host,ip,post");
      for (String poolName : POOL_NAMES) {
        for (String fieldName : FIELD_NAMES) {
          sb.append(",").append(poolName).append(".").append(fieldName);
        }
      }

      THREAD_POOL_URL = sb.toString();
    } else {
      THREAD_POOL_URL = THREAD_POOL_V5_URL;
    }

    return THREAD_POOL_URL;
  }

  public static String getClusterName(ServerInfo jsonServerInfo) {
    Map<String, Object> data = (Map<String, Object>) JsonDataSourceCachedFactory
        .getDataSource(jsonServerInfo, getClusterArchitectureDataUrl(jsonServerInfo), false, getClusterArchitectureDataUrl(jsonServerInfo)
            .contains("format=smile")).fetchData();
    return data == null ? null : (String) data.get("cluster_name");
  }

  public static String getNodeName(ServerInfo jsonServerInfo, String nodeId) {
    Map<String, Object> data = (Map<String, Object>) JsonDataSourceCachedFactory
        .getDataSource(jsonServerInfo, getClusterArchitectureDataUrl(jsonServerInfo), false, getClusterArchitectureDataUrl(jsonServerInfo)
            .contains("format=smile")).fetchData();

    if (data == null) {
      return null;
    }

    Map<String, Object> nodeData = JsonUtil.quickNavigateToElement(data, "nodes", nodeId);

    if (nodeData == null) {
      return null;
    }

    return (String) nodeData.get("name");
  }

  public static String getNodeNameFromShortId(ServerInfo jsonServerInfo, String shortNodeId) {
    Map<String, Object> data = (Map<String, Object>) JsonDataSourceCachedFactory
        .getDataSource(jsonServerInfo, getClusterArchitectureDataUrl(jsonServerInfo), false, getClusterArchitectureDataUrl(jsonServerInfo)
            .contains("format=smile")).fetchData();

    if (data == null) {
      return null;
    }

    Map<String, Object> nodeData = JsonUtil.quickNavigateToElement(data, "nodes");

    if (nodeData == null) {
      return null;
    }

    for (String nodeId : nodeData.keySet()) {
      if (nodeId.startsWith(shortNodeId)) {
        return (String) ((Map<String, Object>) nodeData.get(nodeId)).get("name");
      }
    }

    return null;
  }

  /**
   * @return the ID of the node within which monitoring is started
   */
  public static String getLocalNodeId(ServerInfo jsonServerInfo) {
    Map<String, Object> data = (Map<String, Object>) JsonDataSourceCachedFactory
        .getDataSource(jsonServerInfo, getNodesInfoUrl(jsonServerInfo), false, getNodesInfoUrl(jsonServerInfo)
            .contains("format=smile")).fetchData();

    if (data == null) {
      return null;
    }

    Map<String, Object> nodeData = JsonUtil.quickNavigateToElement(data, "nodes");

    if (nodeData == null) {
      return null;
    }

    if (nodeData.keySet().size() == 0) {
      return null;
    }

    if (nodeData.keySet().size() != 1) {
      throw new IllegalStateException("More than 1 local node found! " + nodeData);
    }

    for (String nodeId : nodeData.keySet()) {
      // only one such node should exist, so just return it
      return nodeId;
    }

    return null;
  }

  /**
   * @return the ID of the current master node
   */
  public static String getCurrentMasterNodeId(ServerInfo jsonServerInfo) {
    CachableReliableDataSourceBase<Object, JsonDataProvider> dataSource = JsonDataSourceCachedFactory
        .getDataSource(jsonServerInfo, getClusterTopologyUrl(jsonServerInfo), false,
                       getClusterTopologyUrl(jsonServerInfo)
                           .contains("format=smile"), CustomEsClusterStateJsonHandler.class.getName());

    if (dataSource == null) {
      return null;
    }

    Map<String, Object> data = (Map<String, Object>) dataSource.fetchData();

    if (data == null) {
      return null;
    }

    return (String) data.get("master_node");
  }

  public static boolean isLocalNodeCurrentMasterNode(ServerInfo jsonServerInfo) {
    return isLocalNode(jsonServerInfo, getCurrentMasterNodeId(jsonServerInfo));
  }

  /**
   * Checks whether some node with nodeId is started in the same JVM as local node running the monitor. This
   * is important since we have to collect only stats related to executing JVM.
   * <p/>
   * Accepts only node ID, not node name!
   *
   * @param nodeId
   * @return
   */
  public static boolean isLocalNode(ServerInfo jsonServerInfo, String nodeId) {
    String localNodeId = getLocalNodeId(jsonServerInfo);

    Map<String, Object> data = (Map<String, Object>) JsonDataSourceCachedFactory
        .getDataSource(jsonServerInfo, getClusterArchitectureDataUrl(jsonServerInfo), false,
                       getClusterArchitectureDataUrl(jsonServerInfo).contains("format=smile")).fetchData();

    if (data == null) {
      throw new IllegalStateException("Data is null!");
    }

    Map<String, Object> localNodeData = JsonUtil.quickNavigateToElement(data, "nodes", localNodeId);
    Map<String, Object> nodeData = JsonUtil.quickNavigateToElement(data, "nodes", nodeId);

    if (localNodeData == null) {
      throw new IllegalStateException("Local node data wasn't found!");
    }
    if (nodeData == null) {
      return false;
    }

    boolean checkIpAddress = false;

    String localNodeTransportAddress = (String) localNodeData.get("transport_address");
    String nodeTransportAddress = (String) nodeData.get("transport_address");

    if (localNodeTransportAddress != null && nodeTransportAddress != null) {
      checkIpAddress = extractIp(localNodeTransportAddress).equals(extractIp(nodeTransportAddress));
    } else {
      String localNodeHttpAddress = (String) localNodeData.get("http_address");
      String nodeHttpAddress = (String) nodeData.get("http_address");

      if (localNodeHttpAddress == null || nodeHttpAddress == null) {
        String msg = "Both pairs of local/node transport and http addresses are not completely non-null!" +
            "Transport: Local = " + localNodeTransportAddress + ", Node = " + nodeTransportAddress +
            "HTTP: Local = " + localNodeHttpAddress + ", Node = " + nodeHttpAddress;
        throw new IllegalStateException(msg);
      }

      checkIpAddress = extractIp(localNodeHttpAddress).equals(extractIp(nodeHttpAddress));
    }

    boolean checkJvmPid = true;

    // also check JVM pid to be sure (on ES versions which provide this piece of info)
    if (localNodeData.get("jvm") != null && ((Map<String, Object>) localNodeData.get("jvm")).get("pid") != null) {
      Integer localNodeJvmPid = (Integer) ((Map<String, Object>) localNodeData.get("jvm")).get("pid");
      Integer nodeJvmPid = (Integer) ((Map<String, Object>) nodeData.get("jvm")).get("pid");
      checkJvmPid = localNodeJvmPid.equals(nodeJvmPid);
    }

    return checkIpAddress && checkJvmPid;
  }

  public static List<String> getLocalhostNodeIds(ServerInfo jsonServerInfo) {
    List<String> ids = new FastList<String>();

    String localNodeId = EsClusterInfo.getLocalNodeId(jsonServerInfo);

    if (localNodeId == null) {
      LOG.error("LocalNodeId is null");
      return ids;
    }

    Map<String, Object> data = (Map<String, Object>) JsonDataSourceCachedFactory
        .getDataSource(jsonServerInfo, getListNodesInfoUrl(jsonServerInfo), false,
                       getListNodesInfoUrl(jsonServerInfo).contains("format=smile")).fetchData();

    if (data == null) {
      LOG.error("Data was null for " + getListNodesInfoUrl(jsonServerInfo) + ", no localhostNodeIds");
      return ids;
    }

    Map<String, Map<String, Object>> nodesData = (Map<String, Map<String, Object>>) data.get("nodes");

    // find local node first and its IP
    Map<String, Object> localNode = nodesData.get(localNodeId);

    if (localNode == null) {
      LOG.error("Data " + (data == null ? null : "") + " contains no nodes for localNodeId " + localNodeId);
      return ids;
    }

    String localNodeIp = extractIp((String) localNode.get("transport_address"));

    // find all nodes with that IP (local node as well)
    for (Map.Entry<String, Map<String, Object>> node : nodesData.entrySet()) {
      String nodeIp = extractIp((String) node.getValue().get("transport_address"));
      if (localNodeIp.equals(nodeIp)) {
        ids.add(node.getKey());
      }
    }

    return ids;
  }

  public static List<EsNode> getLocalhostNodes(ServerInfo jsonServerInfo) {
    List<EsNode> ids = new FastList<EsNode>();

    String localNodeId = EsClusterInfo.getLocalNodeId(jsonServerInfo);

    Map<String, Object> data = (Map<String, Object>) JsonDataSourceCachedFactory
        .getDataSource(jsonServerInfo, getListNodesInfoUrl(jsonServerInfo), false,
                       getListNodesInfoUrl(jsonServerInfo).contains("format=smile")).fetchData();
    Map<String, Map<String, Object>> nodesData = (Map<String, Map<String, Object>>) data.get("nodes");

    // find local node first and its IP
    Map<String, Object> localNode = nodesData.get(localNodeId);

    if (localNode == null) {
      return ids;
    }

    String localNodeIp = extractIp((String) localNode.get("transport_address"));

    // find all nodes with that IP (local node as well)
    for (Map.Entry<String, Map<String, Object>> node : nodesData.entrySet()) {
      String nodeIp = extractIp((String) node.getValue().get("transport_address"));
      if (localNodeIp.equals(nodeIp)) {
        EsNode esNode = new EsNode();
        esNode.nodeId = node.getKey();
        esNode.nodeName = (String) node.getValue().get("name");
        ids.add(esNode);
      }
    }

    return ids;
  }

  public static String extractIp(String nodeHttpAddress) {
    return nodeHttpAddress.substring(nodeHttpAddress.indexOf("/") + 1, nodeHttpAddress.indexOf(":"));
  }

  public static String getIndicesJsonRootElement(ServerInfo jsonServerInfo) {
    if (ES_INDICES_JSON_ROOT_ELEMENT.length() != 0) {
      return ES_INDICES_JSON_ROOT_ELEMENT.toString();
    }

    synchronized (getStatsInfoUrl(jsonServerInfo)) {
      if (ES_INDICES_JSON_ROOT_ELEMENT.length() != 0) {
        return ES_INDICES_JSON_ROOT_ELEMENT.toString();
      }

      // althought stats API call, we use async = false because it will run only few times (doesn't have to refresh in
      // the background every X seconds)
      Map<String, Object> data = (Map<String, Object>) JsonDataSourceCachedFactory
          .getDataSource(jsonServerInfo, getStatsInfoUrl(jsonServerInfo), false,
                         getStatsInfoUrl(jsonServerInfo).contains("format=smile"), CustomEsIndicesRootJsonHandler.class
                             .getName()).fetchData();

      if (data == null) {
        return null;
      }

      Map<String, Object> indices = JsonUtil.quickNavigateToElement(data, "_all", "indices");

      if (indices != null) {
        // pre-0.90 version hierarchy
        ES_INDICES_JSON_ROOT_ELEMENT.append("_all:indices");
        return ES_INDICES_JSON_ROOT_ELEMENT.toString();
      } else {
        // es 0.90 has a bit different format, so here is the fallback
        indices = JsonUtil.quickNavigateToElement(data, "indices");

        if (indices != null) {
          ES_INDICES_JSON_ROOT_ELEMENT.append("indices");
          return ES_INDICES_JSON_ROOT_ELEMENT.toString();
        }
      }

      // if none is found, return _all:indices for now, but keep the value unintialized
      return "_all:indices";
    }
  }

  public static String getEsVersion(ServerInfo jsonServerInfo) {
    String esVersion = ES_VERSION_MAP.get(jsonServerInfo.getId());

    if (esVersion != null) {
      return esVersion;
    }

    synchronized (ES_VERSION_MAP) {
      esVersion = ES_VERSION_MAP.get(jsonServerInfo.getId());
      if (esVersion != null) {
        return esVersion;
      }

      Map<String, Object> data = (Map<String, Object>) JsonDataSourceCachedFactory
          .getDataSource(jsonServerInfo, VERSION_INFO_URL, false,
                         VERSION_INFO_URL.contains("format=smile")).fetchData();

      if (data == null) {
        LOG.info("Can't read ES version, got null for jsonData for " + jsonServerInfo.getServer());
        return null;
      }

      Map<String, Object> versionData = (Map<String, Object>) data.get("version");

      if (versionData == null) {
        LOG.info("Can't read ES version, got null for 'version' field of jsonData for " + jsonServerInfo.getServer());
        return null;
      }

      // adjustment for Crate
      if (versionData.get("es_version") != null) {
        // Crate
        esVersion = (String) versionData.get("es_version");
      } else {
        // "real" ES
        esVersion = (String) versionData.get("number");
      }

      LOG.info("Resolved ES version for " + jsonServerInfo.getServer() + " to : " + esVersion);

      ES_VERSION_MAP.put(jsonServerInfo.getId(), esVersion);

      return esVersion;
    }
  }

  public static String getJvmStatsApiUrl() {
    return JVM_STATS_URL;
  }

  public static boolean isDataHostingNode(ServerInfo jsonServerInfo) {
    if (getEsVersion(jsonServerInfo) == null) {
      return false;
    }

    // 0. versions didn't expose nodes handler, so we'll assume all nodes host data
    if (getEsVersion(jsonServerInfo).startsWith("0.")) {
      return true;
    }

    String nodeId = getLocalNodeId(jsonServerInfo);

    if (nodeId == null) {
      return false;
    }

    String nodeName = getNodeName(jsonServerInfo, nodeId);

    if (nodeName == null) {
      return false;
    }

    Object data = JsonDataSourceCachedFactory.getDataSource(jsonServerInfo, NODES_META_INFO, false, false).fetchData();

    if (data != null) {
      List<Map<String, Object>> nodes = (List<Map<String, Object>>) data;

      if (nodes != null) {
        for (Map<String, Object> node : nodes) {
          if (nodeName.equals(node.get("n"))) {
            String nodeIdStart = (String) node.get("id");

            if (nodeId.startsWith(nodeIdStart)) {
              String role = (String) node.get("r");
              LOG.info("Found node " + nodeId + " with role: " + role);

              if (role != null) {
                for (char c: role.toLowerCase().toCharArray()) {
                  if (DATA_HOSTING_ROLES.contains(c)) {
                    return true;
                  }
                }
              }
            }
          }
        }
      }
    }

    return false;
  }

  // currently unused, but left here in case it is needed in the future
  public static boolean anyDataHostingNodesOnTheMachine(ServerInfo jsonServerInfo) {
    if (getEsVersion(jsonServerInfo) == null) {
      return false;
    }

    // 0. versions didn't expose nodes handler, so we'll assume all nodes host data
    if (getEsVersion(jsonServerInfo).startsWith("0.")) {
      return true;
    }

    String nodeId = getLocalNodeId(jsonServerInfo);

    if (nodeId == null) {
      return false;
    }

    String nodeName = getNodeName(jsonServerInfo, nodeId);

    if (nodeName == null) {
      return false;
    }

    Map<String, Object> data = (Map<String, Object>) JsonDataSourceCachedFactory
        .getDataSource(jsonServerInfo, NODES_META_INFO, false, false).fetchData();

    // first find IP of this node
    String ip = null;

    if (data != null) {
      List<Map<String, Object>> nodes = (List<Map<String, Object>>) data.get("root");

      if (nodes != null) {
        for (Map<String, Object> node : nodes) {
          if (nodeName.equals(node.get("n"))) {
            String nodeIdStart = (String) node.get("id");

            if (nodeId.startsWith(nodeIdStart)) {
              ip = (String) node.get("i");
              break;
            }
          }
        }
      }
    }

    if (ip == null) {
      return false;
    }

    if (data != null) {
      List<Map<String, Object>> nodes = (List<Map<String, Object>>) data.get("root");

      if (nodes != null) {
        for (Map<String, Object> node : nodes) {
          if (ip.equals(node.get("i"))) {
            String role = (String) node.get("r");

            if ("d".equalsIgnoreCase(role)) {
              return true;
            }
          }
        }
      }
    }

    return false;
  }
}
