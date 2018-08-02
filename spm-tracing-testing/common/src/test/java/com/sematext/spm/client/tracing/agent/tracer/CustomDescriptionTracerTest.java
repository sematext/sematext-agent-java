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
import static com.sematext.spm.client.tracing.utils.TracingTesting.setupSink;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.config.Config;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.FailureType;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.pointcuts.custom.ExtensionPointcut;
import com.sematext.spm.client.tracing.agent.transformation.TracingTransform;
import com.sematext.spm.client.tracing.utils.MockTransactionSink;
import com.sematext.spm.client.tracing.utils.TracingContext;
import com.sematext.spm.client.tracing.utils.TracingJUnit4ClassRunner;
import com.sematext.spm.client.unlogger.Logspect;
import com.sematext.spm.client.unlogger.xml.InstrumentationDescriptor;
import com.sematext.spm.client.unlogger.xml.InstrumentationLoaderException;
import com.sematext.spm.client.unlogger.xml.XMLInstrumentationDescriptorLoader;

@RunWith(TracingJUnit4ClassRunner.class)
@TracingContext(tracers = { CustomDescriptionTracerTest.CustomTracer.class })
public class CustomDescriptionTracerTest {

  public static class CustomTracer implements Tracer {
    private final InstrumentationDescriptor descriptor;

    public CustomTracer() {
      XMLInstrumentationDescriptorLoader loader = new XMLInstrumentationDescriptorLoader(ExtensionPointcut.class);
      try {
        this.descriptor = loader
            .load(CustomDescriptionTracerTest.class.getResourceAsStream("/custom-description-tracer-test.xml"));
      } catch (InstrumentationLoaderException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public String getName() {
      return descriptor.getName();
    }

    @Override
    public Collection<Logspect> createLogspects(ClassLoader loader) {
      return descriptor.getLogspects();
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[0];
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }
  }

  public void longWorker() {
    System.out.println("Doing work");
  }

  public void fastWorker() {
    System.out.println("Fast worker");
  }

  public void testMethodName1() {
    System.out.println("testMethodName1");
  }

  @Test
  public void testMethodMatch() throws Exception {
    MockTransactionSink sink = setupSink();

    Tracing.newTrace("/", Call.TransactionType.BACKGROUND, 1L, 0L, true);

    longWorker();
    fastWorker();
    testMethodName1();

    assertEquals(1, findMatching(".*longWorker.*", sink.getTransactions()).size());
    assertEquals(1, findMatching(".*fastWorker.*", sink.getTransactions()).size());
    assertEquals(1, findMatching(".*testMethodName1.*", sink.getTransactions()).size());

    Tracing.endTrace();
  }

  public static class Service {
    public void loadUser() {
      System.out.println("loadUser");
    }

    public void loadSystems(Long userId) {
      System.out.println("loadSystems");
    }
  }

  @Test
  public void testWholeClassMatch() throws Exception {
    MockTransactionSink sink = setupSink();

    Tracing.newTrace("/", Call.TransactionType.BACKGROUND, 1L, 0L, true);

    final Service service = new Service();

    service.loadSystems(10L);
    service.loadUser();

    assertEquals(1, findMatching(".*loadUser.*", sink.getTransactions()).size());
    assertEquals(1, findMatching(".*loadSystems.*", sink.getTransactions()).size());

    Tracing.endTrace();
  }

  public static class Foo {
    public Foo(int x) {
    }

    public Foo(int x, int y) {
    }

    public Foo(String name) {
    }
  }

  @Test
  public void testConstructorMatch() throws Exception {
    MockTransactionSink sink = setupSink();

    Tracing.newTrace("/", Call.TransactionType.BACKGROUND, 1L, 0L, true);

    new Foo(42);
    new Foo(42, 24);
    new Foo("foo");

    assertEquals(3, findMatching(".*Foo.*", sink.getTransactions()).size());

    Tracing.endTrace();
  }

  public static class Job {
    public void doJob() {
      System.out.println("job");
    }
  }

  @Test
  public void testEntryPoint() throws Exception {
    MockTransactionSink sink = setupSink();

    new Job().doJob();

    assertEquals(1, findMatching(".*doJob.*", sink.getTransactions()).size());
    assertEquals("com.sematext.spm.client.tracing.agent.tracer.CustomDescriptionTracerTest$Job#doJob", sink
        .getTransactions().get(0).getRequest());
  }

  public static class CustomJob {
    public void doJob() {
      System.out.println("job");
    }
  }

  @Test
  public void testEntryPointTransactionName() throws Exception {
    MockTransactionSink sink = setupSink();

    new CustomJob().doJob();

    assertEquals(1, findMatching(".*CustomJob.*", sink.getTransactions()).size());

    assertEquals("CustomTransactionName", sink.getTransactions().get(0).getRequest());
  }

  public static class UnsuccessfulJob {
    public void doJob() {
      throw new IllegalStateException();
    }
  }

  @Test
  public void testEntryPointFailure() throws Exception {
    MockTransactionSink sink = setupSink();

    try {
      new UnsuccessfulJob().doJob();
    } catch (Exception e) { /* pass */ }

    assertEquals(1, findMatching(".*UnsuccessfulJob.*", sink.getTransactions()).size());

    PartialTransaction transaction = sink.getTransactions().get(0);

    assertTrue(transaction.isFailed());
    assertEquals(FailureType.EXCEPTION, transaction.getFailureType());
    assertNotNull(transaction.getExceptionStackTrace());
  }
}
