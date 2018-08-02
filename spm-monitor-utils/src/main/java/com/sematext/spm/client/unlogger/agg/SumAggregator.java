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
package com.sematext.spm.client.unlogger.agg;

import java.util.Collection;
import java.util.Map;

import com.sematext.spm.client.unlogger.LogLine;
import com.sematext.spm.client.unlogger.LogLine.Key;
import com.sematext.spm.client.unlogger.annotations.SumGroupBy;

public class SumAggregator extends PointcutBasedAggregator {

  // private final SumGroupBy config;
  // Hmm, access via Annotation proxy is slow?
  private final Key[] groupByKeys;
  private final Key[] aggregatesKeys;
  private final LogLine.Factory logLineFactory;

  private static final Key[] ADDITIONAL_KEYS = new Key[] { Key.COUNT };

  public SumAggregator(SumGroupBy config, String name) {
    super(name);
    groupByKeys = config.groupBy();
    aggregatesKeys = config.aggregate();
    logLineFactory = LogLine.Factory.make(groupByKeys, ADDITIONAL_KEYS, aggregatesKeys);
  }

  @Override
  public void aggregate(LogLine line, Collection<LogLine> ungroupedSlice, Map<Object, Aggregate> groupedSlice) {
    StatsKey key = StatsKey.make(getName(), groupByKeys, line);

    Aggregate sumAggregate = groupedSlice.get(key);
    if (sumAggregate == null) {
      sumAggregate = new SumAggregate(key);
      groupedSlice.put(key, sumAggregate);
    }
    sumAggregate.process(line);
  }

  private static final class StatsKey {
    private final String partitioner;
    private final int hashCode;
    private final Key[] keysSlice;
    private final LogLine data;

    private StatsKey(String partitioner, Key[] keysSlice, LogLine data, int hashCode) {
      this.partitioner = partitioner;
      this.hashCode = hashCode;
      this.data = data;
      this.keysSlice = keysSlice;
    }

    public static StatsKey make(String partitioner, Key[] keysSlice, LogLine values) {
      return new StatsKey(partitioner, keysSlice, values, hashCode(partitioner, keysSlice, values));
    }

    private static int hashCode(String partitioner, Key[] keysSlice, LogLine values) {
      int result = 1;
      result = 31 * result + ((partitioner == null) ? 0 : partitioner.hashCode());

      for (int i = 0; i < keysSlice.length; i++) {
        Object val = values.get(keysSlice[i]);
        result = 31 * result + (val != null ? val.hashCode() : 0);
      }

      return result;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      StatsKey other = (StatsKey) obj;
      if (partitioner == null) {
        if (other.partitioner != null) {
          return false;
        }
      } else if (!partitioner.equals(other.partitioner)) {
        return false;
      }

      for (Key key : keysSlice) {
        Object x = data.get(key);
        Object y = other.data.get(key);
        if (x == y) {
          continue;
        }
        if (x == null || y == null) {
          return false;
        }
        if (!x.equals(y)) {
          return false;
        }
      }
      return true;
    }

    public void fill(LogLine logLine) {
      for (Key key : keysSlice) {
        logLine.put(key, data.get(key));
      }
    }

  }

  private final class SumAggregate implements Aggregate {

    private final StatsKey groupingKey;
    private long count = 0;
    private final long[] aggregates;

    private SumAggregate(StatsKey groupingKey) {
      this.groupingKey = groupingKey;
      aggregates = new long[Key.values().length];
    }

    @Override
    public void process(LogLine line) {
      count++;
      for (Key aggregate : aggregatesKeys) {
        Number number = (Number) line.get(aggregate);

        if (number != null) {
          aggregates[aggregate.ordinal()] += number.longValue();
        }
      }
    }

    @Override
    public LogLine toOut() {
      LogLine res = logLineFactory.make(groupingKey.partitioner);
      groupingKey.fill(res);
      res.put(Key.COUNT, count);
      // res.put(LogLine.Key.SUM, sum);
      for (Key aggregate : aggregatesKeys) {
        res.put(aggregate, aggregates[aggregate.ordinal()]);
      }

      return res;
    }

  }

  @Override
  public String toString() {
    return "SumAggregator [aggregate=" + aggregatesKeys + ", groupBy=" + groupByKeys + ", getName()=" + getName() + "]";
  }

}
