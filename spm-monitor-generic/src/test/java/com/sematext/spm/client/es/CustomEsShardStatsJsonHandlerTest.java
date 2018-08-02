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
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.json.JsonUtil;

public class CustomEsShardStatsJsonHandlerTest {
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory());

  @Test
  public void testExtractStats1() throws IOException {
    InputStream data = this.getClass().getResourceAsStream("es-stats1.log");

    CustomEsShardStatsJsonHandler handler = new CustomEsShardStatsJsonHandler();

    String localNodeId = "R5X2AdORRmWCEdxybSeZcw";
    List<String> localNodeIds = new ArrayList<String>();
    localNodeIds.add(localNodeId);

    Map<String, Object> json = handler.parse(data, JSON_MAPPER.getFactory(), localNodeIds, "indices");

    Assert.assertEquals("R5X2AdORRmWCEdxybSeZcw", ((Map<String, Object>) JsonUtil
        .findMatchingPaths(json, "$.indices.twitter.shards.0[?(@.routing.node=R5X2AdORRmWCEdxybSeZcw)].routing").get(0).
            getMatchedObject()).get("node"));
    Assert.assertEquals("STARTED", ((Map<String, Object>) JsonUtil
        .findMatchingPaths(json, "$.indices.twitter.shards.0[?(@.routing.node=R5X2AdORRmWCEdxybSeZcw)].routing").get(0).
            getMatchedObject()).get("state"));
    Assert.assertEquals(true, JsonUtil
        .findMatchingPaths(json, "$.indices.twitter.shards.0[?(@.routing.node=7x5cO1j2T_qkMDCBTVu88w)].routing")
        .isEmpty());
  }

  @Test
  public void testInvalidJson() throws IOException {
    CustomEsShardStatsJsonHandler handler = new CustomEsShardStatsJsonHandler();
    String localNodeId = "sUqNKfsElCQ-i3EgO0Iu0wIAt";
    List<String> localNodeIds = new ArrayList<String>();
    localNodeIds.add(localNodeId);

    Assert.assertNull(handler.parse(new ByteArrayInputStream("".getBytes()), JSON_MAPPER
        .getFactory(), localNodeIds, "indices"));
    Assert.assertNull(handler.parse(new ByteArrayInputStream("{}".getBytes()), JSON_MAPPER
        .getFactory(), localNodeIds, "indices"));
  }
}
