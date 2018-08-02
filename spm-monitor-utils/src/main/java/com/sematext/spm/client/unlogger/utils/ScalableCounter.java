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
package com.sematext.spm.client.unlogger.utils;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * Add CPULocal support (and don't forget about false sharing).
 */
public class ScalableCounter {

  private static class PartitionCounter {
    private volatile long counter;
    private static final AtomicLongFieldUpdater<PartitionCounter> UPDATER = AtomicLongFieldUpdater.newUpdater(
        PartitionCounter.class, "counter");

    public void add(long val) {
      long counter = UPDATER.get(this);
      UPDATER.lazySet(this, counter + val);
    }

    public long get() {
      return UPDATER.get(this);
    }
  }

  private final ArrayList<PartitionCounter> partitions = new ArrayList<PartitionCounter>();
  private final ThreadLocal<PartitionCounter> partitionsHolder = new ThreadLocal<PartitionCounter>() {
    @Override
    protected PartitionCounter initialValue() {
      synchronized (partitions) {
        PartitionCounter partitionCounter = new PartitionCounter();
        partitions.add(partitionCounter);
        return partitionCounter;
      }
    }
  };

  public void add(long val) {
    partitionsHolder.get().add(val);
  }

  public void inc() {
    add(1);
  }

  public long get() {
    long val = 0;
    synchronized (partitions) {
      for (PartitionCounter partition : partitions) {
        val += partition.get();
      }
    }
    return val;
  }
}
