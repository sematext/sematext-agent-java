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
package com.sematext.spm.client.tracing.agent.pointcuts.es.transport;

import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.es.transport.ESAsyncActions;
import com.sematext.spm.client.tracing.agent.es.transport.SpmListenableActionFutureAccess;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;

@LoggerPointcuts(name = "es:adapter-action-future-pointcut", methods = {
    "void org.elasticsearch.action.support.AdapterActionFuture#onResponse(java.lang.Object r)",
    "void org.elasticsearch.action.support.AdapterActionFuture#onThrowable(java.lang.Throwable r)"
})
public class AdapterActionFuturePointcut implements UnloggableLogger {
  @Override
  public void logBefore(LoggerContext context) {
    final SpmListenableActionFutureAccess future = (SpmListenableActionFutureAccess) context.getThat();
    final ESAsyncActions asyncAction = future._$spm_tracing$_getESAsyncActions();
    if (asyncAction != null) {
      Tracing.newTrace("/", Call.TransactionType.BACKGROUND, asyncAction.getTraceId(), asyncAction
          .getParentCallId(), asyncAction.isSampled(), true);
      Tracing.current().newCall("Async", asyncAction.getStartTs());
      Tracing.current().setTag(Call.CallTag.ES);
      Tracing.current().setAsync(true);
      Tracing.current().setAnnotation(asyncAction.getAnnotation());
      if (context.getJoinPoint().getShortName().contains("onThrowable")) {
        Tracing.current().setFailed(true);
      }
      Tracing.current().endCall();
      Tracing.endTrace();
    }
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
  }
}
