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
package com.sematext.spm.client.tracing.agent.errors;

import java.util.HashMap;
import java.util.Map;

import com.sematext.spm.client.tracing.TracingParameters;
import com.sematext.spm.client.tracing.agent.NoTrace;
import com.sematext.spm.client.tracing.agent.Sink;
import com.sematext.spm.client.tracing.agent.Trace;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.util.Hostname;

public class ErrorsTracker {

  public static void track(Throwable throwable, String message) {
    final Trace<? extends Trace<?>> trace = Tracing.current();
    final String token = ServiceLocator.getConfig().getToken();

    final Long traceId = trace == NoTrace.instance() ? null : trace.getTraceId();
    final Long callId = trace == NoTrace.instance() ? null : trace.getCallId();
    final Long parentCallId = trace == NoTrace.instance() ? null : trace.getParentCallId();
    final boolean sampled = trace.isSampled();
    final Map<String, String> parameters = new HashMap<String, String>(trace.getTransactionParameters());
    if (message != null) {
      parameters.put(TracingParameters.ERROR_MESSAGE.getKey(), message);
    }
    if (throwable != null) {
      parameters.put(TracingParameters.ERROR_CLASS.getKey(), throwable.getClass().getName());
    }
    parameters.put(TracingParameters.HOST.getKey(), Hostname.getLocalEndpoint().getHostname());

    final TracingError error = new TracingError(token, traceId, callId, parentCallId,
                                                System.currentTimeMillis(), sampled, parameters);

    sink(error);
  }

  private static void sink(TracingError tracingError) {
    if (ServiceLocator.getErrorSinks() != null) {
      for (Sink<TracingError> sink : ServiceLocator.getErrorSinks()) {
        if (ServiceLocator.getTracingErrorSampler() == null || ServiceLocator.getTracingErrorSampler()
            .sample(tracingError)) {
          sink.sink(tracingError);
        }
      }
    }
  }
}
