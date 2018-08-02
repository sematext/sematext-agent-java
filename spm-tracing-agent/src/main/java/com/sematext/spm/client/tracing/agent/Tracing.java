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
package com.sematext.spm.client.tracing.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.Call.TransactionType;
import com.sematext.spm.client.tracing.agent.model.Endpoint;
import com.sematext.spm.client.tracing.agent.model.FailureType;
import com.sematext.spm.client.tracing.agent.model.HttpHeaders;
import com.sematext.spm.client.tracing.agent.model.HttpHeaders.CrossAppCallHeader;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.util.AsyncContext;
import com.sematext.spm.client.tracing.agent.util.Hostname;
import com.sematext.spm.client.unlogger.JoinPoint;

public final class Tracing {

  private static final Log LOG = LogFactory.getLog(Tracing.class);

  private static final ThreadLocal<Random> RANDOM = new ThreadLocal<Random>() {
    @Override
    protected Random initialValue() {
      return new Random();
    }
  };

  private static final ThreadLocal<Trace> CONTEXT = new ThreadLocal<Trace>() {
    @Override
    protected Trace initialValue() {
      return NoTrace.instance();
    }
  };

  private static final ThreadLocal<CallStack> CALL_STACK_POOL = new ThreadLocal<CallStack>() {
    @Override
    protected CallStack initialValue() {
      return new CallStack(ServiceLocator.getConfig().getStackSizeThreshold());
    }
  };

  private static final ThreadLocal<CallArray> CALL_ARRAY_POOL = new ThreadLocal<CallArray>() {
    @Override
    protected CallArray initialValue() {
      return new CallArray(100);
    }
  };

  private static final Endpoint LOCAL_ENDPOINT = Hostname.getLocalEndpoint();

  public static Trace newTrace(String request, TransactionType type) {
    return newTrace(request, type, RANDOM.get().nextLong(), Call.ROOT_CALL_ID);
  }

  public static Trace newTrace(String request, TransactionType type, long traceId, long parentCallId) {
    return newTrace(request, type, traceId, parentCallId, ServiceLocator.getTransactionSampler().sample(request));
  }

  public static Trace newTrace(String request, TransactionType type, long traceId, long parentCallId, boolean sample) {
    return newTrace(request, type, traceId, parentCallId, sample, false);
  }

  public static Trace newTrace(String request, TransactionType type, long traceId, long parentCallId, boolean sample,
                               boolean forked) {
    final ActiveTrace trace = new ActiveTrace(request, type, traceId, parentCallId, sample, forked);
    CONTEXT.set(trace);
    return trace;
  }

  public static AsyncContext registerAsyncTrace(Object worker) {
    // TODO mark root call for async trace (call.async = true), so in frontend timing can be recalculated
    final Trace trace = Tracing.current();
    if (trace != NoTrace.instance()) {
      final long parentCallId;
      if (trace.getCurrentCall() == null) {
        parentCallId = trace.getParentCallId();
      } else {
        parentCallId = trace.getCallId();
      }
      return AsyncContext.create(worker, trace.getTraceId(), parentCallId, trace.isSampled());
    }
    return null;
  }

  public static void setTrace(Trace trace) {
    CONTEXT.set(trace);
  }

  public static void endTrace() {
    CONTEXT.remove();
  }

  public static Trace<? extends Trace<?>> current() {
    return CONTEXT.get();
  }

  public static class ActiveTrace implements Trace<ActiveTrace> {
    private final long traceId;
    private final long parentCallId;
    private final CallStack stack;
    private final String request;
    private final boolean sampled;
    private final CallArray sunkCalls;
    private final boolean forked;
    private final TransactionType type;
    private ResponseHeaders responseHeaders;
    private CrossAppOutInfo crossAppOutInfo;
    private Object transactionSummary;
    private final TransactionNamer namer = new TransactionNamer();
    private boolean async;
    private FailureType failureType;
    private Throwable exception;
    private boolean ignore;
    private final Map<String, String> parameters = new HashMap<String, String>();

    public ActiveTrace(String request, TransactionType type, long traceId, long parentCallId, boolean sampled,
                       boolean forked) {
      this.traceId = traceId;
      this.parentCallId = parentCallId;
      this.stack = CALL_STACK_POOL.get();
      this.stack.clear();
      this.request = request;
      this.type = type;
      this.sampled = sampled;
      this.forked = forked;
      this.sunkCalls = CALL_ARRAY_POOL.get();
      this.sunkCalls.clean();
      this.async = false;
      this.failureType = null;
      this.exception = null;
      this.ignore = false;
    }

    @Override
    public Boolean isSampled() {
      return sampled;
    }

    @Override
    public long getTraceId() {
      return traceId;
    }

    @Override
    public long getCallId() {
      final Call currentCall = getCurrentCall();
      if (currentCall != null) {
        return currentCall.getCallId();
      }
      return Call.ROOT_CALL_ID;
    }

    @Override
    public long getParentCallId() {
      return parentCallId;
    }

    @Override
    public Call getCurrentCall() {
      return stack.peek();
    }

    @Override
    public void setCrossAppOutInfo(CrossAppOutInfo crossAppOutInfo) {
      this.crossAppOutInfo = crossAppOutInfo;
    }

    @Override
    public void setResponseHeaders(ResponseHeaders metadata) {
      this.responseHeaders = metadata;
    }

    @Override
    public void sendCrossAppOutHeaders() {
      if (this.responseHeaders != null && this.crossAppOutInfo != null) {
        long duration = System.currentTimeMillis() - crossAppOutInfo.getStartTs();
        boolean sampled = duration >= ServiceLocator.getConfig().getDurationThresholdMillis();
        final String header = HttpHeaders.encodeCrossAppCallHeader(
            crossAppOutInfo.getCallId(),
            crossAppOutInfo.getParentCallId(),
            crossAppOutInfo.getTraceId(),
            duration,
            ServiceLocator.getConfig().getToken(),
            request,
            LOCAL_ENDPOINT,
            sampled
        );
        this.responseHeaders.addHeader(HttpHeaders.SPM_TRACING_CROSS_APP_CALL, header);
      }
    }

    @Override
    public void newCall(JoinPoint jp) {
      newCall(String.format("%s#%s()", jp.getType(), jp.getShortName()), System.currentTimeMillis());
    }

    @Override
    public void newCall(String signature, long startTime) {
      final Call currentCall = getCurrentCall();

      if (stack.size() >= ServiceLocator.getConfig().getStackSizeThreshold()) {
        LOG.warn(String.format("Call stack threshold [%s] exceeded for token %s", ServiceLocator.getConfig()
            .getStackSizeThreshold(), ServiceLocator.getConfig().getToken()));
        return;
      }

      final Call call = stack.push();
      call.setChildDuration(0);
      call.setSignature(signature);
      call.setStartTimestamp(startTime);
      call.setLevel(stack.size());
      call.setCallId(RANDOM.get().nextLong());
      call.setCallTag(Call.CallTag.REGULAR);
      call.setParent(null);
      call.setEntryPoint(false);
      call.setCrossAppToken(null);
      call.setCrossAppCallId(null);
      call.setCrossAppParentCallId(null);
      call.setCrossAppDuration(null);
      call.setAnnotation(null);
      call.setFailed(false);
      call.setSkipExternalTracingStatistics(false);
      call.getParameters().clear();

      if (currentCall != null) {
        call.setParentCallId(currentCall.getCallId());
        call.setParent(currentCall);
      } else {
        call.setParentCallId(parentCallId);
      }
    }

    @Override
    public void setStartTimestamp(long startTimestamp) {
      final Call currentCall = getCurrentCall();
      if (currentCall != null) {
        currentCall.setStartTimestamp(startTimestamp);
      }
    }

    @Override
    public void setFailed(Boolean failed) {
      final Call currentCall = getCurrentCall();
      if (currentCall != null) {
        currentCall.setFailed(failed);
      }
    }

    @Override
    public void setFailureType(FailureType type) {
      this.failureType = type;
    }

    @Override
    public void setException(Throwable t) {
      this.exception = t;
    }

    @Override
    public void setTransactionParameter(String key, String value) {
      this.parameters.put(key, value);
    }

    @Override
    public Map<String, String> getTransactionParameters() {
      return this.parameters;
    }

    @Override
    public void setMethodParameter(String key, String value) {
      final Call currentCall = getCurrentCall();
      if (currentCall != null) {
        currentCall.getParameters().put(key, value);
      }
    }

    @Override
    public Map<String, String> getMethodParameters() {
      final Call currentCall = getCurrentCall();
      if (currentCall != null) {
        return currentCall.getParameters();
      }
      return Collections.emptyMap();
    }

    @Override
    public void setExternal(Boolean external) {
      final Call currentCall = getCurrentCall();
      if (currentCall != null) {
        currentCall.setExternal(external);
      }
    }

    @Override
    public void setSkipExternalTracingStatistics(boolean skip) {
      final Call currentCall = getCurrentCall();
      if (currentCall != null) {
        currentCall.setSkipExternalTracingStatistics(skip);
      }
    }

    @Override
    public void setTag(Call.CallTag tag) {
      final Call currentCall = getCurrentCall();
      if (currentCall != null) {
        currentCall.setCallTag(tag);
      }
    }

    @Override
    public void setEntryPoint(boolean entryPoint) {
      final Call currentCall = getCurrentCall();
      if (currentCall != null) {
        currentCall.setEntryPoint(entryPoint);
      }
    }

    @Override
    public void setCrossAppInHeader(final CrossAppCallHeader header) {
      final Call currentCall = getCurrentCall();
      if (currentCall != null && header != null) {
        currentCall.setCrossAppToken(header.getToken());
        currentCall.setCrossAppCallId(header.getCallId());
        currentCall.setCrossAppParentCallId(header.getParentCallId());
        currentCall.setCrossAppDuration(header.getDuration());
        currentCall.setCrossAppEndpoint(header.getEndpoint());
        currentCall.setCrossAppRequest(header.getRequest());
        currentCall.setCrossAppSampled(header.isSampled());
      }
    }

    @Override
    public void setAsync(boolean async) {
      this.async = async;
    }

    @Override
    public void setAnnotation(Object annotation) {
      final Call currentCall = getCurrentCall();
      if (currentCall != null) {
        currentCall.setAnnotation(annotation);
      }
    }

    @Override
    public <A> A getAnnotation() {
      final Call currentCall = getCurrentCall();
      if (currentCall != null) {
        return (A) currentCall.getAnnotation();
      }
      return null;
    }

    @Override
    public void setTransactionSummary(Object summary) {
      this.transactionSummary = summary;
    }

    @Override
    public Object getTransactionSummary() {
      return transactionSummary;
    }

    @Override
    public void endCall() {
      endCall(System.currentTimeMillis());
    }

    @Override
    public void ignore() {
      this.ignore = true;
    }

    @Override
    public TransactionNamer getNamer() {
      return namer;
    }

    private PartialTransaction createTransaction() {
      final PartialTransaction transaction = new PartialTransaction();
      final Call rootCall = sunkCalls.get(sunkCalls.size() - 1);
      transaction.setCallId(rootCall.getCallId());
      transaction.setParentCallId(rootCall.getParentCallId());
      transaction.setTraceId(traceId);
      transaction.setEndpoint(LOCAL_ENDPOINT);
      transaction.setAsynchronous(async);
      transaction.setTransactionType(type);
      transaction.setTransactionSummary(transactionSummary);
      transaction.setStartTimestamp(rootCall.getStartTimestamp());
      transaction.setEndTimestamp(rootCall.getEndTimestamp());
      transaction.setDuration(rootCall.getDuration());
      transaction.setFailed(rootCall.getFailed());
      if (forked) {
        transaction.setRequest(request);
      } else {
        transaction.setRequest(namer.getName());
      }
      transaction.setToken(ServiceLocator.getConfig().getToken());
      transaction.setEntryPoint(rootCall.isEntryPoint());
      transaction.setExceptionStackTrace(exception);
      transaction.setFailureType(failureType);
      transaction.setParameters(parameters);

      final List<Call> calls = new ArrayList<Call>();
      for (int i = 0; i < sunkCalls.size(); i++) {
        final Call call = new Call();
        sunkCalls.get(i).copy(call);
        calls.add(call);
      }

      transaction.setCalls(calls);

      return transaction;
    }

    @Override
    public void endCall(long endTimestamp) {
      final Call currentCall = getCurrentCall();
      if (currentCall != null) {
        currentCall.setEndTimestamp(endTimestamp);
        final long duration = endTimestamp - currentCall.getStartTimestamp();
        currentCall.setDuration(duration);
        currentCall.setSelfDuration(duration - currentCall.getChildDuration());
        if (currentCall.getParent() != null) {
          long childDuration = currentCall.getParent().getChildDuration();

          currentCall.getParent().setChildDuration(childDuration + duration);
          currentCall.setParent(null);
        }

        currentCall.copy(sunkCalls.add());
      }
      stack.pop();

      if (stack.isEmpty()) {
        if (!ignore) {
          final PartialTransaction transaction = createTransaction();
          ServiceLocator.getTracingStatistics().record(transaction);

          if (currentCall != null && (forked || currentCall.getDuration() >= ServiceLocator.getConfig()
              .getDurationThresholdMillis())) {
            sinkTransaction(transaction);
          }

          if (currentCall == null && LOG.isDebugEnabled()) {
            LOG.debug(
                "Transaction will not be sinked because current call is null, endpoint: " + transaction.getEndpoint());
          }

          if (currentCall != null && LOG.isDebugEnabled()) {
            LOG.debug("Transaction will not be sinked because it is under threshold, duration is: " + currentCall
                .getDuration() + ", endpoint is: " + transaction.getEndpoint());
          }
        }

        sunkCalls.clean();
      }

    }

    private void sinkTransaction(PartialTransaction transaction) {
      if (sampled) {
        for (Sink<PartialTransaction> sink : ServiceLocator.getTransactionSinks()) {
          try {
            sink.sink(transaction);
          } catch (Throwable e) {
            LOG.warn("Can't sink transaction", e);
          }
        }
      }
    }

    @Override
    public boolean callStackEmpty() {
      return stack.isEmpty();
    }

    @Override
    public boolean isLastCall() {
      return stack.size() == 1;
    }

    @Override
    public ActiveTrace fork() {
      return new ActiveTrace(request, type, getTraceId(), getCallId(), sampled, true);
    }

    @Override
    public void forceEnd(boolean failed) {
      while (stack.size() > 1) {
        endCall();
      }
      if (!stack.isEmpty()) {
        stack.peek().setFailed(failed);
        endCall();
      }
    }

    @Override
    public String toString() {
      return "ActiveTrace{" +
          "traceId=" + traceId +
          ", parentCallId=" + parentCallId +
          ", request='" + request + '\'' +
          ", sampled=" + sampled +
          ", forked=§" + forked +
          ", type=" + type +
          ", responseHeaders=" + responseHeaders +
          ", crossAppOutInfo=" + crossAppOutInfo +
          ", async=" + async +
          ", failureType=" + failureType +
          '}';
    }
  }
}
