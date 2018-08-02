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
package com.sematext.spm.client.tracing.agent.pointcuts.httpclient4;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.instrumentation.MixinCast;
import com.sematext.spm.client.tracing.agent.Trace;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.httpclient4.CloseableHttpClientAccess;
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
import com.sematext.spm.client.tracing.agent.pointcuts.solrj5.ConcurrentUpdateSolrClientHack;
import com.sematext.spm.client.tracing.agent.pointcuts.solrj5.HttpClient4SolrRequest;
import com.sematext.spm.client.tracing.agent.util.AsyncContext;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;

@LoggerPointcuts(name = "closeable-http-client-pointcut", methods = {
    "org.apache.http.client.methods.CloseableHttpResponse org.apache.http.impl.client.CloseableHttpClient#doExecute(org.apache.http.HttpHost host, "
        +
        "org.apache.http.HttpRequest request, org.apache.http.protocol.HttpContext context)"
})
public class CloseableHttpClientPointcut implements UnloggableLogger {
  private static final Log LOG = LogFactory.getLog(CloseableHttpClientPointcut.class);

  private static void maybeStartAsyncTransaction(LoggerContext context) {
    final HttpRequestAccess request = context.getMethodParam("request");
    ConcurrentUpdateSolrClientHack
        .maybeStartAsyncTransaction(request, request._$spm_tracing$_getRequestLine()._$spm_tracing$_getUri());
  }

  private static void maybeStopAsyncTransaction(LoggerContext context) {
    final HttpRequestAccess request = context.getMethodParam("request");
    if (AsyncContext.clean(request) != null) {
      Tracing.endTrace();
    }
  }

  @Override
  public void logBefore(LoggerContext context) {
    maybeStartAsyncTransaction(context);

    final Trace tracing = Tracing.current();
    tracing.newCall(context.getJoinPoint());
    tracing.setTag(Call.CallTag.HTTP_REQUEST);

    final HttpRequestAccess httpRequest = context.getMethodParam("request");
    try {
      final CloseableHttpClientAccess access = (CloseableHttpClientAccess) context.getThat();
      final HttpHostAccess host = context.getMethodParam("host");

      if (access._$spm_tracing$_isSolrClient()) {
        HttpClient4SolrRequest.before(tracing, host, httpRequest);
      } else {
        final RequestLineAccess requestLine = httpRequest._$spm_tracing$_getRequestLine();
        final HTTPRequestAnnotation annotation = HTTPRequestAnnotation
            .request(HttpClientURL.makeUrl(host, requestLine));
        annotation.setMethod(requestLine._$spm_tracing$_getMethod());
        tracing.setAnnotation(annotation);
      }

      httpRequest._$spm_tracing$_setHeader(HttpHeaders.SPM_TRACING_TRACE_ID, String.valueOf(tracing.getTraceId()));
      httpRequest._$spm_tracing$_setHeader(HttpHeaders.SPM_TRACING_CALL_ID, String.valueOf(tracing.getCallId()));
      httpRequest._$spm_tracing$_setHeader(HttpHeaders.SPM_TRACING_SAMPLED, String.valueOf(tracing.isSampled()));
    } catch (Throwable e) {
      LOG.error("Can't set http request annotation.", e);
    }
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
    try {
      /**
       * Sometimes httpclient proxies org.apache.http.HttpResponse using org.apache.http.impl.client.CloseableHttpResponseProxy.
       * It means, that all mixins are gone and CCE will be thrown at this place. It is possible to do a trick -
       * access to proxy field which holds reference to HttpResponse and make another proxy to HttpResponseAccess which
       * calls original object instead of proxied.
       */
      final HttpResponseAccess httpResponse = MixinCast.cast(returnValue, HttpResponseAccess.class, "original");

      final StatusLineAccess statusLine = httpResponse._$spm_tracing$_getStatusLine();

      final CloseableHttpClientAccess client = (CloseableHttpClientAccess) context.getThat();
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
      LOG.debug("Can't update http request annotation.", e);
    } finally {
      Tracing.current().endCall();

      maybeStopAsyncTransaction(context);
    }
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
    Trace trace = Tracing.current();
    if (((CloseableHttpClientAccess) context.getThat())._$spm_tracing$_isSolrClient()) {
      HttpClient4SolrRequest.exception(Tracing.current());
    }
    trace.setFailed(true);
    trace.endCall();

    maybeStopAsyncTransaction(context);
  }
}
