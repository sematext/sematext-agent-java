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
package com.sematext.spm.client.tracing.agent.tracer.solr4j;

import static com.sematext.spm.client.tracing.utils.TracingTesting.setupSink;
import static java.util.Collections.singletonMap;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.http.HttpResponse;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.DirectXmlRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.util.ContentStreamBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.model.SolrAnnotation;
import com.sematext.spm.client.tracing.agent.tracer.Tracers;
import com.sematext.spm.client.tracing.utils.MockTransactionSink;
import com.sematext.spm.client.tracing.utils.TracingContext;
import com.sematext.spm.client.tracing.utils.TracingJUnit4ClassRunner;
import com.sematext.spm.client.tracing.utils.TracingTesting;

@RunWith(TracingJUnit4ClassRunner.class)
@TracingContext(tracers = { Tracers.Solrj5Tracer.class, Tracers.HttpClient4Tracer.class })
public class Solrj51TracerIntegrationTest {

  private SolrClient getSolrClient() {
    return new HttpSolrClient("http://localhost:8983/solr");
  }

  @Before
  @SuppressWarnings("unchecked")
  public void preStart() throws Exception {
    final SolrClient client = getSolrClient();
    CollectionAdminResponse response = new CollectionAdminRequest.List().process(client);

    for (Object names : response.getResponse().getAll("collections")) {
      for (String name : (List<String>) names) {
        if (name.equals("test-collection-1") || name.equals("test-collection-2")) {
          final CollectionAdminRequest.Delete request = new CollectionAdminRequest.Delete();
          request.setCollectionName(name);
          request.process(client);
        }
      }
    }

    CollectionAdminRequest.Create create1Request = new CollectionAdminRequest.Create();
    create1Request.setCollectionName("test-collection-1");
    create1Request.setNumShards(1);
    create1Request.process(client);

    CollectionAdminRequest.Create create2Request = new CollectionAdminRequest.Create();
    create2Request.setCollectionName("test-collection-2");
    create2Request.setNumShards(1);
    create2Request.process(client);
  }

  private SolrAnnotation testRequestType(SolrRequest<?> request, String collection, SolrAnnotation.RequestType expected)
      throws Exception {
    Tracing.newTrace("test-request-type", Call.TransactionType.BACKGROUND);
    final MockTransactionSink mockTransSink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(mockTransSink);

    try {
      request.process(getSolrClient(), collection);

      assertEquals(1, mockTransSink.getTransactions().size());
      assertEquals(1, mockTransSink.getTransactions().get(0).getCalls().size());

      final Call call = mockTransSink.getTransactions().get(0).getCalls().get(0);
      assertEquals(Call.CallTag.SOLR, call.getCallTag());

      final SolrAnnotation annotation = (SolrAnnotation) call.getAnnotation();
      assertNotNull(annotation);

      assertEquals(expected, annotation.getRequestType());
      assertEquals(200, annotation.getResponseStatus());
      assertTrue(annotation.isSucceed());
      return annotation;
    } finally {
      Tracing.endTrace();
    }
  }

  private static final String XML_DOC = "<add>\n" +
      "<doc>\n" +
      "  <field name=\"id\">SOLR1000</field>\n" +
      "  <field name=\"name\">Solr, the Enterprise Search Server</field>\n" +
      "</doc>\n" +
      "</add>\n" +
      "\n";

  @Test
  public void testRequestTypes() throws Exception {
    SolrAnnotation annotation = testRequestType(new QueryRequest(new MapSolrParams(singletonMap("q", "*.*"))), "test-collection-1", SolrAnnotation.RequestType.QUERY);
    final SolrInputDocument document = new SolrInputDocument();
    document.addField("firstName", "Corey");
    document.addField("lastName", "Taylor");
    annotation = testRequestType(new UpdateRequest()
                                     .add(document), "test-collection-1", SolrAnnotation.RequestType.UPDATE);
    assertEquals("test-collection-1", annotation.getCollection());

    final ContentStreamUpdateRequest contentStreamRequest = new ContentStreamUpdateRequest("/update");
    contentStreamRequest.addContentStream(new ContentStreamBase.StringStream(XML_DOC, "application/xml"));
    annotation = testRequestType(contentStreamRequest, "test-collection-1", SolrAnnotation.RequestType.UPDATE);
    assertEquals("test-collection-1", annotation.getCollection());

    final DirectXmlRequest xmlRequest = new DirectXmlRequest("/update", XML_DOC);
    annotation = testRequestType(xmlRequest, "test-collection-1", SolrAnnotation.RequestType.UPDATE);
    assertEquals("test-collection-1", annotation.getCollection());

    testRequestType(new CollectionAdminRequest.List(), null, SolrAnnotation.RequestType.COLLECTION_ADMIN);
  }

  @Test
  public void testConcurrentUpdate() throws Exception {
    Tracing.newTrace("test-request-type", Call.TransactionType.BACKGROUND);
    final MockTransactionSink mockTransSink = setupSink();

    final CountDownLatch latch = new CountDownLatch(10);

    final ConcurrentUpdateSolrClient client = new ConcurrentUpdateSolrClient("http://localhost:8983/solr/test-collection-1", 2, 10) {
      @Override
      public void onSuccess(HttpResponse resp) {
        latch.countDown();
      }

      @Override
      public void handleError(Throwable ex) {
        ex.printStackTrace();
        latch.countDown();
      }
    };

    for (int i = 0; i < 10; i++) {
      final SolrInputDocument document = new SolrInputDocument();
      document.addField("firstName", "Corey");
      document.addField("lastName", "Taylor");

      new UpdateRequest("/test-collection-1/update").add(document).process(client, "test-collection-1");
    }

    latch.await();

    List<PartialTransaction> async = TracingTesting.findAsync(mockTransSink.getTransactions());
    assertEquals(10, async.size());
  }

}
