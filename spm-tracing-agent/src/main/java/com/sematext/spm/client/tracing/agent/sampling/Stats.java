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

import java.util.concurrent.atomic.AtomicLong;

public final class Stats {
  public static final Stats INSTANCE = new Stats();

  private final AtomicLong sampled = new AtomicLong(0);
  private final AtomicLong total = new AtomicLong(0);
  private final AtomicLong writeCount = new AtomicLong(0);
  private final AtomicLong writeLatencySum = new AtomicLong(0);
  private final AtomicLong sinkCount = new AtomicLong(0);
  private final AtomicLong sinkLatencySum = new AtomicLong(0);

  public void addSampled() {
    sampled.incrementAndGet();
  }

  public void addTotal() {
    total.incrementAndGet();
  }

  public long getSampled() {
    return sampled.get();
  }

  public long getTotal() {
    return total.get();
  }

  public void updateWriteLatency(long writeLatency) {
    writeCount.incrementAndGet();
    writeLatencySum.addAndGet(writeLatency);
  }

  public double getAvgWriteLatency() {
    long sum = writeLatencySum.get();
    long count = writeCount.get();
    if (count == 0) {
      return 0d;
    }
    return ((double) sum) / count;
  }

  public void updateSinkLatency(long sinkLatency) {
    sinkCount.incrementAndGet();
    sinkLatencySum.addAndGet(sinkLatency);
  }

  public double getAvgSinkLatency() {
    long sum = sinkLatencySum.get();
    long count = sinkCount.get();
    if (count == 0) {
      return 0d;
    }
    return ((double) sum) / count;
  }

}
