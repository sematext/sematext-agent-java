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

public final class CrossAppOutInfo {
  private final long traceId;
  private final long callId;
  private final long parentCallId;
  private final long startTs;

  private CrossAppOutInfo(long traceId, long callId, long parentCallId, long startTs) {
    this.traceId = traceId;
    this.callId = callId;
    this.parentCallId = parentCallId;
    this.startTs = startTs;
  }

  public long getTraceId() {
    return traceId;
  }

  public long getCallId() {
    return callId;
  }

  public long getParentCallId() {
    return parentCallId;
  }

  public long getStartTs() {
    return startTs;
  }

  public static CrossAppOutInfo create(Trace trace) {
    return new CrossAppOutInfo(trace.getTraceId(), trace.getCallId(), trace.getParentCallId(), System
        .currentTimeMillis());
  }

  @Override
  public String toString() {
    return "CrossAppOutInfo{" +
        "traceId=" + traceId +
        ", callId=" + callId +
        ", parentCallId=" + parentCallId +
        ", startTs=" + startTs +
        '}';
  }
}
