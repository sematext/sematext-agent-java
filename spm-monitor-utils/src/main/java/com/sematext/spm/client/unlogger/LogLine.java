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
package com.sematext.spm.client.unlogger;

import java.util.Arrays;
import java.util.Iterator;

import com.sematext.spm.client.util.CollectionUtils;

/**
 *
 *
 */
public final class LogLine implements Iterable<Object> {

  private final String name;
  private final Object[] line;

  private final Factory patterns;
  private static final Object NULL = new Object() {
    public String toString() {
      return "NULL marker, if you see it in dump something wrong";
    }

    ;
  };

  private LogLine(String name, Factory patterns) {
    this.name = name;
    this.line = new Object[patterns.size()];
    this.patterns = patterns;
  }

  public static final class Factory {
    private final byte[] patterns;
    private final int size;
    private final Key[] keys;

    private Factory(byte[] patterns, int size, Key[] keys) {
      this.patterns = patterns;
      this.size = size;
      this.keys = keys;
    }

    public int size() {
      return size;
    }

    public int index(Key key) {
      return patterns[key.ordinal()];
    }

    public LogLine make(String sectionName) {
      LogLine line = new LogLine(sectionName, this);
      return line;
    }

    public static Factory make(Key[]... keys) {
      byte[] patterns = new byte[Key.values().length];
      Arrays.fill(patterns, (byte) -1);

      Key[] allKeys = CollectionUtils.join(Key.class, keys);
      byte globalPosition = 0;
      for (Key key : allKeys) {
        patterns[key.ordinal()] = globalPosition++;
      }

      return new Factory(patterns, globalPosition, allKeys);
    }

  }

  /**
   * Added for performance drama. Instead of use a LinkedHashMap with string as keys, as we did it previously to store
   * log lines, here we "globally" describe the all keys here. NOTE, the order of Keys not describe the order of
   * output(good design :-)), see {@link com.sematext.spm.client.unlogger.LogLine.Factory } to order description.
   */
  public enum Key {
    // SECTION,
    //
    SEQUENCE_ID, RELATIVE_CALL_DEPTH, ENTER_TIME, DURATION_OWN_TOTAL, DURATION_OWN_CPU, DURATION_TOTAL, DURATION_CPU, RETURN_TYPE, CLASS_SIMPLE_NAME, METHOD_SIMPLE_NAME, THREAD_NAME,
    // Solr
    SOLR_CORE_NAME, SOLR_HANDLER_NAME,
    // SolrCloud
    SOLR_COLLECTION_NAME, SOLR_SHARD_NAME, SOLR_REPLICA_NAME,
    //
    RAW_SOLR_QUERY, SOLR_QUERY, LUCENE_QUERY, SOLR_HANDLER_LOGICAL_NAME,
    // Aggregations
    COUNT,
    // Exceptions
    EXCEPTION_TIME, EXCEPTION_TYPE, IS_HANDLED,
    // HBase
    HBASE_REGIONSERVER_GLOBAL_MEMSTORE_UPPERLIMIT;

    // "Macro" for all types of time
    public static final Key[] DURATIONS_PACK = { DURATION_OWN_TOTAL, DURATION_OWN_CPU, DURATION_TOTAL, DURATION_CPU };
  }

  public void putAll(Key[] keys, Object[] values) {
    for (int i = 0; i < keys.length; i++) {
      put(keys[i], values[i]);
    }

  }

  public void put(Key key, Object value) {
    int index = patterns.index(key);
    if (index == -1) {
      throw new IllegalStateException("Unacceptable key " + key + " for logLine :" + Arrays.asList(patterns.keys));
    }
    line[index] = value != null ? value : NULL;
  }

  public Object get(Key key) {
    int index = patterns.index(key);
    if (index == -1) {
      return null;
    }
    Object val = line[index];
    return unmark(val);
  }

  private static Object unmark(Object val) {
    return val != NULL ? val : null;
  }

  public String getName() {
    return name;
  }

  @Override
  public Iterator<Object> iterator() {
    return new Itr();
  }

  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    out.append("LogLine [" + name + "]");
    String delim = " ";
    for (Key key : patterns.keys) {
      out.append(delim).append(key).append(" -> ").append(get(key));
      delim = " , ";
    }
    return out.toString();
  }

  private final class Itr implements Iterator<Object> {
    private int nextPos;

    public Itr() {
      nextPos = scanForNext(0);
    }

    @Override
    public boolean hasNext() {
      return nextPos < line.length;
    }

    @Override
    public Object next() {
      if (!hasNext()) {
        return null;
      }
      Object val = unmark(line[nextPos]);
      nextPos = scanForNext(nextPos + 1);
      return val;
    }

    private int scanForNext(int current) {
      if (current >= line.length) {
        return current;
      }
      for (int i = current; i < line.length; i++) {
        if (line[i] != null) {
          return i;
        }
      }
      return line.length;
    }

    @Override
    public void remove() {
      throw new IllegalStateException();
    }
  }
}
