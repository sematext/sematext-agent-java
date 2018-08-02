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
package com.sematext.spm.client.tracing.agent.pointcuts.httpclient3;

import com.sematext.spm.client.tracing.agent.httpclient3.HttpClientAccess;
import com.sematext.spm.client.tracing.agent.httpclient3.HttpMethodAccess;
import com.sematext.spm.client.tracing.agent.solrj5.SolrHttpClient;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;

@LoggerPointcuts(name = "http-client-pointcut", methods = {
    "int org.apache.commons.httpclient.HttpClient#executeMethod(org.apache.commons.httpclient.HttpMethod method)",
    "int org.apache.commons.httpclient.HttpClient#executeMethod(org.apache.commons.httpclient.HostConfiguration config, org.apache.commons.httpclient.HttpMethod method)",
    "int org.apache.commons.httpclient.HttpClient#executeMethod(org.apache.commons.httpclient.HostConfiguration config, org.apache.commons.httpclient.HttpMethod method, org.apache.commons.httpclient.HttpState state)"
})
public class HttpClientPointcut implements UnloggableLogger {
  @Override
  public void logBefore(LoggerContext context) {
    HttpClientAccess httpClient = (HttpClientAccess) context.getThat();
    if (httpClient._$spm_tracing$_isSolrClient()) {

      SolrHttpClient.markSolrClient(httpClient);
      HttpMethodAccess method = context.getMethodParam("method");
      if (method != null) {
        method._$spm_tracing$_setSolrClient(true);
      }
    }
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
  }
}
