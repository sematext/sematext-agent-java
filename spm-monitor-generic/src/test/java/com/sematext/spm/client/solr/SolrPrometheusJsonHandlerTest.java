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
package com.sematext.spm.client.solr;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.json.JsonMatchingPath;
import com.sematext.spm.client.json.JsonUtil;

public class SolrPrometheusJsonHandlerTest {
  private static final String SCRAPE =
      "solr_core_requests_total{category=\"QUERY\",core=\"c1\",handler=\"/select\",internal=\"true\"} 4\n" +
          "solr_core_requests_total{category=\"QUERY\",core=\"c1\",handler=\"/stream\"} 5\n" +
          "solr_core_requests_total{category=\"UPDATE\",core=\"c1\",handler=\"/update\"} 6\n";

  private static Map<String, Object> parse() throws IOException {
    return new SolrPrometheusJsonHandler().parse(new ByteArrayInputStream(SCRAPE.getBytes("UTF-8")), null);
  }

  @Test
  public void testQueryHandlersWithoutInternalLabelAreClientRequests() throws IOException {
    List<Map<String, Object>> requests = (List<Map<String, Object>>) parse().get("solr_core_requests_total");

    Assert.assertEquals("true", requests.get(0).get("internal"));
    Assert.assertEquals("false", requests.get(1).get("internal"));
    Assert.assertNull(requests.get(2).get("internal"));
  }

  @Test
  public void testStreamHandlerSurvivesCollectionReMatch() throws IOException {
    Map<String, Object> data = parse();
    Collection<JsonMatchingPath> discovered = JsonUtil.findDistinctMatchingPaths(data,
        "$.solr_core_requests_total[?(@.core=${core} && @.category=QUERY && @.handler=${handler} && " +
            "@.internal=${internal})]");

    Assert.assertEquals(2, discovered.size());
    for (JsonMatchingPath path : discovered) {
      Assert.assertEquals(path.getFullObjectPath(), 1,
                          JsonUtil.findMatchingPaths(data, path.getFullObjectPath()).size());
    }
  }
}
