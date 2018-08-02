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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import com.sematext.spm.client.tracing.Trace;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.httpclient3.HttpClientAccess;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.HttpHeaders;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.model.annotation.HTTPRequestAnnotation;
import com.sematext.spm.client.tracing.agent.solrj5.SolrHttpClient;
import com.sematext.spm.client.tracing.agent.util.Hostname;
import com.sematext.spm.client.tracing.utils.MockTransactionSink;
import com.sematext.spm.client.tracing.utils.TracingContext;
import com.sematext.spm.client.tracing.utils.TracingJUnit4ClassRunner;

@RunWith(TracingJUnit4ClassRunner.class)
@TracingContext(tracers = { Tracers.TracedMethodsTracer.class, Tracers.HttpClient3Tracer.class })
public class HttpClient3TracerTest {

  private HttpServer server;
  private int port;

  @Before
  public void before() throws Exception {
    port = 8080;
    while (true) {
      try {
        ServerSocket socket = new ServerSocket(port);
        socket.close();
        break;
      } catch (BindException e) {
        port++;
      }
    }

    System.out.println("Started on port " + port);

    server = HttpServer.create();
    server.createContext("/ping", new HttpHandler() {
      @Override
      public void handle(HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(200, 4);
        httpExchange.getResponseBody().write("pong".getBytes());
        httpExchange.getResponseBody().close();
        httpExchange.close();
      }
    });
    server.createContext("/not-found", new HttpHandler() {
      @Override
      public void handle(HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(404, 0);
        httpExchange.close();
      }
    });
    server.createContext("/cross-app", new HttpHandler() {
      @Override
      public void handle(HttpExchange httpExchange) throws IOException {
        final Long traceId = Long
            .parseLong(httpExchange.getRequestHeaders().getFirst(HttpHeaders.SPM_TRACING_TRACE_ID));
        final Long callId = Long.parseLong(httpExchange.getRequestHeaders().getFirst(HttpHeaders.SPM_TRACING_CALL_ID));

        httpExchange.getResponseHeaders()
            .set(HttpHeaders.SPM_TRACING_CROSS_APP_CALL, HttpHeaders.encodeCrossAppCallHeader(
                100L, callId, traceId, 10, "1234", "/cross-app", Hostname.getLocalEndpoint(), false
            ));

        httpExchange.sendResponseHeaders(200, 0);
        httpExchange.close();
      }
    });
    server.bind(new InetSocketAddress("localhost", port), 0);
    server.start();
  }

  @After
  public void after() throws Exception {
    server.stop(2);
  }

  @Trace(force = true)
  private void executeMethod(HttpMethod method) throws Exception {
    HttpClient client = new HttpClient();
    client.executeMethod(method);
  }

  private static List<Call> filter(Call.CallTag tag, List<PartialTransaction> transactions) {
    final List<Call> filtered = new ArrayList<Call>();
    for (PartialTransaction transaction : transactions) {
      for (Call call : transaction.getCalls()) {
        if (tag == call.getCallTag()) {
          filtered.add(call);
        }
      }
    }
    return filtered;
  }

  private void testExecuteMethod(String methodName, HttpMethod method, String url, int responseCode) throws Exception {
    final MockTransactionSink sink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(sink);

    executeMethod(method);

    final List<Call> getCalls = filter(Call.CallTag.HTTP_REQUEST, sink.getTransactions());
    assertEquals(1, getCalls.size());

    Call getCall = getCalls.get(0);
    assertNotNull(getCall.getAnnotation());

    HTTPRequestAnnotation annotation = (HTTPRequestAnnotation) getCall.getAnnotation();

    assertEquals(methodName, annotation.getMethod());
    assertEquals(url, annotation.getUrl());
    assertEquals(responseCode, annotation.getResponseCode());
  }

  @Test
  public void testExecuteMethod() throws Exception {
    final String pingUrl = "http://localhost:" + port + "/ping";

    testExecuteMethod("GET", new GetMethod(pingUrl), pingUrl, 200);
    testExecuteMethod("POST", new PostMethod(pingUrl), pingUrl, 200);
    testExecuteMethod("DELETE", new DeleteMethod(pingUrl), pingUrl, 200);
    testExecuteMethod("HEAD", new HeadMethod(pingUrl), pingUrl, 200);
    testExecuteMethod("PUT", new PutMethod(pingUrl), pingUrl, 200);

    final String notFoundUrl = "http://localhost:" + port + "/not-found";

    testExecuteMethod("GET", new GetMethod(notFoundUrl), notFoundUrl, 404);
    testExecuteMethod("POST", new PostMethod(notFoundUrl), notFoundUrl, 404);
    testExecuteMethod("DELETE", new DeleteMethod(notFoundUrl), notFoundUrl, 404);
    testExecuteMethod("HEAD", new HeadMethod(notFoundUrl), notFoundUrl, 404);
    testExecuteMethod("PUT", new PutMethod(notFoundUrl), notFoundUrl, 404);
  }

  @Trace(force = true)
  public void crossAppCall() throws Exception {
    HttpClient client = new HttpClient();
    client.executeMethod(new GetMethod("http://localhost:" + port + "/cross-app"));
  }

  @Test
  public void testCaptureCrossApplicationCall() throws Exception {
    final MockTransactionSink sink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(sink);

    crossAppCall();

    List<Call> calls = filter(Call.CallTag.HTTP_REQUEST, sink.getTransactions());
    assertEquals(1, calls.size());

    Call httpRequestCall = calls.get(0);

    HTTPRequestAnnotation annotation = (HTTPRequestAnnotation) httpRequestCall.getAnnotation();
    assertNotNull(annotation);

    assertEquals("1234", httpRequestCall.getCrossAppToken());
    assertEquals(new Long(100L), httpRequestCall.getCrossAppCallId());
    assertEquals(new Long(10L), httpRequestCall.getCrossAppDuration());
    assertNotNull(httpRequestCall.getCrossAppParentCallId());
  }

  @Test
  public void testCaptureSolrCalls() throws Exception {
    Tracing.newTrace("test-request-type", Call.TransactionType.BACKGROUND);
    final MockTransactionSink mockTransSink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(mockTransSink);

    HttpClient httpClient = new HttpClient();
    SolrHttpClient.markSolrClient((HttpClientAccess) httpClient);

    GetMethod select = new GetMethod("http://localhost:" + port + "/select");

    httpClient.executeMethod(select);

    List<Call> solrCalls = filter(Call.CallTag.SOLR, mockTransSink.getTransactions());
    assertEquals(1, solrCalls.size());
  }
}
