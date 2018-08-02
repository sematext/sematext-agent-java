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

import com.sematext.spm.client.tracing.agent.NoTrace;
import com.sematext.spm.client.tracing.agent.Trace;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.es.transport.ESAsyncActions;
import com.sematext.spm.client.tracing.agent.es.transport.SpmListenableActionFutureAccess;
import com.sematext.spm.client.tracing.agent.model.annotation.ESAnnotation;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;

@LoggerPointcuts(name = "es:plain-listenable-action-future", constructors = {
    "org.elasticsearch.action.support.PlainListenableActionFuture(boolean p, org.elasticsearch.threadpool.ThreadPool pool)"
})
public class PlainListenableActionFuturePointcut implements UnloggableLogger {
  @Override
  public void logBefore(LoggerContext context) {
    final Trace trace = Tracing.current();

    if (trace != NoTrace.instance() && trace.getCurrentCall() != null && trace.getAnnotation() != null) {
      final SpmListenableActionFutureAccess future = (SpmListenableActionFutureAccess) context.getThat();

      final ESAnnotation annotation = Tracing.current().getAnnotation();

      /**
       * make copy of actions here? is it possible to update es actions after request was built?
       */
      final ESAsyncActions asyncActions = new ESAsyncActions(annotation,
                                                             trace.getTraceId(), trace.getCallId(), System
                                                                 .currentTimeMillis(),
                                                             trace.isSampled());

      future._$spm_tracing$_setESAsyncActions(asyncActions);
    }
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
  }
}
