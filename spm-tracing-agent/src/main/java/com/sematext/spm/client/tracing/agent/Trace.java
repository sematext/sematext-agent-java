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

import java.util.Map;

import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.FailureType;
import com.sematext.spm.client.tracing.agent.model.HttpHeaders;
import com.sematext.spm.client.unlogger.JoinPoint;

public interface Trace<T extends Trace> {
  long getTraceId();

  long getCallId();

  long getParentCallId();

  Call getCurrentCall();

  void setResponseHeaders(ResponseHeaders headers);

  void sendCrossAppOutHeaders();

  void newCall(JoinPoint joinPoint);

  void newCall(String signature, long startTimestamp);

  void setStartTimestamp(long startTimestamp);

  void setFailed(Boolean failed);

  void setFailureType(FailureType type);

  void setException(Throwable t);

  void setTransactionParameter(String key, String value);

  Map<String, String> getTransactionParameters();

  void setMethodParameter(String key, String value);

  Map<String, String> getMethodParameters();

  void setExternal(Boolean external);

  void setSkipExternalTracingStatistics(boolean skip);

  void setTag(Call.CallTag tag);

  void setEntryPoint(boolean entryPoint);

  void endCall();

  void endCall(long endTimestamp);

  void ignore();

  void setCrossAppInHeader(HttpHeaders.CrossAppCallHeader header);

  void setAsync(boolean async);

  void setAnnotation(Object annotation);

  <A> A getAnnotation();

  void setTransactionSummary(Object summary);

  Object getTransactionSummary();

  boolean callStackEmpty();

  boolean isLastCall();

  T fork();

  Boolean isSampled();

  void setCrossAppOutInfo(CrossAppOutInfo crossAppOutInfo);

  TransactionNamer getNamer();

  void forceEnd(boolean failed);
}
