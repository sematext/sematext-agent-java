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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PrometheusTextJsonHandlerTest {
  private static Map<String, Object> parse(String text) throws IOException {
    return new PrometheusTextJsonHandler().parse(new ByteArrayInputStream(text.getBytes("UTF-8")), null);
  }

  private static List<Map<String, Object>> samples(Map<String, Object> data, String name) {
    return (List<Map<String, Object>>) data.get(name);
  }

  @Test
  public void testParsesLabelsAndValue() throws IOException {
    Map<String, Object> data = parse(
        "solr_core_requests_total{category=\"QUERY\",core=\"c1\",handler=\"/select\"} 4.0\n");

    List<Map<String, Object>> requests = samples(data, "solr_core_requests_total");
    Assert.assertEquals(1, requests.size());
    Assert.assertEquals("QUERY", requests.get(0).get("category"));
    Assert.assertEquals("c1", requests.get(0).get("core"));
    Assert.assertEquals("/select", requests.get(0).get("handler"));
    Assert.assertEquals(4L, requests.get(0).get("value"));
  }

  @Test
  public void testGroupsSamplesUnderTheirName() throws IOException {
    Map<String, Object> data = parse(
        "m_total{result=\"hit\"} 2.0\n" +
            "m_total{result=\"miss\"} 1.0\n" +
            "other 7\n");

    Assert.assertEquals(2, data.size());
    Assert.assertEquals(2, samples(data, "m_total").size());
    Assert.assertEquals(7L, samples(data, "other").get(0).get("value"));
  }

  @Test
  public void testKeepsFractionalValuesAsDouble() throws IOException {
    Map<String, Object> data = parse("solr_core_index_size_megabytes{core=\"c1\"} 0.0123\n");

    Assert.assertEquals(0.0123d, samples(data, "solr_core_index_size_megabytes").get(0).get("value"));
  }

  @Test
  public void testNonFiniteValuesStayDouble() throws IOException {
    Map<String, Object> data = parse("a NaN\nb +Inf\nc -Inf\n");

    Assert.assertTrue(Double.isNaN((Double) samples(data, "a").get(0).get("value")));
    Assert.assertEquals(Double.POSITIVE_INFINITY, samples(data, "b").get(0).get("value"));
    Assert.assertEquals(Double.NEGATIVE_INFINITY, samples(data, "c").get(0).get("value"));
  }

  @Test
  public void testUnescapesLabelValues() throws IOException {
    Map<String, Object> data = parse("m{path=\"C:\\\\dir\",msg=\"say \\\"hi\\\"\",multi=\"a\\nb\",brace=\"}\"} 1\n");

    Map<String, Object> sample = samples(data, "m").get(0);
    Assert.assertEquals("C:\\dir", sample.get("path"));
    Assert.assertEquals("say \"hi\"", sample.get("msg"));
    Assert.assertEquals("a\nb", sample.get("multi"));
    Assert.assertEquals("}", sample.get("brace"));
    Assert.assertEquals(1L, sample.get("value"));
  }

  @Test
  public void testIgnoresTimestamp() throws IOException {
    Map<String, Object> data = parse("m{a=\"b\"} 3 1700000000000\n");

    Assert.assertEquals(3L, samples(data, "m").get(0).get("value"));
  }

  @Test
  public void testSkipsCommentsBlankLinesAndEof() throws IOException {
    Map<String, Object> data = parse(
        "# HELP m help text\n" +
            "# TYPE m counter\n" +
            "\n" +
            "m_total 1\n" +
            "# EOF\n");

    Assert.assertEquals(1, data.size());
    Assert.assertEquals(1L, samples(data, "m_total").get(0).get("value"));
  }

  @Test
  public void testSkipsMalformedLines() throws IOException {
    Map<String, Object> data = parse(
        "m{a=\"b\" 1\n" +
            "novalue\n" +
            "bad{a=\"b\"} notanumber\n" +
            "good 2\n");

    Assert.assertEquals(1, data.size());
    Assert.assertEquals(2L, samples(data, "good").get(0).get("value"));
  }

  @Test
  public void testEmptyBodyGivesEmptyMap() throws IOException {
    Assert.assertTrue(parse("").isEmpty());
  }

  private static void assertEachDiscoveredPathMatchesOnce(Map<String, Object> data, String ymlPath, int expected) {
    Collection<JsonMatchingPath> discovered = JsonUtil.findDistinctMatchingPaths(data, ymlPath);
    Assert.assertEquals(ymlPath, expected, discovered.size());
    for (JsonMatchingPath path : discovered) {
      Assert.assertEquals(path.getFullObjectPath(), 1, JsonUtil.findMatchingPaths(data, path.getFullObjectPath()).size());
    }
  }

  @Test
  public void testSolr10YmlPathsSurviveCollectionReMatch() throws IOException {
    Map<String, Object> data = new PrometheusTextJsonHandler().parse(
        getClass().getResourceAsStream("solr10-cloud-metrics.prom"), null);
    String cloud = "@.collection=${collection} && @.shard=${shard} && @.core=${core}";

    assertEachDiscoveredPathMatchesOnce(data, "$.solr_core_indexsearcher_cache_lookups_total[?(" + cloud +
        " && @.name=${cacheName} && @.result=hit)]", 15);
    assertEachDiscoveredPathMatchesOnce(data, "$.solr_core_requests_total[?(" + cloud +
        " && @.category=QUERY && @.handler=${handler} && @.internal=${internal})]", 5);
    assertEachDiscoveredPathMatchesOnce(data, "$.solr_core_requests_times_milliseconds_sum[?(" + cloud +
        " && @.category=UPDATE && @.handler=${handler})]", 3);
    assertEachDiscoveredPathMatchesOnce(data, "$.solr_core_update_commit_ops_total[?(" + cloud +
        " && @.ops=commits)]", 3);
  }

  @Test
  public void testSolr10LabelShapesForEventDrivenSeries() throws IOException {
    // these series only exist after their first event; label names and values from the Solr 10.0.0 source
    Map<String, Object> data = parse(
        "solr_core_indexwriter_merges_total{category=\"INDEX\",core=\"c1\",merge_state=\"started\",merge_type=\"minor\"} 3\n" +
            "solr_core_indexwriter_merges_total{category=\"INDEX\",core=\"c1\",merge_state=\"completed\",merge_type=\"minor\",result=\"success\"} 2\n" +
            "solr_core_indexwriter_merges_total{category=\"INDEX\",core=\"c1\",merge_state=\"completed\",merge_type=\"minor\",result=\"error\"} 1\n" +
            "solr_core_indexwriter_merge_docs_total{category=\"INDEX\",core=\"c1\",merge_op=\"delete\",merge_state=\"completed\",merge_type=\"major\",result=\"success\"} 7\n" +
            "solr_core_update_maintenance_ops_total{category=\"UPDATE\",core=\"c1\",ops=\"rollback\"} 1\n" +
            "solr_core_update_errors_total{category=\"UPDATE\",core=\"c1\"} 4\n" +
            "solr_core_requests_timeout_total{category=\"QUERY\",core=\"c1\",handler=\"/select\",internal=\"false\"} 2\n" +
            "solr_core_requests_errors_total{category=\"QUERY\",core=\"c1\",handler=\"/select\",internal=\"false\",source=\"client\"} 5\n" +
            "solr_core_requests_errors_total{category=\"QUERY\",core=\"c1\",handler=\"/select\",internal=\"false\",source=\"server\"} 1\n");

    assertEachDiscoveredPathMatchesOnce(data, "$.solr_core_indexwriter_merges_total[?(@.core=${core} && " +
        "@.merge_type=minor && @.merge_state=completed && @.result=${result})]", 2);
    assertEachDiscoveredPathMatchesOnce(data, "$.solr_core_indexwriter_merges_total[?(@.core=${core} && " +
        "@.merge_type=${mergeType} && @.merge_state=completed && @.result=error)]", 1);
    assertEachDiscoveredPathMatchesOnce(data, "$.solr_core_indexwriter_merge_docs_total[?(@.core=${core} && " +
        "@.merge_type=major && @.merge_op=delete && @.merge_state=completed && @.result=${result})]", 1);
    assertEachDiscoveredPathMatchesOnce(data, "$.solr_core_update_maintenance_ops_total[?(@.core=${core} && " +
        "@.ops=rollback)]", 1);
    assertEachDiscoveredPathMatchesOnce(data, "$.solr_core_update_errors_total[?(@.core=${core})]", 1);
    assertEachDiscoveredPathMatchesOnce(data, "$.solr_core_requests_timeout_total[?(@.core=${core} && " +
        "@.category=QUERY && @.handler=${handler} && @.internal=${internal})]", 1);
    assertEachDiscoveredPathMatchesOnce(data, "$.solr_core_requests_errors_total[?(@.core=${core} && " +
        "@.category=QUERY && @.handler=${handler} && @.internal=${internal} && @.source=${errorSource})]", 2);
  }

  @Test
  public void testRealSolr10ScrapeMatchesYmlPaths() throws IOException {
    InputStream is = getClass().getResourceAsStream("solr10-cloud-metrics.prom");
    Map<String, Object> data = new PrometheusTextJsonHandler().parse(is, null);

    List<JsonMatchingPath> hits = JsonUtil.findMatchingPaths(data,
        "$.solr_core_indexsearcher_cache_lookups_total[?(@.collection=${collection} && @.shard=${shard} && " +
            "@.core=${core} && @.name=${cacheName} && @.result=hit)]");
    // 3 cores x 5 caches
    Assert.assertEquals(15, hits.size());
    for (JsonMatchingPath path : hits) {
      Assert.assertEquals(4, path.getPathAttributes().size());
    }

    List<JsonMatchingPath> filterHits = JsonUtil.findMatchingPaths(data,
        "$.solr_core_indexsearcher_cache_lookups_total[?(@.core=col1_shard1_replica_n2 && @.name=filterCache && " +
            "@.result=hit)]");
    Assert.assertEquals(1, filterHits.size());
    Assert.assertEquals(2L, JsonUtil.findValueIn("value", filterHits.get(0).getMatchedObject()));

    List<JsonMatchingPath> internalSelects = JsonUtil.findMatchingPaths(data,
        "$.solr_core_requests_times_milliseconds_sum[?(@.core=${core} && @.handler=${handler} && " +
            "@.internal=true)]");
    Assert.assertEquals(2, internalSelects.size());
  }
}
