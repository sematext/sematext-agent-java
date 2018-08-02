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
package com.sematext.spm.client.redis;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.List;
import java.util.Map;

import com.sematext.spm.client.util.Tuple;

public final class RedisInfoMetricExtractors {
  private RedisInfoMetricExtractors() {
  }

  private static final class BaseDBExtractor<T> implements RedisInfoMetricExtractor<List<Tuple<String, T>>> {
    private final String fieldName;
    private final ValueHolders.ValueHolderFactory<T> holderFactory;
    private final ValueInjections.ValueInjection<T> injection;
    private final Map<String, ValueHolders.ValueHolder<T>> holders = new UnifiedMap<String, ValueHolders.ValueHolder<T>>();

    private BaseDBExtractor(String fieldName, ValueHolders.ValueHolderFactory<T> holderFactory,
                            ValueInjections.ValueInjection<T> injection) {
      this.holderFactory = holderFactory;
      this.injection = injection;
      this.fieldName = fieldName;
    }

    private ValueHolders.ValueHolder<T> getHolder(String db) {
      ValueHolders.ValueHolder<T> holder = holders.get(db);
      if (holder == null) {
        holder = holderFactory.create();
        holders.put(db, holder);
      }
      return holder;
    }

    @Override
    public List<Tuple<String, T>> extract(RedisInfo info) {
      List<Tuple<String, T>> dbMetrics = new FastList<Tuple<String, T>>();
      for (String db : info.getDatabases()) {
        String metricValue = info.get(db, fieldName);
        if (metricValue != null) {
          T value = injection.inject(metricValue);
          dbMetrics.add(Tuple.tuple(db, getHolder(db).updateValue(value)));
        }
      }
      return dbMetrics;
    }
  }

  private static final class BaseExtractor<T> implements RedisInfoMetricExtractor<T> {
    private final String fieldName;
    private final ValueHolders.ValueHolder<T> valueHolder;
    private final ValueInjections.ValueInjection<T> valueInjection;

    private BaseExtractor(String fieldName, ValueHolders.ValueHolder<T> valueHolder,
                          ValueInjections.ValueInjection<T> valueInjection) {
      this.fieldName = fieldName;
      this.valueHolder = valueHolder;
      this.valueInjection = valueInjection;
    }

    @Override
    public T extract(RedisInfo info) {
      return valueHolder.updateValue(valueInjection.inject(info.get(fieldName)));
    }
  }

  @SuppressWarnings("unchecked")
  public static RedisInfoMetricExtractor<Object> doubleExtractor(final String fieldName) {
    return new BaseExtractor(fieldName, ValueHolders.identityHolderFactory(Double.class)
        .create(), ValueInjections.DOUBLE_INJECTION);
  }

  @SuppressWarnings("unchecked")
  public static RedisInfoMetricExtractor<Object> longExtractor(final String fieldName) {
    return new BaseExtractor(fieldName, ValueHolders.identityHolderFactory(Long.class)
        .create(), ValueInjections.LONG_INJECTION);
  }

  @SuppressWarnings("unchecked")
  public static RedisInfoMetricExtractor<Object> counterExtractor(final String fieldName) {
    return new BaseExtractor(fieldName, ValueHolders.counterHolderFactory().create(), ValueInjections.LONG_INJECTION);
  }

  @SuppressWarnings("unchecked")
  public static RedisInfoMetricExtractor<Object> longDBExtractor(final String fieldName) {
    return new BaseDBExtractor(fieldName, ValueHolders
        .identityHolderFactory(Long.class), ValueInjections.LONG_INJECTION);
  }
}
