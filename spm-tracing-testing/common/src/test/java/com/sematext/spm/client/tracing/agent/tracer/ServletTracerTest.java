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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sematext.spm.client.tracing.TracingParameters;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.errors.TracingError;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.HttpHeaders;
import com.sematext.spm.client.tracing.agent.model.HttpHeaders.CrossAppCallHeader;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.model.WebTransactionSummary;
import com.sematext.spm.client.tracing.agent.util.Hostname;
import com.sematext.spm.client.tracing.utils.MockHttpServletRequest;
import com.sematext.spm.client.tracing.utils.MockHttpServletResponse;
import com.sematext.spm.client.tracing.utils.MockSink;
import com.sematext.spm.client.tracing.utils.MockTransactionSink;
import com.sematext.spm.client.tracing.utils.TracingContext;
import com.sematext.spm.client.tracing.utils.TracingJUnit4ClassRunner;

@RunWith(TracingJUnit4ClassRunner.class)
@TracingContext(tracers = Tracers.ServletTracer.class)
public class ServletTracerTest {

  public static class JspRenderServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    }
  }

  public static class DispatchServlet extends HttpServlet {
    private final JspRenderServlet headerJsp;
    private final JspRenderServlet footerJsp;

    public DispatchServlet(JspRenderServlet headerJsp, JspRenderServlet footerJsp) {
      this.headerJsp = headerJsp;
      this.footerJsp = footerJsp;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      headerJsp.service(req, resp);
      footerJsp.service(req, resp);
    }
  }

  public static class FailingServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      throw new IllegalStateException();
    }
  }

  @Test
  public void testNestedServletCalls() throws Exception {
    final MockTransactionSink sink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(sink);

    final JspRenderServlet headerJsp = new JspRenderServlet();
    final JspRenderServlet footerJsp = new JspRenderServlet();
    final DispatchServlet dispatcher = new DispatchServlet(headerJsp, footerJsp);

    final MockHttpServletResponse response = new MockHttpServletResponse();

    dispatcher.service(new MockHttpServletRequest("/users/", "GET"), response);

    assertEquals(1, sink.getTransactions().size());

    PartialTransaction transaction = sink.getTransactions().get(0);

    assertEquals(3, transaction.getCalls().size());

    Call d = transaction.getCalls().get(2), jsp1 = transaction.getCalls().get(1), jsp2 = transaction.getCalls().get(0);

    assertEquals(0, d.getParentCallId());
    assertEquals(jsp1.getParentCallId(), d.getCallId());
    assertEquals(jsp2.getParentCallId(), d.getCallId());
    assertEquals(200, ((WebTransactionSummary) transaction.getTransactionSummary()).getResponseCode());

    assertEquals("com.sematext.spm.client.tracing.agent.tracer.ServletTracerTest$JspRenderServlet#service", transaction
        .getRequest());

    //assertEquals("GET", ((WebTransactionSummary) d.getTransactionSummary()).getRequestMethod());

    assertNull(response.getHeader(HttpHeaders.SPM_TRACING_CROSS_APP_CALL));
  }

  @Test
  public void testCrossAppTracing() throws Exception {
    final MockTransactionSink sink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(sink);

    final JspRenderServlet headerJsp = new JspRenderServlet();
    final JspRenderServlet footerJsp = new JspRenderServlet();
    final DispatchServlet dispatcher = new DispatchServlet(headerJsp, footerJsp);

    final MockHttpServletRequest request = new MockHttpServletRequest("com.sematext.spm.client.tracing.agent.tracer.ServletTracerTest$JspRenderServlet#doGet", "GET");
    request.setHeader(HttpHeaders.SPM_TRACING_CALL_ID, "1");
    request.setHeader(HttpHeaders.SPM_TRACING_TRACE_ID, "2");
    request.setHeader(HttpHeaders.SPM_TRACING_SAMPLED, "true");

    final MockHttpServletResponse response = new MockHttpServletResponse();

    dispatcher.service(request, response);

    final String crossAppCallHeader = response.getHeader(HttpHeaders.SPM_TRACING_CROSS_APP_CALL);
    assertNotNull(crossAppCallHeader);

    assertEquals(1, sink.getTransactions().size());

    PartialTransaction transaction = sink.getTransactions().get(0);

    Call dispatcherJspCall = transaction.getCalls().get(2);
    Call headerJspCall = transaction.getCalls().get(1);
    Call footerJspCall = transaction.getCalls().get(0);

    assertEquals(1L, dispatcherJspCall.getParentCallId());

    final CrossAppCallHeader header = HttpHeaders.decodeCrossAppCallHeader(crossAppCallHeader);
    assertNotNull(header);

    assertEquals(dispatcherJspCall.getCallId(), header.getCallId());
    assertEquals(dispatcherJspCall.getParentCallId(), header.getParentCallId());

    assertEquals(dispatcherJspCall.getCallId(), headerJspCall.getParentCallId());

    assertEquals(dispatcherJspCall.getCallId(), footerJspCall.getParentCallId());
  }

  @Test
  public void testFailedServletTransactionParametersCapture() throws Exception {
    final MockTransactionSink transactionSink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(transactionSink);

    final MockSink<TracingError> errorSink = new MockSink<TracingError>();
    ServiceLocator.getErrorSinks().clear();
    ServiceLocator.getErrorSinks().add(errorSink);

    final FailingServlet servlet = new FailingServlet();
    final MockHttpServletRequest request = new MockHttpServletRequest("/status", "GET");

    final MockHttpServletResponse response = new MockHttpServletResponse();

    try {
      servlet.service(request, response);
    } catch (Exception e) {
      //pass
    }

    assertEquals(1, transactionSink.getTransactions().size());

    final PartialTransaction transaction = transactionSink.getTransactions().get(0);

    assertTrue(transaction.isFailed());

    assertEquals(1, errorSink.getEvents().size());

    final TracingError tracingError = errorSink.getEvents().get(0);

    assertEquals("/status", tracingError.getParameters().get(TracingParameters.REQUEST.getKey()));
    assertEquals("java.lang.IllegalStateException", tracingError.getParameters()
        .get(TracingParameters.ERROR_CLASS.getKey()));
    assertNotNull(tracingError.getParameters().get(TracingParameters.DURATION.getKey()));
    assertEquals(Hostname.getLocalEndpoint().getHostname(), tracingError.getParameters()
        .get(TracingParameters.HOST.getKey()));
  }
}
