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
package com.sematext.spm.client.tracing.agent.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AsyncContext {
  private final Long traceId;
  private final Long parentCallId;
  private final boolean sampled;

  private static final class IdentityKey {
    private final Object key;
    private final int hashCode;

    public IdentityKey(Object key) {
      this.key = key;
      this.hashCode = System.identityHashCode(key);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      return this.key == ((IdentityKey) obj).key;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }
  }

  private AsyncContext(Long traceId, Long parentCallId, boolean sampled) {
    this.traceId = traceId;
    this.parentCallId = parentCallId;
    this.sampled = sampled;
  }

  public Long getTraceId() {
    return traceId;
  }

  public Long getParentCallId() {
    return parentCallId;
  }

  public boolean isSampled() {
    return sampled;
  }

  private static final Map<IdentityKey, AsyncContext> CONTEXT = new ConcurrentHashMap<IdentityKey, AsyncContext>();

  public static AsyncContext create(Object th, Long traceId, Long parentCallId, boolean sampled) {
    final AsyncContext ctx = new AsyncContext(traceId, parentCallId, sampled);
    CONTEXT.put(new IdentityKey(th), ctx);
    return ctx;
  }

  public static AsyncContext get(Object th) {
    return CONTEXT.get(new IdentityKey(th));
  }

  public static AsyncContext clean(Object th) {
    return CONTEXT.remove(new IdentityKey(th));
  }

  @Override
  public String toString() {
    return "AsyncContext{" +
        "traceId=" + traceId +
        ", parentCallId=" + parentCallId +
        ", sampled=" + sampled +
        '}';
  }
}
