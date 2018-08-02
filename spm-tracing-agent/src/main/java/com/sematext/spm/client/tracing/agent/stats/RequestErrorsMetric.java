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
import com.sematext.spm.client.tracing.agent.model.FailureType;
import com.sematext.spm.client.tracing.agent.model.Http;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.model.WebTransactionSummary;

public class RequestErrorsMetric extends Statistics.Metric<String> {
  private final DiffCounterVar<Long> exceptionsCountVar;
  private final DiffCounterVar<Long> responseCode5xxCountVar;
  private final DiffCounterVar<Long> responseCode4xxCountVar;
  private final DiffCounterVar<Long> errorsCountVar;

  public RequestErrorsMetric(String request, MutableVarProvider provider) {
    super(request);
    this.exceptionsCountVar = provider.newCounter(Long.class);
    this.responseCode5xxCountVar = provider.newCounter(Long.class);
    this.responseCode4xxCountVar = provider.newCounter(Long.class);
    this.errorsCountVar = provider.newCounter(Long.class);
  }

  public Long getExceptionsCount() {
    return exceptionsCountVar.get();
  }

  public Long getResponseCode5xxCount() {
    return responseCode5xxCountVar.get();
  }

  public Long getResponseCode4xxCount() {
    return responseCode4xxCountVar.get();
  }

  public Long getErrorsCount() {
    return errorsCountVar.get();
  }

  private void updateHttpResponseStatusStatistics(PartialTransaction transaction) {
    final WebTransactionSummary summary = (WebTransactionSummary) transaction.getTransactionSummary();
    if (summary != null) {
      if (Http.is4xx(summary.getResponseCode())) {
        responseCode4xxCountVar.increment(1L);
      } else if (Http.is5xx(summary.getResponseCode())) {
        responseCode5xxCountVar.increment(1L);
      }
    }
  }

  @Override
  protected void update(PartialTransaction transaction) {
    if (transaction.isEntryPoint()) {
      if (transaction.isFailed()) {
        errorsCountVar.increment(1L);

        if (FailureType.EXCEPTION.equals(transaction.getFailureType())) {
          exceptionsCountVar.increment(1L);
          if (Call.TransactionType.WEB.equals(transaction.getTransactionType())) {
            updateHttpResponseStatusStatistics(transaction);
          }
        } else if (FailureType.HTTP_RESPONSE.equals(transaction.getFailureType())) {
          updateHttpResponseStatusStatistics(transaction);
        }
      }
    }
  }

  @Override
  protected void update(PartialTransaction transaction, Call call) {

  }
}
