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

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.Call.CallTag;
import com.sematext.spm.client.tracing.agent.model.HttpHeaders;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.model.annotation.HTTPRequestAnnotation;
import com.sematext.spm.client.tracing.agent.util.Hostname;
import com.sematext.spm.client.tracing.utils.MockTransactionSink;
import com.sematext.spm.client.tracing.utils.TracingContext;
import com.sematext.spm.client.tracing.utils.TracingJUnit4ClassRunner;

@RunWith(TracingJUnit4ClassRunner.class)
@TracingContext(tracers = { Tracers.TracedMethodsTracer.class, Tracers.HttpClient4Tracer.class })
public class HttpClient4TracerTest {

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
  private void executeRequest(HttpUriRequest request) throws Exception {
    CloseableHttpClient client = HttpClients.createDefault();
    client.execute(request);
  }

  private static List<Call> filter(CallTag tag, List<PartialTransaction> transactions) {
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

  private void testExecuteRequest(String methodName, HttpUriRequest request, String url, int responseCode)
      throws Exception {
    final MockTransactionSink sink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(sink);

    executeRequest(request);

    final List<Call> getCalls = filter(CallTag.HTTP_REQUEST, sink.getTransactions());
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

    testExecuteRequest("GET", new HttpGet(pingUrl), pingUrl, 200);
    testExecuteRequest("POST", new HttpPost(pingUrl), pingUrl, 200);
    testExecuteRequest("DELETE", new HttpDelete(pingUrl), pingUrl, 200);
    testExecuteRequest("HEAD", new HttpHead(pingUrl), pingUrl, 200);
    testExecuteRequest("PUT", new HttpPut(pingUrl), pingUrl, 200);

    final String notFoundUrl = "http://localhost:" + port + "/not-found";

    testExecuteRequest("GET", new HttpGet(notFoundUrl), notFoundUrl, 404);
    testExecuteRequest("POST", new HttpPost(notFoundUrl), notFoundUrl, 404);
    testExecuteRequest("DELETE", new HttpDelete(notFoundUrl), notFoundUrl, 404);
    testExecuteRequest("HEAD", new HttpHead(notFoundUrl), notFoundUrl, 404);
    testExecuteRequest("PUT", new HttpPut(notFoundUrl), notFoundUrl, 404);
  }

  @Trace(force = true)
  public void relativeUriCall() throws Exception {
    CloseableHttpClient client = HttpClients.createDefault();
    client.execute(new HttpHost("localhost", port), new HttpGet("/ping"));
  }

  @Test
  public void testRelativeUri() throws Exception {
    final MockTransactionSink sink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(sink);

    relativeUriCall();

    List<Call> calls = filter(CallTag.HTTP_REQUEST, sink.getTransactions());
    assertEquals(1, calls.size());

    Call httpRequest = calls.get(0);
    HTTPRequestAnnotation annotation = (HTTPRequestAnnotation) httpRequest.getAnnotation();

    assertEquals("GET", annotation.getMethod());
    assertEquals("http://localhost:" + port + "/ping", annotation.getUrl());
    assertEquals(200, annotation.getResponseCode());
  }

  @Trace(force = true)
  public void crossAppCall() throws Exception {
    CloseableHttpClient client = HttpClients.createDefault();
    client.execute(new HttpGet("http://localhost:" + port + "/cross-app"));
  }

  @Test
  public void testCaptureCrossApplicationCall() throws Exception {
    final MockTransactionSink sink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(sink);

    crossAppCall();

    List<Call> calls = filter(CallTag.HTTP_REQUEST, sink.getTransactions());
    assertEquals(1, calls.size());

    Call httpRequestCall = calls.get(0);

    HTTPRequestAnnotation annotation = (HTTPRequestAnnotation) httpRequestCall.getAnnotation();
    assertNotNull(annotation);
    assertEquals("http://localhost:" + port + "/cross-app", annotation.getUrl());

    assertEquals("1234", httpRequestCall.getCrossAppToken());
    assertEquals(new Long(100L), httpRequestCall.getCrossAppCallId());
    assertEquals(new Long(10L), httpRequestCall.getCrossAppDuration());
    assertNotNull(httpRequestCall.getCrossAppParentCallId());
  }

}
