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

import static com.sematext.spm.client.tracing.agent.model.Call.CallTag.ES;
import static com.sematext.spm.client.tracing.agent.model.Call.CallTag.SOLR;
import static com.sematext.spm.client.tracing.agent.model.Call.CallTag.SQL_QUERY;

import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;

public class DatabaseOperationMetric extends Statistics.Metric<DatabaseOperation> {
  private final DiffCounterVar<Long> countVar;
  private final DiffCounterVar<Long> durationVar;

  public DatabaseOperationMetric(DatabaseOperation operation, MutableVarProvider provider) {
    super(operation);
    this.countVar = provider.newCounter(Long.class);
    this.durationVar = provider.newCounter(Long.class);
  }

  public Long count() {
    return countVar.get();
  }

  public Long duration() {
    return durationVar.get();
  }

  @Override
  protected void update(PartialTransaction transaction) {
  }

  @Override
  protected void update(PartialTransaction transaction, Call call) {
    if ((call.getCallTag() == SOLR || call.getCallTag() == ES || call.getCallTag() == SQL_QUERY)
        && call.getAnnotation() != null) {
      countVar.increment(1L);
      durationVar.increment(call.getDuration());
    }
  }
}
