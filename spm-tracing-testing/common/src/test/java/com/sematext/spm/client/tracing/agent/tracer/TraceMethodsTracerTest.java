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

import static com.sematext.spm.client.tracing.utils.TracingTesting.findMatching;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.sematext.spm.client.tracing.Trace;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.utils.MockTransactionSink;
import com.sematext.spm.client.tracing.utils.TracingContext;
import com.sematext.spm.client.tracing.utils.TracingJUnit4ClassRunner;

@RunWith(TracingJUnit4ClassRunner.class)
@TracingContext(tracers = Tracers.TracedMethodsTracer.class)
public class TraceMethodsTracerTest {

  @Trace(force = true)
  public void method1() {
    System.out.println("method1");
  }

  @Trace(force = false)
  public void method2() {
    System.out.println("method2");
  }

  @Trace(force = true)
  public void service() {
    daoCall();
  }

  @Trace
  public void daoCall() {
    System.out.println("daoCall()");
  }

  @Test
  public void testCreateNewTransactionIfNotExists() throws Exception {
    final MockTransactionSink sink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(sink);

    method1();

    assertEquals(1, sink.getTransactions().size());
    assertEquals(1, sink.getTransactions().get(0).getCalls().size());
    assertTrue(sink.getTransactions().get(0).isEntryPoint());
    assertTrue(sink.getTransactions().get(0).getCalls().get(0).isEntryPoint());
  }

  @Test
  public void testDoNotCreateTransactionIfForceIsFalse() {
    final MockTransactionSink sink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(sink);

    method2();

    assertTrue(sink.getTransactions().isEmpty());
  }

  @Test
  public void testShouldKeepTransactionName() {
    final MockTransactionSink sink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(sink);

    service();

    assertEquals(1, sink.getTransactions().size());

    assertEquals(2, sink.getTransactions().get(0).getCalls().size());
    assertEquals(1, findMatching(".*daoCall.*", sink.getTransactions()).size());
    assertEquals(1, findMatching(".*service.*", sink.getTransactions()).size());
  }
}
