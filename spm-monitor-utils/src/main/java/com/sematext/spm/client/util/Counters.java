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
package com.sematext.spm.client.util;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Counters<T> extends PubSub<Tuple<T, Long>, Map<T, AtomicLong>> {
  public Counters(long consumptionIntervalMillis, Clock clock, Map<T, AtomicLong> init) {
    super(consumptionIntervalMillis, clock, init);
  }

  public Counters() {
    super(TimeUnit.MINUTES.toMillis(1), Clocks.WALL, new ConcurrentHashMap<T, AtomicLong>());
  }

  @Override
  public void publish(Tuple<T, Long> entry) {
    AtomicLong counter = container().get(entry.getFirst());
    if (counter == null) {
      counter = new AtomicLong(0L);
      container().put(entry.getFirst(), counter);
    }
    counter.addAndGet(entry.getSecond());
  }

  @Override
  protected Map<T, AtomicLong> newContainer() {
    return new ConcurrentHashMap<T, AtomicLong>();
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Map<T, AtomicLong> emptyContainer() {
    return Collections.EMPTY_MAP;
  }
}
