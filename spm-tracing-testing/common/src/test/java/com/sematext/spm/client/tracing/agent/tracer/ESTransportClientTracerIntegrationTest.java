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
package com.sematext.spm.client.tracing.agent.tracer;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sematext.spm.client.tracing.Trace;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.InetAddress;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.model.annotation.ESAnnotation;
import com.sematext.spm.client.tracing.utils.MockTransactionSink;
import com.sematext.spm.client.tracing.utils.TracingContext;
import com.sematext.spm.client.tracing.utils.TracingJUnit4ClassRunner;

@RunWith(TracingJUnit4ClassRunner.class)
@TracingContext(tracers = { Tracers.ESTransportClient.class, Tracers.TracedMethodsTracer.class })
public class ESTransportClientTracerIntegrationTest {

  private static final String[] WORDS = {
      "A", "a", "aa", "aal", "aalii", "aam", "Aani", "aardvark", "aardwolf", "Aaron", "Aaronic", "Aaronical",
      "Aaronite",
      "Aaronitic", "Aaru", "Ab", "aba", "Ababdeh", "Ababua", "abac", "abaca", "abacate", "abacay", "abacinate",
      "abacination",
      "abaciscus", "abacist", "aback", "abactinal", "abactinally", "abaction", "abactor", "abaculus", "abacus",
      "Abadite",
      "abaff", "abaft", "abaisance", "abaiser", "abaissed", "abalienate", "abalienation", "abalone", "Abama",
      "abampere",
      "abandon", "abandonable", "abandoned", "abandonedly", "abandonee", "abandoner", "abandonment", "Abanic",
      "Abantes",
      "abaptiston", "Abarambo", "Abaris", "abarthrosis", "abarticular", "abarticulation", "abas", "abase", "abased",
      "abasedly", "abasedness", "abasement", "abaser", "Abasgi", "abash", "abashed", "abashedly", "abashedness",
      "abashless", "abashlessly", "abashment", "abasia", "abasic", "abask", "Abassin", "abastardize", "abatable",
      "abate", "abatement", "abater", "abatis", "abatised", "abaton", "abator", "abattoir", "Abatua", "abature",
      "abave", "abaxial", "abaxile", "abaze", "abb", "Abba", "abbacomes", "abbacy", "Abbadide"
  };

  private Client client;

  @Before
  public void before() throws IOException {
    client = new TransportClient()
        .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

    try {
      client.admin().indices().prepareDelete("tweets", "tweet").execute().actionGet();
    } catch (Exception e) {
      /* pass */
    }

    final BulkRequestBuilder bulkInsert = client.prepareBulk();

    for (int i = 0; i < WORDS.length; i++) {
      final XContentBuilder doc = jsonBuilder()
          .startObject()
          .field("author", "author" + (i % 2))
          .field("tweet", WORDS[i])
          .endObject();
      bulkInsert.add(client.prepareIndex("tweets", "tweet").setSource(doc));
    }

    BulkResponse response = bulkInsert.execute().actionGet();
    System.out.println("BulkInsertRequest hasFailures = " + response.hasFailures());
  }

  @Trace
  public void search() {
    client.prepareSearch("tweets")
        .setQuery(QueryBuilders.termQuery("author", "author0"))
        .addAggregation(AggregationBuilders.terms("termz").field("tweet"))
        .addAggregation(AggregationBuilders.count("count").field("tweet"))
        .execute()
        .actionGet();
  }

  private static List<Call> findMatching(List<PartialTransaction> transactions, String signature) {
    final List<Call> matching = new ArrayList<Call>();
    for (PartialTransaction transaction : transactions) {
      for (Call call : transaction.getCalls()) {
        if (signature == null || call.getSignature().contains(signature)) {
          matching.add(call);
        }
      }
    }
    return matching;
  }

  @Test
  public void testSearch() {
    Tracing.newTrace("es-trace", Call.TransactionType.BACKGROUND);

    final MockTransactionSink mockCallSink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(mockCallSink);

    search();

    assertEquals(2, mockCallSink.getTransactions().size());

    final List<Call> executeCalls = findMatching(mockCallSink.getTransactions(), "SearchRequestBuilder#execute");
    assertEquals(1, executeCalls.size());

    final Call execute = executeCalls.get(0);
    final ESAnnotation es = (ESAnnotation) execute.getAnnotation();

    assertNotNull(es.getActions().get(0));
    assertEquals("tweets", es.getActions().get(0).getIndex());

    assertNotNull(es.getActions().get(0).getQuery());
  }

  @Trace
  public void insert() throws Exception {
    for (int i = 0; i < WORDS.length; i++) {
      XContentBuilder doc = jsonBuilder()
          .startObject()
          .field("author", "author" + i)
          .field("tweet", WORDS[i])
          .endObject();

      client.prepareIndex("tweets", "tweet")
          .setSource(doc).execute().actionGet();
    }
  }

  @Test
  public void testInsert() throws Exception {
    Tracing.newTrace("test-insert", Call.TransactionType.BACKGROUND);

    final MockTransactionSink mockCallSink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(mockCallSink);

    insert();

    assertEquals(WORDS.length + 1, mockCallSink.getTransactions().size());

    final List<Call> executeCalls = findMatching(mockCallSink.getTransactions(), "IndexRequestBuilder#execute");
    assertEquals(WORDS.length, executeCalls.size());

    for (Call call : executeCalls) {
      ESAnnotation annotation = (ESAnnotation) call.getAnnotation();
      assertEquals(new InetAddress("localhost", 9300), annotation.getAddresses().get(0));
    }

    final List<Call> asyncCalls = findMatching(mockCallSink.getTransactions(), "Async");
    assertEquals(WORDS.length, asyncCalls.size());

    for (Call call : asyncCalls) {
      ESAnnotation annotation = (ESAnnotation) call.getAnnotation();
      assertEquals(new InetAddress("localhost", 9300), annotation.getAddresses().get(0));
    }
  }

  @Trace
  public void bulkInsert() throws Exception {
    BulkRequestBuilder bulk = client.prepareBulk();
    for (int i = 0; i < 10; i++) {
      XContentBuilder doc = jsonBuilder().startObject().field("tweet", "tweet").endObject();
      bulk.add(client.prepareIndex("tweets", "tweet").setSource(doc));
    }
    bulk.execute().actionGet();
  }

  @Test
  public void testBulkInsert() throws Exception {
    Tracing.newTrace("/", Call.TransactionType.BACKGROUND);

    final MockTransactionSink sink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(sink);

    bulkInsert();

    assertEquals(2, sink.getTransactions().size());

    List<Call> executeCalls = findMatching(sink.getTransactions(), "BulkRequestBuilder#execute");
    assertEquals(1, executeCalls.size());

    ESAnnotation annotation = (ESAnnotation) executeCalls.get(0).getAnnotation();
    assertEquals(ESAnnotation.RequestType.INDEX_BULK, annotation.getRequestType());
    assertEquals("tweets", annotation.getIndex());
    assertEquals(10, annotation.getActions().size());
  }

  @Trace
  public void bulkDelete() throws Exception {
    BulkRequestBuilder bulk = client.prepareBulk();
    for (int i = 0; i < 10; i++) {
      bulk.add(client.prepareDelete("tweets", "tweet", "10"));
    }
    bulk.execute().actionGet();
  }

  @Test
  public void testBulkDelete() throws Exception {
    Tracing.newTrace("/", Call.TransactionType.BACKGROUND);

    final MockTransactionSink sink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(sink);

    bulkDelete();

    assertEquals(2, sink.getTransactions().size());

    List<Call> executeCalls = findMatching(sink.getTransactions(), "BulkRequestBuilder#execute");
    assertEquals(1, executeCalls.size());

    ESAnnotation annotation = (ESAnnotation) executeCalls.get(0).getAnnotation();
    assertEquals(ESAnnotation.RequestType.DELETE_BULK, annotation.getRequestType());
    assertEquals("tweets", annotation.getIndex());
    assertEquals(10, annotation.getActions().size());
  }

  @Trace
  public void mixedBulk() throws Exception {
    BulkRequestBuilder bulk = client.prepareBulk();
    for (int i = 0; i < 10; i++) {
      bulk.add(client.prepareDelete("tweets", "tweet", "10"));
      XContentBuilder doc = jsonBuilder().startObject().field("tweet", "tweet").endObject();
      bulk.add(client.prepareIndex("tweets", "tweet").setSource(doc));
    }
    bulk.execute().actionGet();
  }

  @Test
  public void testMixedBulk() throws Exception {
    Tracing.newTrace("/", Call.TransactionType.BACKGROUND);

    final MockTransactionSink sink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(sink);

    mixedBulk();

    assertEquals(2, sink.getTransactions().size());

    List<Call> executeCalls = findMatching(sink.getTransactions(), "BulkRequestBuilder#execute");
    assertEquals(1, executeCalls.size());

    ESAnnotation annotation = (ESAnnotation) executeCalls.get(0).getAnnotation();
    assertEquals(ESAnnotation.RequestType.BULK, annotation.getRequestType());
    assertEquals("tweets", annotation.getIndex());
    assertEquals(20, annotation.getActions().size());
  }

}
