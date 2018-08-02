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
package com.sematext.spm.client.tracing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.tracing.agent.CallSinks;
import com.sematext.spm.client.tracing.agent.NoTrace;
import com.sematext.spm.client.tracing.agent.Sink;
import com.sematext.spm.client.tracing.agent.Trace;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.api.TransactionAccess;
import com.sematext.spm.client.tracing.agent.config.ServiceConfigurer;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.errors.TracingError;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.ESAction;
import com.sematext.spm.client.tracing.agent.model.Endpoint;
import com.sematext.spm.client.tracing.agent.model.FailureType;
import com.sematext.spm.client.tracing.agent.model.InetAddress;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.model.SolrAnnotation;
import com.sematext.spm.client.tracing.agent.model.WebTransactionSummary;
import com.sematext.spm.client.tracing.agent.model.annotation.ESAnnotation;
import com.sematext.spm.client.tracing.agent.model.annotation.HTTPRequestAnnotation;
import com.sematext.spm.client.tracing.agent.model.annotation.SQLAnnotation;
import com.sematext.spm.client.tracing.agent.sql.SqlStatement;
import com.sematext.spm.client.tracing.agent.tracer.Tracer;
import com.sematext.spm.client.tracing.agent.transformation.TracingTransform;
import com.sematext.spm.client.tracing.agent.transformation.transforms.MixinTransform;
import com.sematext.spm.client.tracing.utils.TestServiceConfigurer;
import com.sematext.spm.client.unlogger.JoinPoint;
import com.sematext.spm.client.unlogger.LogPointWeaver;
import com.sematext.spm.client.unlogger.Logspect;
import com.sematext.spm.client.unlogger.Pointcut;
import com.sematext.spm.client.unlogger.dispatch.AdviceDispatcher;
import com.sematext.spm.client.unlogger.dispatch.DispatchUnit;
import com.sematext.spm.client.unlogger.dispatch.LastInchOfTrampoline;
import com.sematext.spm.client.util.InstrumentationUtils;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.Loader;
import javassist.NotFoundException;
import javassist.Translator;

public class TestingUnlogger {

  private final LogPointWeaver weaver;
  private final ClassPool classPool = ClassPool.getDefault();
  private final List<TracingTransform> transforms = new ArrayList<TracingTransform>();
  private final Loader loader;

  private TestingUnlogger(Tracer[] tracers) throws Exception {
    final List<String> loggers = new ArrayList<String>();
    final List<Logspect> logspects = new ArrayList<Logspect>();

    for (final Tracer tracer : tracers) {
      logspects.addAll(tracer.createLogspects(TestingUnlogger.class.getClassLoader()));
      transforms.addAll(Arrays.asList(tracer.getStructuralTransforms()));
    }

    final Map<Pointcut, Integer> pointcuts = new HashMap<Pointcut, Integer>();

    for (final Logspect logspect : logspects) {
      for (final Pointcut pointcut : logspect.getPointcuts()) {
        int dispatchId = DispatchUnit.registerDispatchUnit(logspect.getName(), pointcut, logspect.createUnlogger());
        pointcuts.put(pointcut, dispatchId);
      }
    }

    ServiceLocator.configure(new TestServiceConfigurer("123"));

    weaver = new LogPointWeaver(pointcuts, LastInchOfTrampoline.class.getName());

    loader = new Loader(classPool);
    loader.doDelegation = true;

    loader.delegateLoadingOf(LastInchOfTrampoline.class.getName());
    loader.delegateLoadingOf(NoTrace.class.getName());
    loader.delegateLoadingOf(Tracing.class.getName());
    loader.delegateLoadingOf(Trace.class.getName());
    loader.delegateLoadingOf(PartialTransaction.class.getName());
    loader.delegateLoadingOf(JoinPoint.class.getName());
    loader.delegateLoadingOf("org.junit.");
    loader.delegateLoadingOf("org.apache.commons.logging.");
    loader.delegateLoadingOf(Call.class.getName());
    loader.delegateLoadingOf(Call.TransactionType.class.getName());
    loader.delegateLoadingOf(Call.CallTag.class.getName());
    loader.delegateLoadingOf(FailureType.class.getName());
    loader.delegateLoadingOf(Endpoint.class.getName());
    loader.delegateLoadingOf(CallSinks.class.getName());
    loader.delegateLoadingOf(Sink.class.getName());
    loader.delegateLoadingOf(TracingError.class.getName());
    loader.delegateLoadingOf(ESAction.class.getName());
    loader.delegateLoadingOf(ESAction.OperationType.class.getName());
    loader.delegateLoadingOf(ServiceLocator.class.getName());
    loader.delegateLoadingOf(ServiceConfigurer.class.getName());
    loader.delegateLoadingOf(WebTransactionSummary.class.getName());
    loader.delegateLoadingOf(ESAnnotation.class.getName());
    loader.delegateLoadingOf(ESAnnotation.RequestType.class.getName());
    loader.delegateLoadingOf(ESAction.class.getName());
    loader.delegateLoadingOf(SQLAnnotation.class.getName());
    loader.delegateLoadingOf(HTTPRequestAnnotation.class.getName());
    loader.delegateLoadingOf(SolrAnnotation.class.getName());
    loader.delegateLoadingOf(SolrAnnotation.RequestType.class.getName());
    loader.delegateLoadingOf(InetAddress.class.getName());
    loader.delegateLoadingOf(TransactionAccess.class.getName());
    loader.delegateLoadingOf(SqlStatement.class.getName());
    loader.delegateLoadingOf(SqlStatement.Operation.class.getName());

    for (final Tracer tracer : tracers) {
      for (TracingTransform transform : tracer.getStructuralTransforms()) {
        if (transform instanceof MixinTransform) {
          loader.delegateLoadingOf(((MixinTransform) transform).getIface().getName());
        }
      }
    }

    loader.addTranslator(classPool, new Translator() {
      @Override
      public void start(ClassPool pool) throws NotFoundException, CannotCompileException {

      }

      @Override
      public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {
        try {
          weaver.loadPointcutAsClass(pool, classname);
        } catch (NotFoundException e) {
        } catch (Throwable e) {
          e.printStackTrace();
        }
        for (final TracingTransform transform : transforms) {
          try {
            transform.transform(pool.get(classname), InstrumentationUtils.getHierarchy(pool.get(classname)));
          } catch (NotFoundException e) {
          } catch (Throwable t) {
            t.printStackTrace();
          }
        }
      }
    });

    LastInchOfTrampoline.switchTo(AdviceDispatcher.Type.DEFAULT);
  }

  public static TestingUnlogger create(Tracer[] tracer) {
    try {
      return new TestingUnlogger(tracer);
    } catch (Exception e) {
      throw new RuntimeException("Can't create testing unlogger.", e);
    }
  }

  public void weave(Class<?> klass) {
    try {
      weaver.loadPointcutAsClass(classPool, klass.getName());
    } catch (Exception e) {
      throw new RuntimeException("Can't weave class " + klass + ".", e);
    }
  }

  public Loader getLoader() {
    return loader;
  }

  public void forkTest(String className, String method) {
    try {
      Class<?> clazz = loader.loadClass(className);
      clazz.getMethod(method).invoke(clazz.newInstance());
    } catch (Exception e) {
      throw new RuntimeException("Can't fork test.", e);
    }
  }

}
