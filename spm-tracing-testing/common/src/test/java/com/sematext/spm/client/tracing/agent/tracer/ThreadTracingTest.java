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

import org.junit.Test;
import org.junit.runner.RunWith;

import com.sematext.spm.client.tracing.Trace;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.utils.MockTransactionSink;
import com.sematext.spm.client.tracing.utils.TracingContext;
import com.sematext.spm.client.tracing.utils.TracingJUnit4ClassRunner;

@RunWith(TracingJUnit4ClassRunner.class)
@TracingContext(tracers = { Tracers.InterThreadCommunication.class, Tracers.TracedMethodsTracer.class })
public class ThreadTracingTest {

  @Trace
  public void forkThread() throws Exception {
    final Runnable work = new Runnable() {
      @Override
      public void run() {
        ThreadTracingTest.this.runJob();
      }
    };
    final Thread thread = new Thread(work);
    thread.start();
    thread.join();
  }

  @Trace
  public void runJob() {
  }

  @Test
  public void testThread() throws Exception {
    Tracing.newTrace("threading", Call.TransactionType.BACKGROUND);

    final MockTransactionSink mockCallSink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(mockCallSink);

    forkThread();

    for (Call call : mockCallSink.getTransactions().get(0).getCalls()) {
      System.out.println(call);
    }
  }
}
