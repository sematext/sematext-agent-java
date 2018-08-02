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
package com.sematext.spm.client.tracing;

import java.util.concurrent.atomic.AtomicLong;

import com.sematext.spm.client.attributes.RealCounterValueHolder;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.stats.DiffCounterVar;
import com.sematext.spm.client.tracing.agent.stats.MutableVarProvider;
import com.sematext.spm.client.tracing.agent.stats.StatisticsView;
import com.sematext.spm.client.tracing.agent.stats.TracingStatistics;

public final class TracingStatsExtractors {

  private static TracingStatistics getTracingStatistics() {
    return ServiceLocator.getTracingStatistics();
  }

  private static volatile TracingStatistics TRACING_STATISTICS = null;
  private static volatile StatisticsView CALL_STATISTICS_VIEW = null;

  private static class CounterValueHolderVar implements DiffCounterVar<Long> {
    private final RealCounterValueHolder holder = new RealCounterValueHolder();
    private final AtomicLong accumulated = new AtomicLong(0L);

    public CounterValueHolderVar() {
      holder.getValue(0L);
    }

    @Override
    public Long increment(Long diff) {
      return accumulated.addAndGet(diff);
    }

    @Override
    public Long get() {
      return holder.getValue(accumulated.get());
    }
  }

  private static MutableVarProvider MUTABLE_VAR_PROVIDER = new MutableVarProvider() {
    @Override
    @SuppressWarnings("unchecked")
    public <T, C extends Class<T>> DiffCounterVar<T> newCounter(C klass) {
      if (Long.class.equals(klass)) {
        return (DiffCounterVar<T>) new CounterValueHolderVar();
      }
      throw new IllegalArgumentException("Counter of type '" + klass + "' is not implemented.");
    }
  };

  public static StatisticsView callStatisticsView() {
    if (getTracingStatistics() != TRACING_STATISTICS) {
      synchronized (TracingStatsExtractors.class) {
        TracingStatistics current = getTracingStatistics();
        if (current != TRACING_STATISTICS) {
          CALL_STATISTICS_VIEW = current.newCallStatisticsView(MUTABLE_VAR_PROVIDER);
          TRACING_STATISTICS = current;
        }
      }
    }
    return CALL_STATISTICS_VIEW;
  }
}
