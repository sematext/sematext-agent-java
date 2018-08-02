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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FixedRateSampler implements Sampler<String> {
  private final long intervalMillis;
  private final long target;
  private final AtomicLong timestamp = new AtomicLong(0);
  private final AtomicLong count = new AtomicLong(0);

  public FixedRateSampler(long interval, TimeUnit timeUnit, long target) {
    this.intervalMillis = timeUnit.toMillis(interval);
    this.target = target;
  }

  @Override
  public boolean sample(String request) {
    long now = System.currentTimeMillis();
    long ts = timestamp.get();
    if ((ts + intervalMillis) < now) {
      if (timestamp.compareAndSet(ts, now)) {
        count.set(0);
      }
    }
    Stats.INSTANCE.addTotal();
    if (count.incrementAndGet() > target) {
      Stats.INSTANCE.addSampled();
      return false;
    }
    return true;
  }
}
