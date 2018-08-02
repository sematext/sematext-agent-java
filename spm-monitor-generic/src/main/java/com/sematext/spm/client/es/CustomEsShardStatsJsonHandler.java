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
package com.sematext.spm.client.es;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.es.info.EsClusterInfo;
import com.sematext.spm.client.http.ServerInfo;
import com.sematext.spm.client.json.CustomJsonHandler;

public class CustomEsShardStatsJsonHandler implements CustomJsonHandler<Map<String, Object>> {
  private static final Log LOG = LogFactory.getLog(CustomEsShardStatsJsonHandler.class);

  private ServerInfo jsonServerInfo;

  @Override
  public Map<String, Object> parse(InputStream inputStream, JsonFactory jsonFactory) throws IOException {
    String jsonIndicesRoot = EsClusterInfo.getIndicesJsonRootElement(jsonServerInfo);

    if (jsonIndicesRoot == null) {
      LOG.error("JsonIndicesRoot element is null, exiting...");
      return null;
    }

    return parse(inputStream, jsonFactory, EsClusterInfo.getLocalhostNodeIds(jsonServerInfo), jsonIndicesRoot);
  }

  protected Map<String, Object> parse(InputStream inputStream, JsonFactory jsonFactory,
                                      List<String> localNodesIds, String jsonIndicesRoot) throws IOException {
    Map<String, Object> res = new UnifiedMap<String, Object>();
    Map<String, Object> indices = new UnifiedMap<String, Object>();

    if (jsonIndicesRoot.startsWith("_all")) {
      Map<String, Object> all = new UnifiedMap<String, Object>();
      res.put("_all", all);
      all.put("indices", indices);
    } else {
      res.put("indices", indices);
    }

    JsonParser jsonParser = jsonFactory.createParser(inputStream);
    TypeReference<UnifiedMap<String, Object>> typeRef = new TypeReference<UnifiedMap<String, Object>>() {
    };

    CustomJsonHandlerUtil.moveToNextObject(jsonParser, true);
    // we are now at _shards

    // check if true
    if (!"_shards".equals(jsonParser.getCurrentName())) {
      // not the format we expected
      LOG.error("Expected _shards field, found: " + jsonParser.getCurrentName() + ", exiting...");
      return null;
    }

    CustomJsonHandlerUtil.navigateToSiblingFieldWithName(jsonParser, "_all");
    // NOTE: again, readValueAsTree + conversion seems much faster than readValue()
    Map<String, Object> allJsonNode = CustomJsonHandlerUtil.convertToMap(jsonParser.readValueAsTree());
    res.put("_all", allJsonNode);

    jsonParser.nextToken();
    CustomJsonHandlerUtil.navigateToSiblingFieldWithName(jsonParser, "indices");

    // children are index names; each index has "primaries", "total" and "shards" children; "shards" has children where
    // key is "shard name", and value is a list of shards (each shard is a map); that shard has "routing" as first
    // child,
    // inside of it is value "node" based on which we can load or not load

    if (jsonParser.getCurrentToken() == JsonToken.END_OBJECT) {
      // handle when there are no indices
      return res;
    }

    CustomJsonHandlerUtil.moveToNextObject(jsonParser, true);
    // now positioned on index object

    while (true) {
      if (jsonParser.getCurrentToken() == JsonToken.FIELD_NAME) {
        String index = jsonParser.getCurrentName();

        Map<String, Object> indexMap = (Map<String, Object>) indices.get(index);
        Map<String, Object> indexShards = null;
        if (indexMap == null) {
          indexMap = new UnifiedMap<String, Object>();
          indices.put(index, indexMap);
          indexShards = new UnifiedMap<String, Object>();
          indexMap.put("shards", indexShards);
        } else {
          indexShards = (Map<String, Object>) indexMap.get("shards");
        }

        // process single index
        jsonParser.nextToken();
        CustomJsonHandlerUtil.moveToNextObject(jsonParser, true);

        // read "primaries" and "total"
        CustomJsonHandlerUtil.navigateToSiblingFieldWithName(jsonParser, "primaries");
        Map<String, Object> primaries = CustomJsonHandlerUtil.convertToMap(jsonParser.readValueAsTree());
        indexMap.put("primaries", primaries);

        jsonParser.nextToken();
        CustomJsonHandlerUtil.navigateToSiblingFieldWithName(jsonParser, "total");
        Map<String, Object> total = CustomJsonHandlerUtil.convertToMap(jsonParser.readValueAsTree());
        indexMap.put("total", total);

        jsonParser.nextToken();
        CustomJsonHandlerUtil.navigateToSiblingFieldWithName(jsonParser, "shards");

        if (jsonParser.getCurrentToken() == JsonToken.END_OBJECT) {
          // handle when there are no shards; we are now positioned at the end of index object
          jsonParser.nextToken();
          continue;
        }

        CustomJsonHandlerUtil.moveToNextObject(jsonParser, true);

        // read all shards of particular index
        while (true) {
          // now should be at shard X, some replica

          // check if true
          if (jsonParser.getCurrentToken() != JsonToken.FIELD_NAME) {
            LOG.error("Expected JsonToken.FIELD_NAME, found: " + jsonParser.getCurrentToken() + ", " +
                          jsonParser.getCurrentName() + ", exiting...");
            return null;
          }

          String shard = jsonParser.getCurrentName();
          List<Object> shardList = (List<Object>) indexShards.get(shard);
          if (shardList == null) {
            shardList = new FastList<Object>();
            indexShards.put(shard, shardList);
          }

          while (true) {
            jsonParser.nextToken();
            // moved to start of array

            // check if true
            if (jsonParser.getCurrentToken() != JsonToken.START_ARRAY && !"routing"
                .equals(jsonParser.getCurrentName())) {
              LOG.error(
                  "Expected JsonToken.START_ARRAY or 'routing' field, found: " + jsonParser.getCurrentToken() + ", " +
                      jsonParser.getCurrentName() + ", exiting...");
              return null;
            }

            CustomJsonHandlerUtil.moveToNextObject(jsonParser, true);
            jsonParser.nextToken();

            // moved to first routing object

            // NOTE: for some reason readValueAsTree is here much faster than readValueAs; we do conversion to Map internally
            // Map<String, Object> routing = jsonParser.readValueAs(typeRef);
            JsonNode routing = jsonParser.readValueAsTree();

            String node = routing.get("node").asText();

            if (localNodesIds.contains(node)) {
              Map<String, Object> shardStats = readShardStats(jsonParser, routing, typeRef);

              // add as object into result
              shardList.add(shardStats);
            } else {
              CustomJsonHandlerUtil.skipAllSiblings(jsonParser);
            }
            // particular replica is done

            if (jsonParser.getCurrentToken() == JsonToken.END_ARRAY) {
              // if we have END_ARRAY, then all replicas of particular shard are done, we can move to next shard
              jsonParser.nextToken();
              break;
            }
          }

          if (jsonParser.getCurrentToken() == JsonToken.END_OBJECT) {
            // if we have END_OBJECT, then all shards of particular index are done, we can move to next index
            jsonParser.nextToken();

            if (jsonParser.getCurrentToken() == JsonToken.END_OBJECT) {
              // parent object (index) is also closed after "shards" object, just skip to next token
              jsonParser.nextToken();
            } else {
              CustomJsonHandlerUtil.skipAllSiblings(jsonParser);
              jsonParser.nextToken();
            }

            break;
          }
        }
      } else {
        break;
      }
    }

    return res;
  }

  private static Map<String, Object> readShardStats(JsonParser jsonParser, JsonNode routing,
                                                    TypeReference<UnifiedMap<String, Object>> typeRef)
      throws IOException {
    jsonParser.nextToken();

    Map<String, Object> siblings = jsonParser.readValueAs(typeRef);
    siblings.put("routing", CustomJsonHandlerUtil.convertToMap(routing));

    // move to next object
    jsonParser.nextToken();

    return siblings;
  }

  @Override
  public void setJsonServerInfo(ServerInfo jsonServerInfo) {
    this.jsonServerInfo = jsonServerInfo;
  }
}
