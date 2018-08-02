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
package com.sematext.spm.client.tracing.agent.pointcuts.solrj4;

import com.sematext.spm.client.tracing.agent.httpclient4.CloseableHttpClientAccess;
import com.sematext.spm.client.tracing.agent.solrj4.HttpSolrServerAccess;
import com.sematext.spm.client.tracing.agent.solrj5.SolrHttpClient;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;

@LoggerPointcuts(name = "http-solr-client-ctor-pointcut", constructors = {
    "org.apache.solr.client.solrj.impl.HttpSolrServer(java.lang.String url)",
    "org.apache.solr.client.solrj.impl.HttpSolrServer(java.lang.String url, org.apache.http.client.HttpClient client)",
    "org.apache.solr.client.solrj.impl.HttpSolrServer(java.lang.String url, org.apache.http.client.HttpClient client, org.apache.solr.client.solrj.ResponseParser parser)"
})
public class HttpSolrServerCtorPointcut implements UnloggableLogger {
  @Override
  public void logBefore(LoggerContext context) {
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
    final HttpSolrServerAccess httpSolrClient = (HttpSolrServerAccess) context.getThat();
    final CloseableHttpClientAccess httpClient = (CloseableHttpClientAccess) httpSolrClient
        ._$spm_tracing$_getHttpClient();
    SolrHttpClient.markSolrClient(httpClient);
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
  }
}
