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
package com.sematext.spm.client.tracing.utils;

import org.junit.runners.BlockJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.LogWriter;
import com.sematext.spm.client.tracing.TestingUnlogger;
import com.sematext.spm.client.tracing.agent.tracer.Tracer;

import javassist.Loader;

public class TracingJUnit4ClassRunner extends BlockJUnit4ClassRunner {

  private static class StdOutLogWriter implements LogWriter<String> {
    @Override
    public void write(String logLine) {
      System.out.println(logLine);
    }

    @Override
    public void write(String logLine, Throwable throwable) {
      System.out.println(logLine);
      throwable.printStackTrace(System.out);
    }
  }

  private static class Initializer {
    static Loader newLoader(Class<?> testClass) throws Exception {
      final TracingContext ctx = testClass.getAnnotation(TracingContext.class);
      if (ctx == null) {
        throw new RuntimeException("Missing TracingContext Annotation.");
      }

      final List<Tracer> tracers = new ArrayList<Tracer>();
      for (final Class<? extends Tracer> klass : ctx.tracers()) {
        tracers.add(klass.newInstance());
      }

      final TestingUnlogger testing = TestingUnlogger.create(tracers.toArray(new Tracer[tracers.size()]));

      LogFactory.init(new StdOutLogWriter());
      LogFactory.setLoggingLevel("DEBUG");

      return testing.getLoader();
    }
  }

  public TracingJUnit4ClassRunner(Class<?> klass) throws Exception {
    super(Class.forName(klass.getName(), true, Initializer.newLoader(klass)));
  }

}
