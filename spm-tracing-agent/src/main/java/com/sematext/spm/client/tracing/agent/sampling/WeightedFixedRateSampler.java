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
package com.sematext.spm.client.tracing.agent.sampling;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class WeightedFixedRateSampler implements Sampler<String> {

  private final ConcurrentHashMap<String, AtomicInteger> requestsCount = new ConcurrentHashMap<String, AtomicInteger>();
  private final AtomicInteger total = new AtomicInteger(0);
  private final AtomicLong timestamp = new AtomicLong(0);
  private final long limit;
  private final long rareCallsLimit;
  private final long intervalMillis;

  public WeightedFixedRateSampler(long limit, long rareCallsLimit, long interval, TimeUnit timeUnit) {
    this.limit = limit;
    this.rareCallsLimit = rareCallsLimit;
    this.intervalMillis = timeUnit.toMillis(interval);
  }

  void cleanup() {
    requestsCount.clear();
    total.set(0);
  }

  @Override
  public boolean sample(String request) {
    long now = System.currentTimeMillis();
    long ts = timestamp.get();
    if ((ts + intervalMillis) < now) {
      if (timestamp.compareAndSet(ts, now)) {
        cleanup();
      }
    }
    AtomicInteger count = requestsCount.putIfAbsent(request, new AtomicInteger(0));
    if (count == null) {
      count = requestsCount.get(request);
    }
    Stats.INSTANCE.addTotal();
    if (total.incrementAndGet() > limit
        && count.incrementAndGet() > rareCallsLimit) {
      Stats.INSTANCE.addSampled();
      return false;
    }
    return true;
  }
}
