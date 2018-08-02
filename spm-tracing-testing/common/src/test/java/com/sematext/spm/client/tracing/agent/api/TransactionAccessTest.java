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
package com.sematext.spm.client.tracing.agent.api;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.sematext.spm.client.tracing.Trace;
import com.sematext.spm.client.tracing.TracingParameters;
import com.sematext.spm.client.tracing.agent.NoTrace;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.errors.TracingError;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.FailureType;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.tracer.Tracers;
import com.sematext.spm.client.tracing.utils.MockSink;
import com.sematext.spm.client.tracing.utils.MockTransactionSink;
import com.sematext.spm.client.tracing.utils.TracingContext;
import com.sematext.spm.client.tracing.utils.TracingJUnit4ClassRunner;
import com.sematext.spm.client.tracing.utils.TracingTesting;

@RunWith(TracingJUnit4ClassRunner.class)
@TracingContext(tracers = { Tracers.TracedMethodsTracer.class })
public class TransactionAccessTest {

  @Trace(force = false)
  private void tracedMethod() {
    System.out.println("traced method");
  }

  @Test
  public void testSetName() {
    MockTransactionSink mockSink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(mockSink);

    Tracing.newTrace("generated-name", Call.TransactionType.BACKGROUND);
    TransactionAccess.setName("custom-name");
    tracedMethod();
    Tracing.endTrace();

    PartialTransaction trans = mockSink.getTransactions().get(0);
    assertEquals("custom-name", trans.getRequest());
  }

  @Test
  public void testIgnore() {
    MockTransactionSink mockSink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(mockSink);

    Tracing.newTrace("ignored-transaction", Call.TransactionType.BACKGROUND);
    TransactionAccess.ignore();
    tracedMethod();
    Tracing.endTrace();

    assertTrue(mockSink.getTransactions().isEmpty());
  }

  @Test
  public void testSetTransactionParameter() {
    MockTransactionSink mockSink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(mockSink);

    Tracing.newTrace("parametrized-transaction", Call.TransactionType.BACKGROUND);

    TransactionAccess.setTransactionParameter("key-a", "value-a");
    TransactionAccess.setTransactionParameter("key-b", "value-b");

    assertEquals("value-a", TransactionAccess.getTransactionParameters().get("key-a"));
    assertEquals("value-b", TransactionAccess.getTransactionParameters().get("key-b"));

    tracedMethod();
    Tracing.endTrace();

    final PartialTransaction trans = mockSink.getTransactions().get(0);
    assertEquals(new HashMap<String, String>() {{
      put("key-a", "value-a");
      put("key-b", "value-b");
    }}, trans.getParameters());
  }

  @Test
  public void testSetMethodParameter() {
    MockTransactionSink mockSink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(mockSink);

    Tracing.newTrace("parametrized-transaction", Call.TransactionType.BACKGROUND);

    Tracing.current().newCall("root-call", System.currentTimeMillis());

    Tracing.current().newCall("call-1", System.currentTimeMillis());
    TransactionAccess.setMethodParameter("key-call-1", "call-1-value");
    assertEquals("call-1-value", TransactionAccess.getMethodParameters().get("key-call-1"));
    Tracing.current().endCall();

    Tracing.current().newCall("call-2", System.currentTimeMillis());
    TransactionAccess.setMethodParameter("key-call-2", "call-2-value");
    assertEquals("call-2-value", TransactionAccess.getMethodParameters().get("key-call-2"));
    Tracing.current().endCall();

    Tracing.current().endCall();
    Tracing.endTrace();

    final List<Call> call1 = TracingTesting.findMatching("call-1", mockSink.getTransactions());
    assertFalse(call1.isEmpty());
    assertEquals(Collections.singletonMap("key-call-1", "call-1-value"), call1.get(0).getParameters());

    final List<Call> call2 = TracingTesting.findMatching("call-2", mockSink.getTransactions());
    assertFalse(call2.isEmpty());
    assertEquals(Collections.singletonMap("key-call-2", "call-2-value"), call2.get(0).getParameters());
  }

  @Test
  public void testNoticeErrorThrowable() {
    MockSink<TracingError> errorsSink = new MockSink<TracingError>();
    ServiceLocator.getErrorSinks().clear();
    ServiceLocator.getErrorSinks().add(errorsSink);

    MockTransactionSink transactionsSink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(transactionsSink);

    Tracing.newTrace("parametrized-transaction", Call.TransactionType.BACKGROUND);
    assertNotEquals(NoTrace.instance(), Tracing.current());

    TransactionAccess.setTransactionParameter("alias", "trace-1");

    Tracing.current().newCall("A", 0);
    Tracing.current().newCall("B", 1);

    TransactionAccess.noticeError(new IllegalArgumentException());

    assertEquals(NoTrace.instance(), Tracing.current());

    assertEquals(1, transactionsSink.getTransactions().size());

    PartialTransaction transaction = transactionsSink.getTransactions().get(0);
    assertTrue(transaction.isFailed());
    assertEquals(FailureType.EXCEPTION, transaction.getFailureType());

    assertEquals(1, errorsSink.getEvents().size());
    TracingError error = errorsSink.getEvents().get(0);

    assertEquals("java.lang.IllegalArgumentException", error.getParameters()
        .get(TracingParameters.ERROR_CLASS.getKey()));
    assertEquals("trace-1", error.getParameters().get("alias"));
  }

  @Test
  public void testNoticeErrorMessage() {
    MockSink<TracingError> errorsSink = new MockSink<TracingError>();
    ServiceLocator.getErrorSinks().clear();
    ServiceLocator.getErrorSinks().add(errorsSink);

    MockTransactionSink transactionsSink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(transactionsSink);

    Tracing.newTrace("parametrized-transaction", Call.TransactionType.BACKGROUND);
    assertNotEquals(NoTrace.instance(), Tracing.current());

    TransactionAccess.setTransactionParameter("alias", "trace-1");

    Tracing.current().newCall("A", 0);
    Tracing.current().newCall("B", 1);

    TransactionAccess.noticeError("something-went-wrong");

    assertEquals(NoTrace.instance(), Tracing.current());

    assertEquals(1, transactionsSink.getTransactions().size());

    PartialTransaction transaction = transactionsSink.getTransactions().get(0);
    assertTrue(transaction.isFailed());
    assertEquals(FailureType.CUSTOM, transaction.getFailureType());
    assertEquals("something-went-wrong", transaction.getParameters().get(TracingParameters.ERROR_MESSAGE.getKey()));

    assertEquals(1, errorsSink.getEvents().size());
    TracingError error = errorsSink.getEvents().get(0);

    assertEquals("something-went-wrong", error.getParameters().get(TracingParameters.ERROR_MESSAGE.getKey()));
    assertEquals("trace-1", error.getParameters().get("alias"));
  }

}
