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
package com.sematext.spm.client.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsonUtilTest {
  @Test
  public void testFindMatchingPaths_justMap() {
    Map<String, Object> jsonData = new HashMap<String, Object>();
    jsonData.put("upstreams", new HashMap<String, Object>());
    ((Map<String, Object>) jsonData.get("upstreams")).put("someNode", new HashMap<String, Object>());

    Map<String, Object> tmpJsonData = new HashMap<String, Object>();
    tmpJsonData.put("server", "127.0.0.1");
    Map<String, Object> tmpJsonData2 = new HashMap<String, Object>();
    tmpJsonData2.put("x", "20");
    tmpJsonData.put("responses", tmpJsonData2);
    ((Map<String, Object>) ((Map<String, Object>) jsonData.get("upstreams")).get("someNode"))
        .put("serverInfo", tmpJsonData);

    List<JsonMatchingPath> paths = JsonUtil.findMatchingPaths(jsonData, "$.upstreams.${nodeName}.serverInfo");
    Assert.assertEquals(1, paths.size());
    Assert.assertEquals("someNode", paths.get(0).getPathAttributes().get("nodeName"));

    paths = JsonUtil.findMatchingPaths(jsonData, "$.upstreams.${nodeName}.responses");
    Assert.assertEquals(0, paths.size());
  }

  @Test
  public void testFindMatchingPaths_withList() {
    Map<String, Object> jsonData = new HashMap<String, Object>();
    jsonData.put("upstreams", new HashMap<String, Object>());
    ((Map<String, Object>) jsonData.get("upstreams")).put("someNode", new ArrayList<HashMap<String, Object>>());

    Map<String, Object> tmpJsonData = new HashMap<String, Object>();
    tmpJsonData.put("server", "127.0.0.1");
    Map<String, Object> tmpJsonData2 = new HashMap<String, Object>();
    tmpJsonData2.put("x", "20");
    tmpJsonData.put("responses", tmpJsonData2);
    ((List<Map<String, Object>>) ((Map<String, Object>) jsonData.get("upstreams")).get("someNode")).add(tmpJsonData);

    List<JsonMatchingPath> paths = JsonUtil
        .findMatchingPaths(jsonData, "$.upstreams.${nodeName}[?(@.responses.x=${xvalue})].responses");
    Assert.assertEquals(1, paths.size());
    Assert.assertEquals("someNode", paths.get(0).getPathAttributes().get("nodeName"));

    paths = JsonUtil.findMatchingPaths(jsonData, "$.upstreams.someNode[?(@.responses.x=${xvalue})].responses");
    Assert.assertEquals(1, paths.size());
    Assert.assertEquals(1, paths.get(0).getPathAttributes().size());
    Assert.assertEquals("20", paths.get(0).getPathAttributes().get("xvalue"));
  }

  @Test
  public void testFindMatchingPaths_withList2() {
    Map<String, Object> jsonData = new HashMap<String, Object>();
    jsonData.put("upstreams", new HashMap<String, Object>());
    ((Map<String, Object>) jsonData.get("upstreams")).put("someNode", new ArrayList<HashMap<String, Object>>());

    Map<String, Object> tmpJsonData = new HashMap<String, Object>();
    tmpJsonData.put("server", "127.0.0.1");
    Map<String, Object> tmpJsonData2 = new HashMap<String, Object>();
    tmpJsonData2.put("x", "20");
    tmpJsonData2.put("y", "10");
    tmpJsonData.put("responses", tmpJsonData2);
    ((List<Map<String, Object>>) ((Map<String, Object>) jsonData.get("upstreams")).get("someNode")).add(tmpJsonData);

    List<JsonMatchingPath> paths = JsonUtil
        .findMatchingPaths(jsonData, "$.upstreams.${nodeName}[?(@.responses.x=${xvalue})].responses");
    Assert.assertEquals(1, paths.size());
    Assert.assertEquals("someNode", paths.get(0).getPathAttributes().get("nodeName"));

    paths = JsonUtil.findMatchingPaths(jsonData, "$.upstreams.someNode[?(@.responses.x=${xvalue})].responses");
    Assert.assertEquals(1, paths.size());
    Assert.assertEquals(1, paths.get(0).getPathAttributes().size());
    Assert.assertEquals("20", paths.get(0).getPathAttributes().get("xvalue"));

    paths = JsonUtil
        .findMatchingPaths(jsonData, "$.upstreams.${nodeName}[?(@.responses.x=${xvalue} && @.responses.y=10)].responses");
    Assert.assertEquals(1, paths.size());
    Assert.assertEquals("someNode", paths.get(0).getPathAttributes().get("nodeName"));

    paths = JsonUtil
        .findMatchingPaths(jsonData, "$.upstreams.someNode[?(@.responses.x=${xvalue} && @.responses.y=10)].responses");
    Assert.assertEquals(1, paths.size());
    Assert.assertEquals(1, paths.get(0).getPathAttributes().size());
    Assert.assertEquals("20", paths.get(0).getPathAttributes().get("xvalue"));

    paths = JsonUtil
        .findMatchingPaths(jsonData, "$.upstreams.${nodeName}[?(@.responses.x=${xvalue} && @.responses.y=11)].responses");
    Assert.assertEquals(0, paths.size());

    paths = JsonUtil
        .findMatchingPaths(jsonData, "$.upstreams.someNode[?(@.responses.x=${xvalue} && @.responses.y=11)].responses");
    Assert.assertEquals(0, paths.size());

    paths = JsonUtil
        .findMatchingPaths(jsonData, "$.upstreams.someNode[?(@.responses.x=${xvalue} && @.responses.y=10||15)].responses");
    Assert.assertEquals(1, paths.size());
  }

  @Test
  public void testFindMatchingPaths_withList_complex() {
    Map<String, Object> jsonData = new HashMap<String, Object>();
    jsonData.put("_all", new HashMap<String, Object>());
    Map<String, Object> jsonDataIndices = new HashMap<String, Object>();
    ((Map<String, Object>) jsonData.get("_all")).put("indices", jsonDataIndices);
    Map<String, Object> jsonDataIndex1 = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex2 = new HashMap<String, Object>();
    jsonDataIndices.put("index1", jsonDataIndex1);
    jsonDataIndices.put("index2", jsonDataIndex2);
    Map<String, Object> jsonDataIndex1Shards = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex2Shards = new HashMap<String, Object>();
    jsonDataIndex1.put("shards", jsonDataIndex1Shards);
    jsonDataIndex2.put("shards", jsonDataIndex2Shards);
    List<Object> jsonDataIndex1Shard0 = new ArrayList<Object>();
    List<Object> jsonDataIndex1Shard1 = new ArrayList<Object>();
    jsonDataIndex1Shards.put("0", jsonDataIndex1Shard0);
    jsonDataIndex1Shards.put("1", jsonDataIndex1Shard1);
    Map<String, Object> jsonDataIndex1Shard00 = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard01 = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard02 = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard10 = new HashMap<String, Object>();
    jsonDataIndex1Shard0.add(jsonDataIndex1Shard00);
    jsonDataIndex1Shard0.add(jsonDataIndex1Shard01);
    jsonDataIndex1Shard0.add(jsonDataIndex1Shard02);
    jsonDataIndex1Shard1.add(jsonDataIndex1Shard10);
    Map<String, Object> jsonDataIndex1Shard00Routing = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard01Routing = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard02Routing = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard10Routing = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard00Docs = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard01Docs = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard02Docs = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard10Docs = new HashMap<String, Object>();
    jsonDataIndex1Shard00.put("routing", jsonDataIndex1Shard00Routing);
    jsonDataIndex1Shard00.put("docs", jsonDataIndex1Shard00Docs);
    jsonDataIndex1Shard01.put("routing", jsonDataIndex1Shard01Routing);
    jsonDataIndex1Shard01.put("docs", jsonDataIndex1Shard01Docs);
    jsonDataIndex1Shard02.put("routing", jsonDataIndex1Shard02Routing);
    jsonDataIndex1Shard02.put("docs", jsonDataIndex1Shard02Docs);
    jsonDataIndex1Shard10.put("routing", jsonDataIndex1Shard10Routing);
    jsonDataIndex1Shard10.put("docs", jsonDataIndex1Shard10Docs);
    jsonDataIndex1Shard00Routing.put("node", "node1");
    jsonDataIndex1Shard00Routing.put("primary", "true");
    jsonDataIndex1Shard01Routing.put("node", "node2");
    jsonDataIndex1Shard01Routing.put("primary", "false");
    jsonDataIndex1Shard02Routing.put("node", "node3");
    jsonDataIndex1Shard02Routing.put("primary", "false");
    jsonDataIndex1Shard10Routing.put("node", "node1");
    jsonDataIndex1Shard10Routing.put("primary", "true");
    jsonDataIndex1Shard00Docs.put("count", 11);
    jsonDataIndex1Shard01Docs.put("count", 12);
    jsonDataIndex1Shard02Docs.put("count", 13);
    jsonDataIndex1Shard10Docs.put("count", 14);

    List<JsonMatchingPath> paths = JsonUtil.findMatchingPaths(jsonData,
                                                              "$._all.indices.${indexName}.shards.${shardName}[?(@.routing.node=${nodeId})].docs.count");
    Assert.assertEquals(4, paths.size());
    Assert.assertEquals("node1", paths.get(0).getPathAttributes().get("nodeId"));
    Assert.assertEquals("index1", paths.get(0).getPathAttributes().get("indexName"));
    Assert.assertEquals("0", paths.get(0).getPathAttributes().get("shardName"));
    Assert.assertEquals("$._all.indices.index1.shards.0[?(@.routing.node=node1)].docs.count", paths.get(0)
        .getFullObjectPath());
    Assert.assertEquals("node2", paths.get(1).getPathAttributes().get("nodeId"));
    Assert.assertEquals("index1", paths.get(1).getPathAttributes().get("indexName"));
    Assert.assertEquals("0", paths.get(1).getPathAttributes().get("shardName"));
    Assert.assertEquals("$._all.indices.index1.shards.0[?(@.routing.node=node2)].docs.count", paths.get(1)
        .getFullObjectPath());
    Assert.assertEquals("node3", paths.get(2).getPathAttributes().get("nodeId"));
    Assert.assertEquals("index1", paths.get(2).getPathAttributes().get("indexName"));
    Assert.assertEquals("0", paths.get(2).getPathAttributes().get("shardName"));
    Assert.assertEquals("$._all.indices.index1.shards.0[?(@.routing.node=node3)].docs.count", paths.get(2)
        .getFullObjectPath());
    Assert.assertEquals("node1", paths.get(3).getPathAttributes().get("nodeId"));
    Assert.assertEquals("index1", paths.get(3).getPathAttributes().get("indexName"));
    Assert.assertEquals("1", paths.get(3).getPathAttributes().get("shardName"));
    Assert.assertEquals("$._all.indices.index1.shards.1[?(@.routing.node=node1)].docs.count", paths.get(3)
        .getFullObjectPath());

    // also test extracting based on matched paths values
    Assert.assertEquals(11, (JsonUtil.findMatchingPaths(jsonData, paths.get(0).getFullObjectPath())).get(0)
        .getMatchedObject());
    Assert.assertEquals(12, (JsonUtil.findMatchingPaths(jsonData, paths.get(1).getFullObjectPath())).get(0)
        .getMatchedObject());
    Assert.assertEquals(13, (JsonUtil.findMatchingPaths(jsonData, paths.get(2).getFullObjectPath())).get(0)
        .getMatchedObject());
    Assert.assertEquals(14, (JsonUtil.findMatchingPaths(jsonData, paths.get(3).getFullObjectPath())).get(0)
        .getMatchedObject());

    // now remove one "count" value so one of the paths doesn't match anymore
    jsonDataIndex1Shard01Docs.remove("count");
    paths = JsonUtil.findMatchingPaths(jsonData,
                                       "$._all.indices.${indexName}.shards.${shardName}[?(@.routing.node=${nodeId})].docs.count");
    Assert.assertEquals(3, paths.size());
    Assert.assertEquals("node1", paths.get(0).getPathAttributes().get("nodeId"));
    Assert.assertEquals("node3", paths.get(1).getPathAttributes().get("nodeId"));
    Assert.assertEquals("node1", paths.get(2).getPathAttributes().get("nodeId"));
  }

  @Test
  public void testFindMatchingPaths_withList_complex_withSpecialChars() {
    Map<String, Object> jsonData = new HashMap<String, Object>();
    jsonData.put("_all", new HashMap<String, Object>());
    Map<String, Object> jsonDataIndices = new HashMap<String, Object>();
    ((Map<String, Object>) jsonData.get("_all")).put("indices", jsonDataIndices);
    Map<String, Object> jsonDataIndex1 = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex2 = new HashMap<String, Object>();
    jsonDataIndices.put("index.1", jsonDataIndex1);
    jsonDataIndices.put("index.2", jsonDataIndex2);
    Map<String, Object> jsonDataIndex1Shards = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex2Shards = new HashMap<String, Object>();
    jsonDataIndex1.put("shards", jsonDataIndex1Shards);
    jsonDataIndex2.put("shards", jsonDataIndex2Shards);
    List<Object> jsonDataIndex1Shard0 = new ArrayList<Object>();
    List<Object> jsonDataIndex1Shard1 = new ArrayList<Object>();
    jsonDataIndex1Shards.put("0", jsonDataIndex1Shard0);
    jsonDataIndex1Shards.put("1", jsonDataIndex1Shard1);
    Map<String, Object> jsonDataIndex1Shard00 = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard01 = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard02 = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard10 = new HashMap<String, Object>();
    jsonDataIndex1Shard0.add(jsonDataIndex1Shard00);
    jsonDataIndex1Shard0.add(jsonDataIndex1Shard01);
    jsonDataIndex1Shard0.add(jsonDataIndex1Shard02);
    jsonDataIndex1Shard1.add(jsonDataIndex1Shard10);
    Map<String, Object> jsonDataIndex1Shard00Routing = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard01Routing = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard02Routing = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard10Routing = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard00Docs = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard01Docs = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard02Docs = new HashMap<String, Object>();
    Map<String, Object> jsonDataIndex1Shard10Docs = new HashMap<String, Object>();
    jsonDataIndex1Shard00.put("routing", jsonDataIndex1Shard00Routing);
    jsonDataIndex1Shard00.put("docs", jsonDataIndex1Shard00Docs);
    jsonDataIndex1Shard01.put("routing", jsonDataIndex1Shard01Routing);
    jsonDataIndex1Shard01.put("docs", jsonDataIndex1Shard01Docs);
    jsonDataIndex1Shard02.put("routing", jsonDataIndex1Shard02Routing);
    jsonDataIndex1Shard02.put("docs", jsonDataIndex1Shard02Docs);
    jsonDataIndex1Shard10.put("routing", jsonDataIndex1Shard10Routing);
    jsonDataIndex1Shard10.put("docs", jsonDataIndex1Shard10Docs);
    jsonDataIndex1Shard00Routing.put("node", "node.1");
    jsonDataIndex1Shard00Routing.put("primary", "true");
    jsonDataIndex1Shard01Routing.put("node", "node.2");
    jsonDataIndex1Shard01Routing.put("primary", "false");
    jsonDataIndex1Shard02Routing.put("node", "node.3");
    jsonDataIndex1Shard02Routing.put("primary", "false");
    jsonDataIndex1Shard10Routing.put("node", "node.1");
    jsonDataIndex1Shard10Routing.put("primary", "true");
    jsonDataIndex1Shard00Docs.put("count", 11);
    jsonDataIndex1Shard01Docs.put("count", 12);
    jsonDataIndex1Shard02Docs.put("count", 13);
    jsonDataIndex1Shard10Docs.put("count", 14);

    List<JsonMatchingPath> paths = JsonUtil.findMatchingPaths(jsonData,
                                                              "$._all.indices.${indexName}.shards.${shardName}[?(@.routing.node=${nodeId})].docs.count");
    Assert.assertEquals(4, paths.size());
    Assert.assertEquals("node.1", paths.get(0).getPathAttributes().get("nodeId"));
    Assert.assertEquals("index.1", paths.get(0).getPathAttributes().get("indexName"));
    Assert.assertEquals("0", paths.get(0).getPathAttributes().get("shardName"));
    Assert.assertEquals("$._all.indices.index\\.1.shards.0[?(@.routing.node=node.1)].docs.count", paths.get(0)
        .getFullObjectPath());
    Assert.assertEquals("node.2", paths.get(1).getPathAttributes().get("nodeId"));
    Assert.assertEquals("index.1", paths.get(1).getPathAttributes().get("indexName"));
    Assert.assertEquals("0", paths.get(1).getPathAttributes().get("shardName"));
    Assert.assertEquals("$._all.indices.index\\.1.shards.0[?(@.routing.node=node.2)].docs.count", paths.get(1)
        .getFullObjectPath());
    Assert.assertEquals("node.3", paths.get(2).getPathAttributes().get("nodeId"));
    Assert.assertEquals("index.1", paths.get(2).getPathAttributes().get("indexName"));
    Assert.assertEquals("0", paths.get(2).getPathAttributes().get("shardName"));
    Assert.assertEquals("$._all.indices.index\\.1.shards.0[?(@.routing.node=node.3)].docs.count", paths.get(2)
        .getFullObjectPath());
    Assert.assertEquals("node.1", paths.get(3).getPathAttributes().get("nodeId"));
    Assert.assertEquals("index.1", paths.get(3).getPathAttributes().get("indexName"));
    Assert.assertEquals("1", paths.get(3).getPathAttributes().get("shardName"));
    Assert.assertEquals("$._all.indices.index\\.1.shards.1[?(@.routing.node=node.1)].docs.count", paths.get(3)
        .getFullObjectPath());

    // also test extracting based on matched paths values
    Assert.assertEquals(11, (JsonUtil.findMatchingPaths(jsonData, paths.get(0).getFullObjectPath())).get(0)
        .getMatchedObject());
    Assert.assertEquals(12, (JsonUtil.findMatchingPaths(jsonData, paths.get(1).getFullObjectPath())).get(0)
        .getMatchedObject());
    Assert.assertEquals(13, (JsonUtil.findMatchingPaths(jsonData, paths.get(2).getFullObjectPath())).get(0)
        .getMatchedObject());
    Assert.assertEquals(14, (JsonUtil.findMatchingPaths(jsonData, paths.get(3).getFullObjectPath())).get(0)
        .getMatchedObject());

    // now remove one "count" value so one of the paths doesn't match anymore
    jsonDataIndex1Shard01Docs.remove("count");
    paths = JsonUtil.findMatchingPaths(jsonData,
                                       "$._all.indices.${indexName}.shards.${shardName}[?(@.routing.node=${nodeId})].docs.count");
    Assert.assertEquals(3, paths.size());
    Assert.assertEquals("node.1", paths.get(0).getPathAttributes().get("nodeId"));
    Assert.assertEquals("node.3", paths.get(1).getPathAttributes().get("nodeId"));
    Assert.assertEquals("node.1", paths.get(2).getPathAttributes().get("nodeId"));
  }

  @Test
  public void testFindMatchingPaths_lastLeafTag() {
    Map<String, Object> jsonData = new HashMap<String, Object>();
    Map<String, Object> first = new HashMap<String, Object>();
    jsonData.put("first", first);
    Map<String, Object> collector1 = new HashMap<String, Object>();
    collector1.put("a", 1);
    collector1.put("b", 2);
    Map<String, Object> collector2 = new HashMap<String, Object>();
    collector1.put("a", 3);
    collector1.put("b", 4);
    first.put("collector1", collector1);
    first.put("collector2", collector2);

    List<JsonMatchingPath> paths = JsonUtil.findMatchingPaths(jsonData, "$.first.${collectorId}");
    Assert.assertEquals(2, paths.size());
    Assert.assertEquals("collector1", paths.get(1).getPathAttributes().get("collectorId"));
    Assert.assertEquals("collector2", paths.get(0).getPathAttributes().get("collectorId"));
  }

  @Test
  public void testFindMatchingPaths_lastLeafTag2() throws JsonParseException, JsonMappingException, IOException {
    InputStream response = getClass().getResourceAsStream("es-jvm.json");
    TypeReference<UnifiedMap<String, Object>> typeRef = new TypeReference<UnifiedMap<String, Object>>() {
    };
    Map<String, Object> jsonData = new ObjectMapper(new JsonFactory()).readValue(response, typeRef);

    List<JsonMatchingPath> paths = JsonUtil
        .findMatchingPaths(jsonData, "$.nodes.${nodeId}.jvm.gc.collectors.${gcName}");
    Assert.assertEquals(2, paths.size());
    Assert.assertEquals("old", paths.get(1).getPathAttributes().get("gcName"));
    Assert.assertEquals("young", paths.get(0).getPathAttributes().get("gcName"));

    // also test extracting based on matched paths values
    Assert
        .assertEquals(8, ((Map<String, Object>) (JsonUtil.findMatchingPaths(jsonData, paths.get(0).getFullObjectPath()))
            .get(0).getMatchedObject()).get("collection_count"));
    Assert
        .assertEquals(1, ((Map<String, Object>) (JsonUtil.findMatchingPaths(jsonData, paths.get(1).getFullObjectPath()))
            .get(0).getMatchedObject()).get("collection_count"));
  }

  @Test
  public void testFindMatchingPaths_esClusterHealth() throws JsonParseException, JsonMappingException, IOException {
    InputStream response = getClass().getResourceAsStream("es-clusterHealth.json");
    TypeReference<UnifiedMap<String, Object>> typeRef = new TypeReference<UnifiedMap<String, Object>>() {
    };
    Map<String, Object> jsonData = new ObjectMapper(new JsonFactory()).readValue(response, typeRef);

    List<JsonMatchingPath> paths = JsonUtil.findMatchingPaths(jsonData, "$.");
    Assert.assertEquals(1, paths.size());
    Assert.assertEquals("elasticsearch", ((Map<String, Object>) paths.get(0).getMatchedObject()).get("cluster_name"));
  }

  @Test
  public void testParseNodes() {
    String[] nodes = JsonUtil.parseNodes("$.nodes.${nodeId}.jvm.gc.collectors.${gcName}");
    Assert.assertEquals(7, nodes.length);
    Assert.assertEquals("$", nodes[0]);
    Assert.assertEquals("nodes", nodes[1]);
    Assert.assertEquals("${nodeId}", nodes[2]);
    Assert.assertEquals("jvm", nodes[3]);
    Assert.assertEquals("gc", nodes[4]);
    Assert.assertEquals("collectors", nodes[5]);
    Assert.assertEquals("${gcName}", nodes[6]);

    nodes = JsonUtil.parseNodes("$.nodes.node\\.1.jvm.gc.collectors.${gcName}");
    Assert.assertEquals(7, nodes.length);
    Assert.assertEquals("$", nodes[0]);
    Assert.assertEquals("nodes", nodes[1]);
    Assert.assertEquals("node.1", nodes[2]);
    Assert.assertEquals("jvm", nodes[3]);
    Assert.assertEquals("gc", nodes[4]);
    Assert.assertEquals("collectors", nodes[5]);
    Assert.assertEquals("${gcName}", nodes[6]);

    nodes = JsonUtil.parseNodes("$.upstreams.${nodeName}[?(@.responses.x=${xvalue})].responses");
    Assert.assertEquals(4, nodes.length);
    Assert.assertEquals("$", nodes[0]);
    Assert.assertEquals("upstreams", nodes[1]);
    Assert.assertEquals("${nodeName}[?(@.responses.x=${xvalue})]", nodes[2]);
    Assert.assertEquals("responses", nodes[3]);

    nodes = JsonUtil
        .parseNodes("$._all.indices.${indexName}.shards.${shardName}[?(@.routing.node=${nodeId})].docs.count");
    Assert.assertEquals(8, nodes.length);

    nodes = JsonUtil.parseNodes("$.upstreams.someNode[?(@.responses.x=${xvalue} && @.responses.y=11)].responses");
    Assert.assertEquals(4, nodes.length);

    nodes = JsonUtil.parseNodes("$.upstreams.someNode[?(@.responses.x=${xva[lue} && @.responses.y=1]1)].responses");
    Assert.assertEquals(4, nodes.length);
  }

  @Test
  public void testFindMatchingPaths_esIndexStats() throws JsonParseException, JsonMappingException, IOException {
    InputStream response = getClass().getResourceAsStream("es-indexStats.json");
    TypeReference<UnifiedMap<String, Object>> typeRef = new TypeReference<UnifiedMap<String, Object>>() {
    };
    Map<String, Object> jsonData = new ObjectMapper(new JsonFactory()).readValue(response, typeRef);

    List<JsonMatchingPath> paths = JsonUtil
        .findMatchingPaths(jsonData, "$.indices.${indexName}.shards.${shard}[?(@.routing.node=${nodeId} && @.routing.primary=true)].merges");
    Assert.assertEquals(5, paths.size());
    // TODO also test extracting based on matched paths values
  }

  @Test
  public void testFindMatchingPaths_esShardStats() throws JsonParseException, JsonMappingException, IOException {
    InputStream response = getClass().getResourceAsStream("es-shards.json");
    TypeReference<UnifiedMap<String, Object>> typeRef = new TypeReference<UnifiedMap<String, Object>>() {
    };
    Map<String, Object> jsonData = new ObjectMapper(new JsonFactory()).readValue(response, typeRef);

    List<JsonMatchingPath> paths = JsonUtil
        .findMatchingPaths(jsonData, "$.routing_table.indices.${indexName}.shards.${shard}[?(@.node=${nodeId} && @.primary=true)]");
    Assert.assertEquals(10, paths.size());
    paths = JsonUtil
        .findMatchingPaths(jsonData, "$.routing_table.indices.${indexName}.shards.${shard}[?(@.node=${nodeId} && @.primary=true && @.state=STARTED)]");
    Assert.assertEquals(10, paths.size());
    paths = JsonUtil
        .findMatchingPaths(jsonData, "$.routing_table.indices.${indexName}.shards.${shard}[?(@.node=${nodeId} && @.primary=false)]");
    Assert.assertEquals(10, paths.size());
    paths = JsonUtil
        .findMatchingPaths(jsonData, "$.routing_table.indices.${indexName}.shards.${shard}[?(@.node=${nodeId} && @.primary=false && @.state=STARTED)]");
    Assert.assertEquals(0, paths.size());
    paths = JsonUtil
        .findMatchingPaths(jsonData, "$.routing_table.indices.${indexName}.shards.${shard}[?(@.node=${nodeId})]");
    Assert.assertEquals(20, paths
        .size()); // there are 20 shards in the cluster, 10 of them unassigned, all visible in routing_table

    paths = JsonUtil
        .findMatchingPaths(jsonData, "$.routing_nodes.nodes.${nodeId}[?(@.primary=true && @.index=${indexName})]");
    Assert.assertEquals(10, paths.size());
    paths = JsonUtil
        .findMatchingPaths(jsonData, "$.routing_nodes.nodes.${nodeId}[?(@.state=STARTED && @.primary=true && @.index=${indexName})]");
    Assert.assertEquals(10, paths.size());
    paths = JsonUtil
        .findMatchingPaths(jsonData, "$.routing_nodes.nodes.${nodeId}[?(@.primary=false && @.index=${indexName})]");
    Assert.assertEquals(0, paths.size());
    paths = JsonUtil
        .findMatchingPaths(jsonData, "$.routing_nodes.nodes.${nodeId}[?(@.state=STARTED && @.primary=false && @.index=${indexName})]");
    Assert.assertEquals(0, paths.size());
    paths = JsonUtil.findMatchingPaths(jsonData, "$.routing_nodes.nodes.${nodeId}[?(@.index=${indexName})]");
    Assert.assertEquals(10, paths.size()); // routing_nodes.nodes can't show unassigned shards
  }

  @Test
  public void testFindDistinctMatchingPaths_esShardStats()
      throws JsonParseException, JsonMappingException, IOException {
    InputStream response = getClass().getResourceAsStream("es-shards.json");
    TypeReference<UnifiedMap<String, Object>> typeRef = new TypeReference<UnifiedMap<String, Object>>() {
    };
    Map<String, Object> jsonData = new ObjectMapper(new JsonFactory()).readValue(response, typeRef);

    Collection<JsonMatchingPath> paths = JsonUtil
        .findDistinctMatchingPaths(jsonData, "$.routing_table.indices.${indexName}.shards.${shard}[?(@.node=${nodeId} && @.primary=true)]");
    Assert.assertEquals(10, paths.size());
    paths = JsonUtil
        .findDistinctMatchingPaths(jsonData, "$.routing_table.indices.${indexName}.shards.${shard}[?(@.node=${nodeId} && @.primary=true && @.state=STARTED)]");
    Assert.assertEquals(10, paths.size());
    paths = JsonUtil
        .findDistinctMatchingPaths(jsonData, "$.routing_table.indices.${indexName}.shards.${shard}[?(@.node=${nodeId} && @.primary=false)]");
    Assert.assertEquals(10, paths.size());
    paths = JsonUtil
        .findDistinctMatchingPaths(jsonData, "$.routing_table.indices.${indexName}.shards.${shard}[?(@.node=${nodeId} && @.primary=false && @.state=STARTED)]");
    Assert.assertEquals(0, paths.size());
    paths = JsonUtil
        .findDistinctMatchingPaths(jsonData, "$.routing_table.indices.${indexName}.shards.${shard}[?(@.node=${nodeId})]");
    Assert.assertEquals(20, paths
        .size()); // there are 20 shards in the cluster, 10 of them unassigned, all visible in routing_table

    paths = JsonUtil
        .findDistinctMatchingPaths(jsonData, "$.routing_nodes.nodes.${nodeId}[?(@.primary=true && @.index=${indexName})]");
    Assert.assertEquals(2, paths.size());
    paths = JsonUtil
        .findDistinctMatchingPaths(jsonData, "$.routing_nodes.nodes.${nodeId}[?(@.state=STARTED && @.primary=true && @.index=${indexName})]");
    Assert.assertEquals(2, paths.size());
    paths = JsonUtil
        .findDistinctMatchingPaths(jsonData, "$.routing_nodes.nodes.${nodeId}[?(@.primary=false && @.index=${indexName})]");
    Assert.assertEquals(0, paths.size());
    paths = JsonUtil
        .findDistinctMatchingPaths(jsonData, "$.routing_nodes.nodes.${nodeId}[?(@.state=STARTED && @.primary=false && @.index=${indexName})]");
    Assert.assertEquals(0, paths.size());
    paths = JsonUtil.findDistinctMatchingPaths(jsonData, "$.routing_nodes.nodes.${nodeId}[?(@.index=${indexName})]");
    Assert.assertEquals(2, paths.size()); // routing_nodes.nodes can't show unassigned shards
  }

  @Test
  public void testFindDistinctMatchingPaths_listResponse_threadPools()
      throws JsonParseException, JsonMappingException, IOException {
    InputStream response = getClass().getResourceAsStream("es-threadpools.json");
    TypeReference<Object> typeRef = new TypeReference<Object>() {
    };
    Object jsonData = new ObjectMapper(new JsonFactory()).readValue(response, typeRef);

    Collection<JsonMatchingPath> paths = JsonUtil.findDistinctMatchingPaths(jsonData, "$.[?(@.name=${threadPool})]");
    Assert.assertEquals(14, paths.size());

    Iterator<JsonMatchingPath> iter = paths.iterator();
    List<String> threadPools = Arrays.asList("bulk", "fetch_shard_started", "fetch_shard_store", "flush", "force_merge",
                                             "generic", "get", "index", "listener", "management", "refresh", "search", "snapshot", "warmer");
    while (iter.hasNext()) {
      JsonMatchingPath path = iter.next();
      Assert.assertEquals(true, threadPools.contains(path.getPathAttributes().get("threadPool")));
    }
  }

  @Test
  public void testFindMatchingPaths_withDots() throws IOException {
    InputStream response = getClass().getResourceAsStream("nginx-plus.json");
    TypeReference<UnifiedMap<String, Object>> typeRef = new TypeReference<UnifiedMap<String, Object>>() {
    };
    Map<String, Object> jsonData = new ObjectMapper(new JsonFactory()).readValue(response, typeRef);

    List<JsonMatchingPath> paths = JsonUtil.findMatchingPaths(jsonData, "$.server_zones.hg\\.nginx\\.org.${data}");
    Assert.assertEquals(6, paths.size());
    Assert.assertEquals("processing", paths.get(0).getPathAttributes().get("data"));
    Assert.assertEquals("requests", paths.get(1).getPathAttributes().get("data"));
    Assert.assertEquals("responses", paths.get(2).getPathAttributes().get("data"));
    Assert.assertEquals("discarded", paths.get(3).getPathAttributes().get("data"));
    Assert.assertEquals("received", paths.get(4).getPathAttributes().get("data"));
    Assert.assertEquals("sent", paths.get(5).getPathAttributes().get("data"));
  }

  @Test
  public void testFindMatchingPaths_withDots2() throws IOException {
    InputStream response = getClass().getResourceAsStream("nginx-plus.json");
    TypeReference<UnifiedMap<String, Object>> typeRef = new TypeReference<UnifiedMap<String, Object>>() {
    };
    Map<String, Object> jsonData = new ObjectMapper(new JsonFactory()).readValue(response, typeRef);

    List<JsonMatchingPath> paths = JsonUtil.findMatchingPaths(jsonData, "$.server_zones.${zone}");
    Assert.assertEquals(3, paths.size());

    paths = JsonUtil.findMatchingPaths(jsonData, "$.server_zones.hg\\.nginx\\.org");
    Assert.assertEquals(1, paths.size());

    paths = JsonUtil.findMatchingPaths(jsonData, "$.server_zones.hg\\.nginx\\.org.processing");
    Assert.assertEquals(1, paths.size());
  }

  @Test
  public void testExtractClauses() {
    String[] clauses = JsonUtil.extractNodes("abc||def || ghi|| 1234", "||");
    Assert.assertEquals(4, clauses.length);
    Assert.assertEquals("abc", clauses[0]);
    Assert.assertEquals("def ", clauses[1]);
    Assert.assertEquals(" ghi", clauses[2]);
    Assert.assertEquals(" 1234", clauses[3]);

    clauses = JsonUtil.extractNodes("abc", "||");
    Assert.assertEquals(1, clauses.length);
    Assert.assertEquals("abc", clauses[0]);
  }
}
