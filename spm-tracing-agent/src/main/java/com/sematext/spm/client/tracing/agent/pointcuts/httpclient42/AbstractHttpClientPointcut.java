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
package com.sematext.spm.client.tracing.agent.pointcuts.httpclient42;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.tracing.agent.Trace;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.httpclient4.AbstractHttpClientAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.Header4Access;
import com.sematext.spm.client.tracing.agent.httpclient4.HttpClientURL;
import com.sematext.spm.client.tracing.agent.httpclient4.HttpHostAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.HttpRequestAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.HttpResponseAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.RequestLineAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.StatusLineAccess;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.HttpHeaders;
import com.sematext.spm.client.tracing.agent.model.annotation.HTTPRequestAnnotation;
import com.sematext.spm.client.tracing.agent.pointcuts.solrj5.HttpClient4SolrRequest;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;

@LoggerPointcuts(name = "abstract-http-client-pointcut", methods = {
    "org.apache.http.HttpResponse org.apache.http.impl.client.AbstractHttpClient#execute(org.apache.http.HttpHost target, "
        +
        "org.apache.http.HttpRequest request, org.apache.http.protocol.HttpContext context)"
})
public class AbstractHttpClientPointcut implements UnloggableLogger {
  private final Log LOG = LogFactory.getLog(AbstractHttpClientPointcut.class);

  @Override
  public void logBefore(LoggerContext context) {
    final Trace trace = Tracing.current();
    trace.newCall(context.getJoinPoint());
    trace.setTag(Call.CallTag.HTTP_REQUEST);

    final HttpRequestAccess httpRequest = context.getMethodParam("request");
    try {
      final RequestLineAccess requestLine = httpRequest._$spm_tracing$_getRequestLine();
      final HttpHostAccess host = context.getMethodParam("target");

      final AbstractHttpClientAccess client = (AbstractHttpClientAccess) context.getThat();
      if (client._$spm_tracing$_isSolrClient()) {
        HttpClient4SolrRequest.before(trace, host, httpRequest);
      } else {
        final HTTPRequestAnnotation annotation = HTTPRequestAnnotation
            .request(HttpClientURL.makeUrl(host, requestLine));
        annotation.setMethod(requestLine._$spm_tracing$_getMethod());
        trace.setAnnotation(annotation);
      }

      httpRequest._$spm_tracing$_setHeader(HttpHeaders.SPM_TRACING_TRACE_ID, String.valueOf(trace.getTraceId()));
      httpRequest._$spm_tracing$_setHeader(HttpHeaders.SPM_TRACING_CALL_ID, String.valueOf(trace.getCallId()));
      httpRequest._$spm_tracing$_setHeader(HttpHeaders.SPM_TRACING_SAMPLED, String.valueOf(trace.isSampled()));
    } catch (Throwable e) {
      LOG.error("Can't set http request annotation.", e);
    }
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
    try {
      final HttpResponseAccess httpResponse = (HttpResponseAccess) returnValue;
      final StatusLineAccess statusLine = httpResponse._$spm_tracing$_getStatusLine();

      final AbstractHttpClientAccess client = (AbstractHttpClientAccess) context.getThat();

      if (client._$spm_tracing$_isSolrClient()) {
        HttpClient4SolrRequest.after(Tracing.current(), httpResponse);
      } else {
        final HTTPRequestAnnotation annotation = (HTTPRequestAnnotation) Tracing.current().getAnnotation();
        if (annotation != null) {
          annotation.setResponseCode(statusLine._$spm_tracing$_getStatusCode());
        }
      }

      final Header4Access crossAppCallHeader = httpResponse
          ._$spm_tracing$_getFirstHeader(HttpHeaders.SPM_TRACING_CROSS_APP_CALL);
      if (crossAppCallHeader != null) {
        String value = crossAppCallHeader._$spm_tracing$_getValue();
        if (value != null) {
          Tracing.current().setCrossAppInHeader(HttpHeaders.decodeCrossAppCallHeader(value));
        }
      }
    } catch (Throwable e) {
      LOG.error("Can't update http request annotation.", e);
    } finally {
      Tracing.current().endCall();
    }
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
    Tracing.current().setFailed(true);
    if (((AbstractHttpClientAccess) context.getThat())._$spm_tracing$_isSolrClient()) {
      HttpClient4SolrRequest.exception(Tracing.current());
    }
    Tracing.current().endCall();
  }
}
