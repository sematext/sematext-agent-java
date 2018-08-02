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
import java.util.Map;

public final class Call {

  public static enum CallTag {
    REGULAR, SQL_QUERY, JPA, JSP, EXTERNAL, HTTP_REQUEST, ES, SOLR
  }

  public static enum TransactionType {
    WEB, BACKGROUND
  }

  public static final long ROOT_CALL_ID = 0L;

  private long callId;

  private long parentCallId;

  private int level;

  private long startTimestamp;

  private long endTimestamp;

  private long duration;

  private long selfDuration;

  private String signature;

  private Boolean failed;

  private Boolean external;

  private CallTag callTag;

  private boolean entryPoint;

  private String crossAppToken;

  private Long crossAppCallId;

  private Long crossAppParentCallId;

  private Long crossAppDuration;

  private String crossAppRequest;

  private Endpoint crossAppEndpoint;

  private boolean crossAppSampled;

  private Object annotation;

  private Map<String, String> parameters = new HashMap<String, String>();

  private transient long childDuration;

  private transient Call parent;

  private transient boolean skipExternalTracingStatistics;

  public Call() {
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
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

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public Boolean getFailed() {
    return failed;
  }

  public void setFailed(Boolean failed) {
    this.failed = failed;
  }

  public Boolean getExternal() {
    return external;
  }

  public void setExternal(Boolean external) {
    this.external = external;
  }

  public CallTag getCallTag() {
    return callTag;
  }

  public void setCallTag(CallTag callTag) {
    this.callTag = callTag;
  }

  public Boolean isEntryPoint() {
    return entryPoint;
  }

  public void setEntryPoint(boolean entryPoint) {
    this.entryPoint = entryPoint;
  }

  public long getSelfDuration() {
    return selfDuration;
  }

  public void setSelfDuration(long selfDuration) {
    this.selfDuration = selfDuration;
  }

  public String getCrossAppToken() {
    return crossAppToken;
  }

  public void setCrossAppToken(String crossAppToken) {
    this.crossAppToken = crossAppToken;
  }

  public Long getCrossAppCallId() {
    return crossAppCallId;
  }

  public void setCrossAppCallId(Long crossAppCallId) {
    this.crossAppCallId = crossAppCallId;
  }

  public Long getCrossAppParentCallId() {
    return crossAppParentCallId;
  }

  public void setCrossAppParentCallId(Long crossAppParentCallId) {
    this.crossAppParentCallId = crossAppParentCallId;
  }

  public Long getCrossAppDuration() {
    return crossAppDuration;
  }

  public void setCrossAppDuration(Long crossAppDuration) {
    this.crossAppDuration = crossAppDuration;
  }

  public String getCrossAppRequest() {
    return crossAppRequest;
  }

  public void setCrossAppRequest(String crossAppRequest) {
    this.crossAppRequest = crossAppRequest;
  }

  public Endpoint getCrossAppEndpoint() {
    return crossAppEndpoint;
  }

  public void setCrossAppEndpoint(Endpoint crossAppEndpoint) {
    this.crossAppEndpoint = crossAppEndpoint;
  }

  public boolean isCrossAppSampled() {
    return crossAppSampled;
  }

  public void setCrossAppSampled(boolean crossAppSampled) {
    this.crossAppSampled = crossAppSampled;
  }

  public long getChildDuration() {
    return childDuration;
  }

  public void setChildDuration(long childDuration) {
    this.childDuration = childDuration;
  }

  public Call getParent() {
    return parent;
  }

  public void setParent(Call parent) {
    this.parent = parent;
  }

  public boolean isSkipExternalTracingStatistics() {
    return skipExternalTracingStatistics;
  }

  public void setSkipExternalTracingStatistics(boolean skipExternalTracingStatistics) {
    this.skipExternalTracingStatistics = skipExternalTracingStatistics;
  }

  public Object getAnnotation() {
    return annotation;
  }

  public void setAnnotation(Object annotation) {
    this.annotation = annotation;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  public void copy(Call to) {
    to.setCallId(this.getCallId());
    to.setParentCallId(this.getParentCallId());
    to.setLevel(this.getLevel());
    to.setSignature(this.getSignature());
    to.setStartTimestamp(this.getStartTimestamp());
    to.setEndTimestamp(this.getEndTimestamp());
    to.setDuration(this.getDuration());
    to.setSelfDuration(this.getSelfDuration());
    to.setFailed(this.getFailed());
    to.setExternal(this.getExternal());
    to.setCallTag(this.getCallTag());
    to.setEntryPoint(this.isEntryPoint());
    to.setCrossAppToken(this.getCrossAppToken());
    to.setCrossAppCallId(this.getCrossAppCallId());
    to.setCrossAppParentCallId(this.getCrossAppParentCallId());
    to.setCrossAppDuration(this.getCrossAppDuration());
    to.setCrossAppRequest(this.getCrossAppRequest());
    to.setCrossAppEndpoint(this.getCrossAppEndpoint());
    to.setCrossAppSampled(this.isCrossAppSampled());
    to.setAnnotation(this.getAnnotation());
    to.setSkipExternalTracingStatistics(this.isSkipExternalTracingStatistics());
    for (final Map.Entry<String, String> entry : parameters.entrySet()) {
      to.getParameters().put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public String toString() {
    return "Call{" +
        "callId=" + callId +
        ", parentCallId=" + parentCallId +
        ", level=" + level +
        ", startTimestamp=" + startTimestamp +
        ", endTimestamp=" + endTimestamp +
        ", duration=" + duration +
        ", selfDuration=" + selfDuration +
        ", signature='" + signature + '\'' +
        ", failed=" + failed +
        ", external=" + external +
        ", callTag=" + callTag +
        ", entryPoint='" + entryPoint + '\'' +
        '}';
  }
}
