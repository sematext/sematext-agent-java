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

package com.sematext.spm.client.tracing.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import com.sematext.spm.client.tracing.agent.api.TransactionAccess;
import com.sematext.spm.client.tracing.agent.config.Config;
import com.sematext.spm.client.tracing.agent.config.ServiceConfigurer;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.errors.TracingError;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.sampling.Sampler;
import com.sematext.spm.client.tracing.agent.stats.TracingStatistics;
import com.sematext.spm.client.tracing.utils.MockTransactionSink;
import com.sematext.spm.client.unlogger.JoinPoint;

public class TracingTest {
  public static class Configurer implements ServiceConfigurer {
    private final MockTransactionSink callSink = new MockTransactionSink();

    private final Config config = new Config() {{
      setToken("123");
    }};

    @Override
    public Config getConfig() {
      return config;
    }

    @Override
    public Sampler<String> getTransactionSampler() {
      return null;
    }

    @Override
    public Sampler<TracingError> getTracingErrorSampler() {
      return null;
    }

    @Override
    public List<Sink<PartialTransaction>> getTransactionSinks() {
      return Arrays.<Sink<PartialTransaction>>asList(callSink);
    }

    @Override
    public List<Sink<TracingError>> getErrorSinks() {
      return null;
    }

    public MockTransactionSink getCallSink() {
      return callSink;
    }

    @Override
    public TracingStatistics getTracingStatistics() {
      return null;
    }

    @Override
    public TracingAgentControl getTracingAgentControl() {
      return null;
    }
  }

  @Test
  public void testSelfDurationCalculatedCorrectly() {
    final Configurer config = new Configurer();

    ServiceLocator.configure(config);

    Tracing.newTrace("", Call.TransactionType.BACKGROUND, 1, 1, true);

    Tracing.current().newCall("Servlet", 1);
    Tracing.current().newCall("JpaQuery-1", 2);
    Tracing.current().newCall("JdbcQuery-1-1", 3);
    Tracing.current().endCall(4);

    Tracing.current().newCall("JdbcQuery-1-2", 4);
    Tracing.current().endCall(5);

    Tracing.current().endCall(6); //JpaQuery-1

    Tracing.current().newCall("JpaQuery-2", 7);
    Tracing.current().newCall("JdbcQuery-2-1", 8);
    Tracing.current().endCall(9);

    Tracing.current().newCall("JdbcQuery-2-2", 10);
    Tracing.current().endCall(11);

    Tracing.current().endCall(12); //JpaQuery-2

    Tracing.current().endCall(13); //Servlet

    Deque<Call> stack = new ArrayDeque<Call>(config.getCallSink().getTransactions().get(0).getCalls());

    assertEquals(1, stack.pop().getDuration()); //JdbcQuery-1-1
    assertEquals(1, stack.pop().getDuration()); //JdbcQuery-1-2

    assertEquals(4, stack.peek().getDuration()); //JpaQuery-1
    assertEquals(2, stack.pop().getSelfDuration()); //JpaQuery-1

    assertEquals(1, stack.pop().getDuration()); //JdbcQuery-2-1

    assertEquals(1, stack.pop().getDuration()); // JdbcQuery-2-2

    assertEquals(5, stack.peek().getDuration()); // JpaQuery-2
    assertEquals(3, stack.pop().getSelfDuration()); // JpaQuery-2

    assertEquals(12, stack.peek().getDuration()); // Servlet
    assertEquals(3, stack.pop().getSelfDuration()); // Servlet
  }

  @Test
  public void testSinkCallsByDuration() {
    final Configurer config = new Configurer();
    config.getConfig().setDurationThresholdMillis(100);

    ServiceLocator.configure(config);

    Tracing.newTrace("", Call.TransactionType.BACKGROUND, 1, 1, true);

    Tracing.current().newCall("A", 0);
    Tracing.current().newCall("B", 1);
    Tracing.current().endCall(99);
    Tracing.current().endCall(100);

    assertEquals(1, config.getCallSink().getTransactions().size());
    assertEquals(2, config.getCallSink().getTransactions().get(0).getCalls().size());

    config.getCallSink().getTransactions().clear();

    Tracing.current().newCall("A", 0);
    Tracing.current().endCall(10);

    assertTrue(config.getCallSink().getTransactions().isEmpty());

    Tracing.current().newCall("A", 0);
    Tracing.current().endCall(100);

    assertEquals(1, config.getCallSink().getTransactions().size());
    assertEquals(1, config.getCallSink().getTransactions().get(0).getCalls().size());
  }

  @Test
  public void testIgnore() {
    final Configurer config = new Configurer();
    config.getConfig().setDurationThresholdMillis(100);

    ServiceLocator.configure(config);

    Tracing.newTrace("", Call.TransactionType.BACKGROUND, 1, 1, true);

    Tracing.current().newCall("A", 0);
    Tracing.current().newCall("B", 1);
    Tracing.current().endCall(99);
    Tracing.current().endCall(100);

    assertEquals(1, config.getCallSink().getTransactions().size());
    config.getCallSink().getTransactions().clear();

    Tracing.current().ignore();

    Tracing.current().newCall("A", 0);
    Tracing.current().newCall("B", 1);
    Tracing.current().endCall(99);
    Tracing.current().endCall(100);

    assertTrue(config.getCallSink().getTransactions().isEmpty());
  }

  @Test
  public void testNaming() {
    Configurer config = new Configurer();
    ServiceLocator.configure(config);

    Tracing.newTrace("transaction-1", Call.TransactionType.BACKGROUND, 1, 1, true);
    Tracing.current().getNamer().asFramework(new JoinPoint("Framework", "entryPoint", ""));

    Tracing.current().newCall("A", 0);
    Tracing.current().endCall(10);

    Tracing.endTrace();

    assertEquals("Framework#entryPoint", config.getCallSink().getTransactions().get(0).getRequest());

    Tracing.newTrace("", Call.TransactionType.BACKGROUND, 1, 1, true);
    Tracing.current().getNamer().asFramework(new JoinPoint("Framework", "entryPoint", ""));

    TransactionAccess.setName("redefined");

    Tracing.current().newCall("A", 0);
    Tracing.current().endCall(10);

    Tracing.endTrace();

    assertEquals("redefined", config.getCallSink().getTransactions().get(1).getRequest());
  }

  @Test
  public void testForceTraceEnd() {
    Configurer config = new Configurer();
    ServiceLocator.configure(config);

    Tracing.newTrace("transaction-1", Call.TransactionType.BACKGROUND, 1, 1, true);
    Tracing.current().newCall("A", 0);
    Tracing.current().newCall("B", 1);
    Tracing.current().newCall("C", 2);

    assertNotEquals(NoTrace.instance(), Tracing.current());

    Tracing.current().forceEnd(true);

    assertNull(Tracing.current().getCurrentCall());
  }

}
