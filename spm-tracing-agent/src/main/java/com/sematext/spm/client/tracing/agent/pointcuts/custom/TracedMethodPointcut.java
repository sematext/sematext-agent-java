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
package com.sematext.spm.client.tracing.agent.pointcuts.custom;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.tracing.agent.NoTrace;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;
import com.sematext.spm.client.util.ReflectionUtils;

@LoggerPointcuts(name = "logger:traced-method", methodAnnotations = {
    "com.sematext.spm.client.tracing.Trace"
})
public class TracedMethodPointcut implements UnloggableLogger {
  private static final Log LOG = LogFactory.getLog(TracedMethodPointcut.class);

  @Override
  public void logBefore(LoggerContext context) {
    boolean force = false;
    try {
      Class[] types = context.getJoinPoint().getParameterTypes(context.getThat().getClass().getClassLoader());
      Method method = ReflectionUtils
          .getMethod(context.getThat().getClass(), context.getJoinPoint().getShortName(), types);
      if (method != null) {
        Annotation traced = null;
        for (Annotation annotation : method.getDeclaredAnnotations()) {
          if (annotation.annotationType().getName().equals("com.sematext.spm.client.tracing.Trace")) {
            traced = annotation;
            break;
          }
        }
        if (traced != null) {
          Method forceMethod = ReflectionUtils.getMethod(traced.annotationType(), "force");
          if (forceMethod != null) {
            force = (Boolean) forceMethod.invoke(traced);
          } else {
            LOG.error("Missing 'force' parameter for annotation " + traced + ".");
          }
        } else {
          LOG.error("Can't get @Traced annotation for method " + context.getJoinPoint().getShortName() + ".");
        }
      } else {
        LOG.error("Can't get method: " + context.getJoinPoint().getShortName() + ".");
      }
    } catch (Exception e) {
      LOG.error("Can't get annotation parameter.", e);
    }

    boolean entryPoint = false;
    if (Tracing.current() == NoTrace.instance() && force) {
      Tracing.newTrace(context.getJoinPoint().getShortName(), Call.TransactionType.BACKGROUND);
      Tracing.current().getNamer().asFramework(context.getJoinPoint());
      entryPoint = true;
    }
    Tracing.current().newCall(context.getJoinPoint());
    Tracing.current().setTag(Call.CallTag.REGULAR);
    Tracing.current().setEntryPoint(entryPoint);
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
    Tracing.current().endCall();
    if (Tracing.current().callStackEmpty()) {
      Tracing.endTrace();
    }
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
    Tracing.current().endCall();
    if (Tracing.current().callStackEmpty()) {
      Tracing.endTrace();
    }
  }
}
