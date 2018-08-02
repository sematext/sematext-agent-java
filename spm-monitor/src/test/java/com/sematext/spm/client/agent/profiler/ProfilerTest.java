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

package com.sematext.spm.client.agent.profiler;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;

public class ProfilerTest {
  @Test
  public void testShouldRejectWholeStackTraceIfThereAreProfilerMethodCalls() {
    StackTraceElement el0 = new StackTraceElement(List.class.getName(), "add()", "List.java", 20);
    StackTraceElement el1 = new StackTraceElement(Profiler.class.getName(), "profile()", "Profiler.java", 100);
    StackTraceElement el2 = new StackTraceElement(Thread.class.getName(), "run()", "Thread.java", 200);

    StackTraceElement[] processed = Profiler.processStackTrace(true, new StackTraceElement[] { el0, el1, el2 });
    assertEquals(processed.length, 0);
  }

  @Test
  public void testShouldExcludeAgentCallsFromStackTrace() {
    StackTraceElement el0 = new StackTraceElement("com.sematext.spm.client.tracing.pointcut.HttpServletPointcut", "doBefore", "HttpServletPointcut.java", 100);
    StackTraceElement el1 = new StackTraceElement("javax.servlet.http.HttpServlet", "doGet()", "HttpServlet.java", 300);
    StackTraceElement el2 = new StackTraceElement("com.sematext.spm.client.tracing.pointcut.RequestPointcut", "doBefore", "RequestPointcut.java", 100);
    StackTraceElement el3 = new StackTraceElement("org.apache.tomcat.RequestProcessor", "process", "HttpServletPointcut.java", 100);

    StackTraceElement[] processed = Profiler.processStackTrace(true, new StackTraceElement[] { el0, el1, el2, el3 });
    assertEquals(processed.length, 2);
    assertEquals(processed[0].getClassName(), "javax.servlet.http.HttpServlet");
    assertEquals(processed[1].getClassName(), "org.apache.tomcat.RequestProcessor");
  }
}
