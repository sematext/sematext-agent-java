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
package com.sematext.spm.client.tracing.agent.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.tracing.agent.model.Call.TransactionType;

public final class PartialTransaction {
  private long callId;
  private long parentCallId;
  private long traceId;
  private String request;
  private long startTimestamp;
  private long endTimestamp;
  private long duration;
  private String token;
  private boolean failed;
  private boolean entryPoint;
  private Endpoint endpoint;
  private boolean asynchronous;
  private TransactionType transactionType;
  private Object transactionSummary;
  private List<Call> calls;
  private FailureType failureType;
  private Throwable exceptionStackTrace;
  private Map<String, String> parameters = new HashMap<String, String>();

  public long getCallId() {
    return callId;
  }

  public void setCallId(long callId) {
    this.callId = callId;
  }

  public long getParentCallId() {
    return parentCallId;
  }

  public void setParentCallId(long parentCallId) {
    this.parentCallId = parentCallId;
  }

  public long getTraceId() {
    return traceId;
  }

  public void setTraceId(long traceId) {
    this.traceId = traceId;
  }

  public String getRequest() {
    return request;
  }

  public void setRequest(String request) {
    this.request = request;
  }

  public long getStartTimestamp() {
    return startTimestamp;
  }

  public void setStartTimestamp(long startTimestamp) {
    this.startTimestamp = startTimestamp;
  }

  public long getEndTimestamp() {
    return endTimestamp;
  }

  public void setEndTimestamp(long endTimestamp) {
    this.endTimestamp = endTimestamp;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public boolean isFailed() {
    return failed;
  }

  public void setFailed(boolean failed) {
    this.failed = failed;
  }

  public boolean isEntryPoint() {
    return entryPoint;
  }

  public void setEntryPoint(boolean entryPoint) {
    this.entryPoint = entryPoint;
  }

  public Endpoint getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(Endpoint endpoint) {
    this.endpoint = endpoint;
  }

  public boolean isAsynchronous() {
    return asynchronous;
  }

  public void setAsynchronous(boolean asynchronous) {
    this.asynchronous = asynchronous;
  }

  public TransactionType getTransactionType() {
    return transactionType;
  }

  public void setTransactionType(TransactionType transactionType) {
    this.transactionType = transactionType;
  }

  public Object getTransactionSummary() {
    return transactionSummary;
  }

  public void setTransactionSummary(Object transactionSummary) {
    this.transactionSummary = transactionSummary;
  }

  public List<Call> getCalls() {
    return calls;
  }

  public void setCalls(List<Call> calls) {
    this.calls = calls;
  }

  public FailureType getFailureType() {
    return failureType;
  }

  public void setFailureType(FailureType failureType) {
    this.failureType = failureType;
  }

  public Throwable getExceptionStackTrace() {
    return exceptionStackTrace;
  }

  public void setExceptionStackTrace(Throwable exceptionStackTrace) {
    this.exceptionStackTrace = exceptionStackTrace;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  public void copy(PartialTransaction to) {
    to.setCallId(getCallId());
    to.setParentCallId(getParentCallId());
    to.setTraceId(getTraceId());
    to.setRequest(getRequest());
    to.setStartTimestamp(getStartTimestamp());
    to.setEndTimestamp(getEndTimestamp());
    to.setDuration(getDuration());
    to.setToken(getToken());
    to.setFailed(isFailed());
    to.setEntryPoint(isEntryPoint());
    to.setEndpoint(getEndpoint());
    to.setAsynchronous(isAsynchronous());
    to.setTransactionType(getTransactionType());
    to.setTransactionSummary(getTransactionSummary());
    to.setCalls(getCalls());
    to.setFailureType(getFailureType());
    to.setExceptionStackTrace(getExceptionStackTrace());
    if (to.getParameters() == null) {
      to.setParameters(new HashMap<String, String>());
    }
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      to.getParameters().put(entry.getKey(), entry.getValue());
    }
  }
}
