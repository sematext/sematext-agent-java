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
import java.util.List;
import java.util.Map;

public class CustomEsClusterStateJsonHandlerTest {
  private static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory());

  @Test
  public void testExtractStats_twoNodesOnSingleHost() throws IOException {
    InputStream data = this.getClass().getResourceAsStream(
        "/com/sematext/spm/client/es/json/extractor/shards/esShardStatsCalculator1.json");

    CustomEsClusterStateJsonHandler handler = new CustomEsClusterStateJsonHandler();
    Map<String, Object> json = handler.parse(data, JSON_MAPPER.getFactory());

    Assert.assertEquals(null, json.get("routing_table"));
    Assert.assertEquals("sematext-es", json.get("cluster_name"));
    Assert.assertEquals("ZnKXutkCQJ-TQRKTCuPEgA", json.get("master_node"));
    Assert.assertNotNull(json.get("routing_nodes"));
    Assert.assertNotNull(((Map<String, Object>) json.get("routing_nodes")).get("nodes"));
    Assert
        .assertEquals(2, ((Map<String, Object>) ((Map<String, Object>) json.get("routing_nodes")).get("nodes")).size());
    Assert.assertEquals(24, ((List) ((Map<String, Object>) ((Map<String, Object>)
        json.get("routing_nodes")).get("nodes")).get("QTuLgeFwSbaGgn6DKjO3zA")).size());
  }

  @Test
  public void testInvalidJson() throws IOException {
    CustomEsClusterStateJsonHandler handler = new CustomEsClusterStateJsonHandler();
    Assert.assertNull(handler.parse(new ByteArrayInputStream("".getBytes()), JSON_MAPPER.getFactory()));

    Assert.assertNull(handler.parse(new ByteArrayInputStream("{}".getBytes()), JSON_MAPPER.getFactory()));
  }
}
