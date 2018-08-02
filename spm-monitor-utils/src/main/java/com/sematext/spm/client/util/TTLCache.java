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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Write optimised ttl cache. Intended to be used for rare-reads (spm-monitor case - once per 10sec).
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class TTLCache<K, V> {
  private static class Entry<V> {
    private final V value;
    private AtomicLong deadline = new AtomicLong(0L);

    Entry(V value) {
      this.value = value;
    }

    void refresh(long ttl, Clock clock) {
      deadline.getAndSet(clock.now() + ttl);
    }

    boolean expired(Clock clock) {
      return deadline.get() < clock.now();
    }
  }

  public static interface Clock {
    long now();
  }

  private static final Clock WALL = new Clock() {
    @Override
    public long now() {
      return System.currentTimeMillis();
    }
  };

  private final ConcurrentHashMap<K, Entry<V>> entries = new ConcurrentHashMap<K, Entry<V>>();
  private final long ttl;
  private final Clock clock;

  public TTLCache(long ttl, Clock clock) {
    this.ttl = ttl;
    this.clock = clock;
  }

  public TTLCache(long ttl) {
    this(ttl, WALL);
  }

  public V putIfAbsent(K key, V value) {
    Entry<V> entry = new Entry<V>(value);
    Entry<V> existing = entries.putIfAbsent(key, entry);
    if (existing != null) {
      existing.refresh(ttl, clock);
      return existing.value;
    }
    entry.refresh(ttl, clock);
    return null;
  }

  public boolean contains(K key) {
    Entry<V> entry = entries.get(key);
    if (entry != null) {
      if (entry.expired(clock)) {
        entries.remove(key);
        return false;
      }
      return true;
    }
    return false;
  }

  public V get(K key) {
    Entry<V> entry = entries.get(key);
    if (entry != null) {
      if (entry.expired(clock)) {
        entries.remove(key);
        return null;
      }
      return entry.value;
    }
    return null;
  }

  public boolean touch(K key) {
    Entry<V> entry = entries.get(key);
    if (entry != null) {
      entry.refresh(ttl, clock);
      return true;
    }
    return false;
  }

  public Collection<V> values() {
    final Set<V> alive = new HashSet<V>();
    for (Map.Entry<K, Entry<V>> entry : entries.entrySet()) {
      if (entry.getValue().expired(clock)) {
        entries.remove(entry.getKey());
      } else {
        alive.add(entry.getValue().value);
      }
    }
    return alive;
  }
}
