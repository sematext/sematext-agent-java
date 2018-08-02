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

public final class SamplerStatistics {

  public static final SamplerStatistics INSTANCE = new SamplerStatistics(10, TimeUnit.SECONDS);

  private volatile double avg;
  private long count;
  private long sum;
  private long timestamp;

  private final long interval;
  private final TimeUnit timeUnit;

  public SamplerStatistics(long interval, TimeUnit timeUnit) {
    this.interval = interval;
    this.timeUnit = timeUnit;
  }

  public void update(long v) {
    Stats.INSTANCE.updateWriteLatency(v);
    long now = System.currentTimeMillis();
    if (timestamp + timeUnit.toMillis(interval) < now) {
      count = 0;
      sum = 0;
      timestamp = now;
    }
    count++;
    sum += v;
    avg = ((double) sum) / count;
  }

  public double average() {
    return avg;
  }
}

