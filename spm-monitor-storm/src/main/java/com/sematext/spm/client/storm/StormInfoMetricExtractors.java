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
package com.sematext.spm.client.storm;

import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.Arrays;
import java.util.List;

import com.sematext.spm.client.attributes.DoubleCounterValueHolder;
import com.sematext.spm.client.attributes.RealCounterValueHolder;
import com.sematext.spm.client.util.Tuple;

public final class StormInfoMetricExtractors {
  private StormInfoMetricExtractors() {
  }

  private abstract static class BaseExtractor<T> implements StormInfoMetricExtractor<T> {
    private final String fieldName;

    protected BaseExtractor(String fieldName) {
      this.fieldName = fieldName;
    }

    public abstract T getValue(String value);

    @Override
    public T extract(StormInfo info) {
      return getValue(info.get(fieldName));
    }

    @Override
    public List<Tuple<String, T>> extractForTopologies(String topology, StormInfo info) {
      List<Tuple<String, T>> topologyMetric = new FastList<Tuple<String, T>>();

      String metricValue = info.getTopology(topology, fieldName);
      if (metricValue != null) {
        topologyMetric.add(Tuple.tuple(topology, getValue(metricValue)));
      }

      return topologyMetric;
    }

    @Override
    public List<Tuple<String, T>> extractForSupervisors(String supervisor, StormInfo info) {
      List<Tuple<String, T>> supervisorMetric = new FastList<Tuple<String, T>>();

      String metricValue = info.getSupervisor(supervisor, fieldName);
      if (metricValue != null) {
        supervisorMetric.add(Tuple.tuple(supervisor, getValue(metricValue)));
      }

      return supervisorMetric;
    }

    @Override
    public List<Tuple<List<String>, T>> extractForBoltsInputStats(String topology, ExecutorInputStatsKey statsKey,
                                                                  StormInfo info) {
      List<Tuple<List<String>, T>> boltMetric = new FastList<Tuple<List<String>, T>>();

      String metricValue = info.getBoltInputStats(topology, statsKey, fieldName);
      boltMetric.add(Tuple.tuple(Arrays.asList(topology, statsKey.getComponentId(), statsKey.getStream(), statsKey
                                                   .getExecutorId(),
                                               statsKey.getInputComponentId()), getValue(metricValue)));

      return boltMetric;
    }

    @Override
    public List<Tuple<List<String>, T>> extractForBoltsOutputStats(String topology, ExecutorOutputStatsKey statsKey,
                                                                   StormInfo info) {
      List<Tuple<List<String>, T>> boltMetric = new FastList<Tuple<List<String>, T>>();

      String metricValue = info.getBoltOutputStats(topology, statsKey, fieldName);
      boltMetric.add(Tuple.tuple(Arrays.asList(topology, statsKey.getComponentId(),
                                               statsKey.getStream(), statsKey.getExecutorId()), getValue(metricValue)));

      return boltMetric;
    }

    @Override
    public List<Tuple<List<String>, T>> extractForSpoutsOutputStats(String topology, ExecutorOutputStatsKey statsKey,
                                                                    StormInfo info) {
      List<Tuple<List<String>, T>> spoutMetric = new FastList<Tuple<List<String>, T>>();

      String metricValue = info.getSpoutOutputStats(topology, statsKey, fieldName);

      spoutMetric.add(Tuple.tuple(Arrays.asList(topology, statsKey.getComponentId(),
                                                statsKey.getStream(), statsKey
                                                    .getExecutorId()), getValue(metricValue)));

      return spoutMetric;
    }
  }

  private static final class StringExtractor extends BaseExtractor<Object> {
    private StringExtractor(String fieldName) {
      super(fieldName);
    }

    @Override
    public String getValue(String value) {
      return value;
    }
  }

  private static class LongExtractor extends BaseExtractor<Object> {
    protected LongExtractor(String fieldName) {
      super(fieldName);
    }

    @Override
    public Long getValue(String value) {
      if (value == null) {
        return 0L;
      }
      return Long.parseLong(value);
    }
  }

  private static class DoubleExtractor extends BaseExtractor<Object> {
    protected DoubleExtractor(String fieldName) {
      super(fieldName);
    }

    @Override
    public Double getValue(String value) {
      if (value == null) {
        return 0d;
      }
      return Double.parseDouble(value);
    }
  }

  private static final class CounterExtractor extends LongExtractor {
    private final RealCounterValueHolder counterValueHolder = new RealCounterValueHolder();

    private CounterExtractor(String fieldName) {
      super(fieldName);
    }

    @Override
    public Long getValue(String value) {
      return counterValueHolder.getValue(super.getValue(value));
    }
  }

  private static final class DoubleCounterExtractor extends DoubleExtractor {
    private final DoubleCounterValueHolder counterValueHolder = new DoubleCounterValueHolder();

    private DoubleCounterExtractor(String fieldName) {
      super(fieldName);
    }

    @Override
    public Double getValue(String value) {
      return counterValueHolder.getValue(super.getValue(value));
    }
  }

  public static DoubleExtractor doubleExtractor(final String fieldName) {
    return new DoubleExtractor(fieldName);
  }

  public static LongExtractor longExtractor(final String fieldName) {
    return new LongExtractor(fieldName);
  }

  public static CounterExtractor counterExtractor(final String fieldName) {
    return new CounterExtractor(fieldName);
  }

  public static DoubleCounterExtractor doubleCounterExtractor(final String fieldName) {
    return new DoubleCounterExtractor(fieldName);
  }

  public static StringExtractor stringExtractor(final String fieldName) {
    return new StringExtractor(fieldName);
  }
}
