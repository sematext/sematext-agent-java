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
package com.sematext.spm.client.tracing.agent.servlet;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.model.Call;

public final class AsyncListenerProxy {

  private static final String ASYNC_LISTENER_CLASS = "javax.servlet.AsyncListener";

  private AsyncListenerProxy() {
  }

  private static void completeTransaction(Object asyncEvent, final Long traceId, final Long parentCallId,
                                          final Long startTs, final boolean sampled) {
    final SpmAsyncEventAccess event = (SpmAsyncEventAccess) asyncEvent;
    final SpmHttpServletResponseAccess response = (SpmHttpServletResponseAccess) event.getResponse();

//    response._$spm_tracing$_setHeader("X-SPM-Async-Processed", "true");

    Tracing.newTrace("", Call.TransactionType.WEB, traceId, parentCallId, sampled);
    Tracing.current().newCall("async", startTs);
    Tracing.current().setTag(Call.CallTag.REGULAR);
    Tracing.current().endCall();
    Tracing.endTrace();
  }

  public static Object newAsyncListener(final ClassLoader loader, final Long traceId, final Long parentCallId,
                                        final Long startTs, final boolean sampled)
      throws ClassNotFoundException {
    final Class<?> klass = Class.forName(ASYNC_LISTENER_CLASS, true, loader);
    return Proxy.newProxyInstance(loader, new Class[] { klass }, new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        final String methodName = method.getName();
        if ("onComplete".equals(methodName) || "onError".equals(methodName)) {
          completeTransaction(args[0], traceId, parentCallId, startTs, sampled);
          return null;
        }
        return null;
      }
    });
  }
}
