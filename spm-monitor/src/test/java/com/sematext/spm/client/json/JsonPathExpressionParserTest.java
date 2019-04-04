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

import org.junit.Assert;
import org.junit.Test;

public class JsonPathExpressionParserTest {
  @Test
  public void testParseNodes() {
    String[] nodes = JsonPathExpressionParser.parseNodes("$.nodes.${nodeId}.jvm.gc.collectors.${gcName}");
    Assert.assertEquals(7, nodes.length);
    Assert.assertEquals("$", nodes[0]);
    Assert.assertEquals("nodes", nodes[1]);
    Assert.assertEquals("${nodeId}", nodes[2]);
    Assert.assertEquals("jvm", nodes[3]);
    Assert.assertEquals("gc", nodes[4]);
    Assert.assertEquals("collectors", nodes[5]);
    Assert.assertEquals("${gcName}", nodes[6]);

    nodes = JsonPathExpressionParser.parseNodes("$.nodes.node\\.1.jvm.gc.collectors.${gcName}");
    Assert.assertEquals(7, nodes.length);
    Assert.assertEquals("$", nodes[0]);
    Assert.assertEquals("nodes", nodes[1]);
    Assert.assertEquals("node.1", nodes[2]);
    Assert.assertEquals("jvm", nodes[3]);
    Assert.assertEquals("gc", nodes[4]);
    Assert.assertEquals("collectors", nodes[5]);
    Assert.assertEquals("${gcName}", nodes[6]);

    nodes = JsonPathExpressionParser.parseNodes("$.upstreams.${nodeName}[?(@.responses.x=${xvalue})].responses");
    Assert.assertEquals(5, nodes.length);
    Assert.assertEquals("$", nodes[0]);
    Assert.assertEquals("upstreams", nodes[1]);
    Assert.assertEquals("${nodeName}", nodes[2]);
    Assert.assertEquals("?(@.responses.x=${xvalue})", nodes[3]);
    Assert.assertEquals("responses", nodes[4]);

    nodes = JsonPathExpressionParser
        .parseNodes("$._all.indices.${indexName}.shards.${shardName}[?(@.routing.node=${nodeId})].docs.count");
    Assert.assertEquals(9, nodes.length);

    nodes = JsonPathExpressionParser.parseNodes("$.upstreams.someNode[?(@.responses.x=${xvalue} && @.responses.y=11)].responses");
    Assert.assertEquals(5, nodes.length);

    nodes = JsonPathExpressionParser.parseNodes("$.upstreams.someNode[?(@.responses.x=${xva\\[lue} && @.responses.y=11)].responses");
    Assert.assertEquals(5, nodes.length);
  }

  @Test
  public void testParseNodes_brackets() {
    String[] nodes = JsonPathExpressionParser.parseNodes("$[nodes][${nodeId}][jvm][gc][collectors][${gcName}]");
    Assert.assertEquals(7, nodes.length);
    Assert.assertEquals("$", nodes[0]);
    Assert.assertEquals("nodes", nodes[1]);
    Assert.assertEquals("${nodeId}", nodes[2]);
    Assert.assertEquals("jvm", nodes[3]);
    Assert.assertEquals("gc", nodes[4]);
    Assert.assertEquals("collectors", nodes[5]);
    Assert.assertEquals("${gcName}", nodes[6]);

    nodes = JsonPathExpressionParser.parseNodes("$[nodes][node\\.1][jvm][gc][collectors][${gcName}]");
    Assert.assertEquals(7, nodes.length);
    Assert.assertEquals("$", nodes[0]);
    Assert.assertEquals("nodes", nodes[1]);
    Assert.assertEquals("node.1", nodes[2]);
    Assert.assertEquals("jvm", nodes[3]);
    Assert.assertEquals("gc", nodes[4]);
    Assert.assertEquals("collectors", nodes[5]);
    Assert.assertEquals("${gcName}", nodes[6]);

    nodes = JsonPathExpressionParser.parseNodes("$[upstreams][${nodeName}][?(@.responses.x=${xvalue})][responses]");
    Assert.assertEquals(5, nodes.length);
    Assert.assertEquals("$", nodes[0]);
    Assert.assertEquals("upstreams", nodes[1]);
    Assert.assertEquals("${nodeName}", nodes[2]);
    Assert.assertEquals("?(@.responses.x=${xvalue})", nodes[3]);
    Assert.assertEquals("responses", nodes[4]);

    nodes = JsonPathExpressionParser
        .parseNodes("$[_all][indices][${indexName}][shardd][${shardName}][?(@.routing.node=${nodeId})][docs][count]");
    Assert.assertEquals(9, nodes.length);

    nodes = JsonPathExpressionParser.parseNodes("$[upstreams][someNode][?(@.responses.x=${xvalue} && @.responses.y=11)][responses]");
    Assert.assertEquals(5, nodes.length);

    nodes = JsonPathExpressionParser.parseNodes("$[upstreams][someNode][?(@.responses.x=${xva\\[lue} && @.responses.y=11)][responses]");
    Assert.assertEquals(5, nodes.length);
  }
  
  @Test
  public void testParseNodes_mixed_dots_brackets() {
    String[] nodes = JsonPathExpressionParser.parseNodes("$.[nodes][${nodeId}][jvm][gc][collectors][${gcName}]");
    Assert.assertEquals(7, nodes.length);
    Assert.assertEquals("$", nodes[0]);
    Assert.assertEquals("nodes", nodes[1]);
    Assert.assertEquals("${nodeId}", nodes[2]);

    nodes = JsonPathExpressionParser.parseNodes("$[nodes][node\\.1].jvm.gc[collectors][${gcName}]");
    Assert.assertEquals(7, nodes.length);
    Assert.assertEquals("$", nodes[0]);
    Assert.assertEquals("nodes", nodes[1]);
    Assert.assertEquals("node.1", nodes[2]);
    Assert.assertEquals("jvm", nodes[3]);
    Assert.assertEquals("gc", nodes[4]);
    Assert.assertEquals("collectors", nodes[5]);
    Assert.assertEquals("${gcName}", nodes[6]);

    nodes = JsonPathExpressionParser.parseNodes("$[upstreams].${nodeName}[?(@.responses.x=${xvalue})].responses");
    Assert.assertEquals(5, nodes.length);
    Assert.assertEquals("$", nodes[0]);
    Assert.assertEquals("upstreams", nodes[1]);
    Assert.assertEquals("${nodeName}", nodes[2]);
    Assert.assertEquals("?(@.responses.x=${xvalue})", nodes[3]);
    Assert.assertEquals("responses", nodes[4]);
  }

  @Test
  public void testExtractExpressionClauses() {
    String[] clauses = JsonPathExpressionParser.extractExpressionClauses("abc||def || ghi|| 1234", "||");
    Assert.assertEquals(4, clauses.length);
    Assert.assertEquals("abc", clauses[0]);
    Assert.assertEquals("def ", clauses[1]);
    Assert.assertEquals(" ghi", clauses[2]);
    Assert.assertEquals(" 1234", clauses[3]);

    clauses = JsonPathExpressionParser.extractExpressionClauses("abc", "||");
    Assert.assertEquals(1, clauses.length);
    Assert.assertEquals("abc", clauses[0]);
  }
}
