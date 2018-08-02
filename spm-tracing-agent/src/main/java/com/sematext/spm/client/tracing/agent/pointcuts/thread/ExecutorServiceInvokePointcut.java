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
package com.sematext.spm.client.tracing.agent.pointcuts.thread;

import java.util.Collection;

import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;

@LoggerPointcuts(name = "executor-service-invoke-pointcut", methods = {
    "java.util.List java.util.concurrent.ExecutorService#invokeAll(java.util.Collection tasks)",
    "java.util.List java.util.concurrent.ExecutorService#invokeAll(java.util.Collection tasks, long timeout, java.util.concurrent.TimeUnit unit)",
    "java.lang.Object java.util.concurrent.ExecutorService#invokeAny(java.util.Collection tasks)",
    "java.lang.Object java.util.concurrent.ExecutorService#invokeAny(java.util.Collection tasks, long timeout, java.util.concurrent.TimeUnit unit)"
})
public class ExecutorServiceInvokePointcut implements UnloggableLogger {
  @Override
  public void logBefore(LoggerContext context) {
    Collection tasks = (Collection) context.getAllParams()[0];
    if (tasks != null) {
      for (Object task : tasks) {
        Tracing.registerAsyncTrace(task);
      }
    }
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
  }
}
