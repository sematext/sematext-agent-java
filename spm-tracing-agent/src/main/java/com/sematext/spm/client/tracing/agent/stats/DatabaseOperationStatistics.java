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

import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.model.SolrAnnotation;
import com.sematext.spm.client.tracing.agent.model.annotation.ESAnnotation;
import com.sematext.spm.client.tracing.agent.model.annotation.SQLAnnotation;

public class DatabaseOperationStatistics extends Statistics<DatabaseOperation, DatabaseOperationMetric>
    implements StatisticsProcessor {
  private final MutableVarProvider provider;

  public DatabaseOperationStatistics(MutableVarProvider provider) {
    this.provider = provider;
  }

  @Override
  protected DatabaseOperationMetric newMetric(DatabaseOperation operation) {
    return new DatabaseOperationMetric(operation, provider);
  }

  @Override
  public void process(PartialTransaction transaction) {
    for (final Call call : transaction.getCalls()) {
      if (call.getCallTag() == Call.CallTag.SOLR) {
        SolrAnnotation annotation = (SolrAnnotation) call.getAnnotation();
        if (annotation != null) {
          update(new DatabaseOperation("SOLR", annotation.getRequestType().name()), transaction, call);
        }
      } else if (call.getCallTag() == Call.CallTag.ES) {
        ESAnnotation annotation = (ESAnnotation) call.getAnnotation();
        if (annotation != null) {
          update(new DatabaseOperation("ELASTICSEARCH", annotation.getRequestType().name()), transaction, call);
        }
      } else if (call.getCallTag() == Call.CallTag.SQL_QUERY) {
        SQLAnnotation annotation = (SQLAnnotation) call.getAnnotation();
        if (annotation != null) {
          update(new DatabaseOperation("SQL", annotation.getOperation().name()), transaction, call);
        }
      }
    }
  }
}
