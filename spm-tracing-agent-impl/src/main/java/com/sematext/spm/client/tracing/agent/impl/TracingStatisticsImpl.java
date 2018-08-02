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
package com.sematext.spm.client.tracing.agent.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.stats.*;

public final class TracingStatisticsImpl implements TracingStatistics {

  private static class StatisticsViewImpl implements StatisticsView {
    private final CrossAppCallStatistics crossAppCallStatistics;
    private final ExternalCallStatistics externalCallStatistics;
    private final RequestComponentStatistics requestComponentStatistics;
    private final RequestStatistics requestStatistics;
    private final RequestErrorsStatistics requestErrorsStatistics;
    private final DatabaseOperationStatistics databaseOperationStatistics;
    private final List<StatisticsProcessor> processors;

    private StatisticsViewImpl(MutableVarProvider varProvider) {
      this.crossAppCallStatistics = new CrossAppCallStatistics(varProvider);
      this.externalCallStatistics = new ExternalCallStatistics(varProvider);
      this.requestComponentStatistics = new RequestComponentStatistics(varProvider);
      this.requestStatistics = new RequestStatistics(varProvider);
      this.requestErrorsStatistics = new RequestErrorsStatistics(varProvider);
      this.databaseOperationStatistics = new DatabaseOperationStatistics(varProvider);
      this.processors = Arrays.asList(
          crossAppCallStatistics,
          externalCallStatistics,
          requestComponentStatistics,
          requestStatistics,
          requestErrorsStatistics,
          databaseOperationStatistics
      );
    }

    @Override
    public Collection<CrossAppCall> crossAppCalls() {
      return crossAppCallStatistics.getCrossAppCalls();
    }

    @Override
    public Collection<ExternalCall> externalCalls() {
      return externalCallStatistics.getExternalCalls();
    }

    @Override
    public Collection<RequestMetric> requestMetrics() {
      return requestStatistics.values();
    }

    @Override
    public Collection<RequestErrorsMetric> requestErrorMetrics() {
      return requestErrorsStatistics.values();
    }

    @Override
    public Collection<RequestComponentMetric> requestComponentMetrics() {
      return requestComponentStatistics.values();
    }

    @Override
    public Collection<DatabaseOperationMetric> databaseOperationMetrics() {
      return databaseOperationStatistics.values();
    }

    private void update(PartialTransaction transaction) {
      for (StatisticsProcessor processor : processors) {
        processor.process(transaction);
      }
    }
  }

  private final List<StatisticsViewImpl> callStatisticsViews = new CopyOnWriteArrayList<StatisticsViewImpl>();

  @Override
  public void record(PartialTransaction transaction) {
    for (StatisticsViewImpl view : callStatisticsViews) {
      view.update(transaction);
    }
  }

  @Override
  public StatisticsView newCallStatisticsView(MutableVarProvider varProvider) {
    final StatisticsViewImpl view = new StatisticsViewImpl(varProvider);
    callStatisticsViews.add(view);
    return view;
  }
}
