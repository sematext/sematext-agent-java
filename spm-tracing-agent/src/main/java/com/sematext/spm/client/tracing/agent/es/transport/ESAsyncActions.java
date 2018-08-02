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
package com.sematext.spm.client.tracing.agent.es.transport;

import com.sematext.spm.client.tracing.agent.model.annotation.ESAnnotation;

public final class ESAsyncActions {
  private final Long traceId;
  private final Long parentCallId;
  private final Long startTs;
  private final boolean sampled;
  private final ESAnnotation annotation;

  public ESAsyncActions(ESAnnotation annotation, Long traceId, Long parentCallId, Long startTs, boolean sampled) {
    this.annotation = annotation;
    this.traceId = traceId;
    this.parentCallId = parentCallId;
    this.startTs = startTs;
    this.sampled = sampled;
  }

  public ESAnnotation getAnnotation() {
    return annotation;
  }

  public Long getTraceId() {
    return traceId;
  }

  public Long getParentCallId() {
    return parentCallId;
  }

  public Long getStartTs() {
    return startTs;
  }

  public boolean isSampled() {
    return sampled;
  }
}
