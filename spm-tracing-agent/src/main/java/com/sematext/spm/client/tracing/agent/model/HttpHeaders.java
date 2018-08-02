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

public final class HttpHeaders {
  private HttpHeaders() {
  }

  public static final String SPM_TRACING_TRACE_ID = "X-SPM-Tracing-TraceId";
  public static final String SPM_TRACING_CALL_ID = "X-SPM-Tracing-CallId";
  public static final String SPM_TRACING_CROSS_APP_CALL = "X-SPM-Tracing-Cross-App-Call";
  public static final String SPM_TRACING_SAMPLED = "X-SPM-Tracing-Sampled";

  public static String encodeCrossAppCallHeader(Long callId, Long parentCallId, Long traceId, long duration,
                                                String token, String request, Endpoint endpoint, boolean sampled) {
    final StringBuilder header = new StringBuilder();
    header.append(token).append(";");
    header.append(callId).append(";");
    header.append(parentCallId).append(";");
    header.append(traceId).append(";");
    header.append(duration).append(";");
    header.append(request).append(";");
    header.append(endpoint.getAddress()).append(";");
    header.append(endpoint.getHostname()).append(";");
    header.append(sampled);
    return header.toString();
  }

  public static CrossAppCallHeader decodeCrossAppCallHeader(String header) {
    final String[] parts = header.split(";");
    if (parts.length < 8) {
      return null;
    }
    final String token = parts[0];

    final Long callId = parseLongSilently(parts[1]);
    final Long parentCallId = parseLongSilently(parts[2]);
    final Long traceId = parseLongSilently(parts[3]);
    final Long duration = parseLongSilently(parts[4]);
    if (callId == null || parentCallId == null || traceId == null || duration == null) {
      return null;
    }

    final String request = parts[5];
    final Endpoint endpoint = new Endpoint(parts[6], parts[7]);
    boolean sampled = true;
    if (parts.length >= 9) {
      sampled = Boolean.valueOf(parts[8]);
    }
    return new CrossAppCallHeader(token, callId, parentCallId, traceId, duration, request, endpoint, sampled);
  }

  private static Long parseLongSilently(final String v) {
    if (v == null) {
      return null;
    }
    try {
      return Long.parseLong(v);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public static final class CrossAppCallHeader {
    private final String token;
    private final long callId;
    private final long parentCallId;
    private final long traceId;
    private final long duration;
    private final String request;
    private final Endpoint endpoint;
    private final boolean sampled;

    private CrossAppCallHeader(String token, long callId, long parentCallId, long traceId, long duration,
                               String request, Endpoint endpoint, boolean sampled) {
      this.token = token;
      this.callId = callId;
      this.parentCallId = parentCallId;
      this.traceId = traceId;
      this.duration = duration;
      this.request = request;
      this.endpoint = endpoint;
      this.sampled = sampled;
    }

    public String getToken() {
      return token;
    }

    public long getCallId() {
      return callId;
    }

    public long getParentCallId() {
      return parentCallId;
    }

    public long getTraceId() {
      return traceId;
    }

    public long getDuration() {
      return duration;
    }

    public String getRequest() {
      return request;
    }

    public Endpoint getEndpoint() {
      return endpoint;
    }

    public boolean isSampled() {
      return sampled;
    }

    @Override
    public String toString() {
      return "CrossAppCallHeader{" +
          "token='" + token + '\'' +
          ", callId=" + callId +
          ", parentCallId=" + parentCallId +
          ", traceId=" + traceId +
          ", duration=" + duration +
          ", request='" + request + '\'' +
          ", endpoint=" + endpoint +
          ", sampled=" + sampled +
          '}';
    }
  }
}
