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
package com.sematext.spm.client.tracing.agent.stats;

import java.util.Collection;
import java.util.Collections;

import com.sematext.spm.client.tracing.agent.model.PartialTransaction;

public final class NoOpTracingStatistics implements TracingStatistics {
  private NoOpTracingStatistics() {
  }

  private static final NoOpTracingStatistics INSTANCE = new NoOpTracingStatistics();

  public static TracingStatistics noOp() {
    return INSTANCE;
  }

  private static final StatisticsView EMPTY_VIEW = new StatisticsView() {
    @Override
    public Collection<CrossAppCall> crossAppCalls() {
      return Collections.emptyList();
    }

    @Override
    public Collection<ExternalCall> externalCalls() {
      return Collections.emptyList();
    }

    @Override
    public Collection<RequestMetric> requestMetrics() {
      return Collections.emptyList();
    }

    @Override
    public Collection<RequestComponentMetric> requestComponentMetrics() {
      return Collections.emptyList();
    }

    @Override
    public Collection<RequestErrorsMetric> requestErrorMetrics() {
      return Collections.emptyList();
    }

    @Override
    public Collection<DatabaseOperationMetric> databaseOperationMetrics() {
      return Collections.emptyList();
    }
  };

  @Override
  public void record(PartialTransaction transaction) {
  }

  @Override
  public StatisticsView newCallStatisticsView(MutableVarProvider varProvider) {
    return EMPTY_VIEW;
  }
}
