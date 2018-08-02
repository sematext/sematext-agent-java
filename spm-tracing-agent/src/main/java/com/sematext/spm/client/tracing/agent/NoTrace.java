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

import java.util.Collections;
import java.util.Map;

import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.FailureType;
import com.sematext.spm.client.tracing.agent.model.HttpHeaders;
import com.sematext.spm.client.unlogger.JoinPoint;

public final class NoTrace implements Trace<NoTrace> {
  private static final NoTrace INSTANCE = new NoTrace();

  private NoTrace() {
  }

  public static NoTrace instance() {
    return INSTANCE;
  }

  @Override
  public Boolean isSampled() {
    return true;
  }

  @Override
  public long getTraceId() {
    return 0;
  }

  @Override
  public long getCallId() {
    return 0;
  }

  @Override
  public long getParentCallId() {
    return 0;
  }

  @Override
  public Call getCurrentCall() {
    return null;
  }

  @Override
  public void setResponseHeaders(ResponseHeaders metadata) {

  }

  @Override
  public void sendCrossAppOutHeaders() {

  }

  @Override
  public void newCall(JoinPoint joinPoint) {

  }

  @Override
  public void newCall(String signature, long startTimestamp) {

  }

  @Override
  public void setStartTimestamp(long startTimestamp) {

  }

  @Override
  public void setFailed(Boolean failed) {

  }

  @Override
  public void setFailureType(FailureType type) {

  }

  @Override
  public void setException(Throwable t) {

  }

  @Override
  public void setTransactionParameter(String key, String value) {

  }

  @Override
  public Map<String, String> getTransactionParameters() {
    return Collections.emptyMap();
  }

  @Override
  public Map<String, String> getMethodParameters() {
    return Collections.emptyMap();
  }

  @Override
  public void setMethodParameter(String key, String value) {

  }

  @Override
  public void setExternal(Boolean external) {

  }

  @Override
  public void setSkipExternalTracingStatistics(boolean skip) {

  }

  @Override
  public void setTag(Call.CallTag tag) {

  }

  @Override
  public void setEntryPoint(boolean entryPoint) {

  }

  @Override
  public void endCall() {

  }

  @Override
  public void endCall(long endTimestamp) {

  }

  @Override
  public void ignore() {

  }

  @Override
  public void setCrossAppInHeader(HttpHeaders.CrossAppCallHeader header) {

  }

  @Override
  public boolean callStackEmpty() {
    return true;
  }

  @Override
  public boolean isLastCall() {
    return false;
  }

  @Override
  public NoTrace fork() {
    return INSTANCE;
  }

  @Override
  public void setCrossAppOutInfo(CrossAppOutInfo crossAppOutInfo) {

  }

  @Override
  public void setAsync(boolean async) {

  }

  @Override
  public void setAnnotation(Object annotation) {

  }

  @Override
  public <A> A getAnnotation() {
    return null;
  }

  @Override
  public void setTransactionSummary(Object summary) {

  }

  @Override
  public Object getTransactionSummary() {
    return null;
  }

  private static final TransactionNamer NAMER = new TransactionNamer();

  @Override
  public TransactionNamer getNamer() {
    return NAMER;
  }

  @Override
  public void forceEnd(boolean failed) {

  }
}
