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

public class TracingError {
  private String token;
  private Long traceId;
  private Long callId;
  private Long parentCallId;
  private long timestamp;
  private boolean sampled;
  private Map<String, String> parameters;

  public TracingError() {
  }

  public TracingError(String token, Long traceId, Long callId, Long parentCallId, long timestamp, boolean sampled,
                      Map<String, String> parameters) {
    this.token = token;
    this.traceId = traceId;
    this.callId = callId;
    this.parentCallId = parentCallId;
    this.timestamp = timestamp;
    this.sampled = sampled;
    this.parameters = parameters;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Long getTraceId() {
    return traceId;
  }

  public void setTraceId(Long traceId) {
    this.traceId = traceId;
  }

  public Long getCallId() {
    return callId;
  }

  public void setCallId(Long callId) {
    this.callId = callId;
  }

  public Long getParentCallId() {
    return parentCallId;
  }

  public void setParentCallId(Long parentCallId) {
    this.parentCallId = parentCallId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public boolean isSampled() {
    return sampled;
  }

  public void setSampled(boolean sampled) {
    this.sampled = sampled;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  public void copy(TracingError to) {
    to.token = token;
    to.traceId = traceId;
    to.callId = callId;
    to.parentCallId = parentCallId;
    to.timestamp = timestamp;
    to.sampled = sampled;
    to.parameters = new HashMap<String, String>(parameters);
  }
}
