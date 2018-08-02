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
package com.sematext.spm.client.tracing.agent.pointcuts.solrj5;

import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.solrj5.SolrHttpClientParams;
import com.sematext.spm.client.tracing.agent.solrj5.UpdateRequestAccess;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;

@LoggerPointcuts(name = "concurrent-update-solr-client-pointcut", methods = {
    "org.apache.solr.common.util.NamedList org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient#request(org.apache.solr.client.solrj.SolrRequest request, java.lang.String collection)",
    "org.apache.solr.common.util.NamedList org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient#request(org.apache.solr.client.solrj.SolrRequest request)"
})
public class ConcurrentUpdateSolrClientPointcut implements UnloggableLogger {

  @Override
  public void logBefore(LoggerContext context) {
    Object req = context.getMethodParam("request");
    if (req instanceof UpdateRequestAccess) {
      UpdateRequestAccess updateRequest = (UpdateRequestAccess) req;
      updateRequest._$spm_tracing$_setParam(SolrHttpClientParams.SPM_SOLR_TRACING_ASYNC, "true");
      updateRequest._$spm_tracing$_setParam(SolrHttpClientParams.SPM_SOLR_TRACING_TRACE_ID, String
          .valueOf(Tracing.current().getTraceId()));
      updateRequest._$spm_tracing$_setParam(SolrHttpClientParams.SPM_SOLR_TRACING_CALL_ID, String
          .valueOf(Tracing.current().getCallId()));
      updateRequest._$spm_tracing$_setParam(SolrHttpClientParams.SPM_SOLR_TRACING_SAMPLED, String
          .valueOf(Tracing.current().isSampled()));
    }
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
  }
}
