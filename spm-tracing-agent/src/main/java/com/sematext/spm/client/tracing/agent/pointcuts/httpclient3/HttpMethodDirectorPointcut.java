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

import com.sematext.spm.client.tracing.agent.Trace;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.httpclient3.HeaderAccess;
import com.sematext.spm.client.tracing.agent.httpclient3.HttpMethodAccess;
import com.sematext.spm.client.tracing.agent.httpclient3.HttpMethodDirectorAccess;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.HttpHeaders;
import com.sematext.spm.client.tracing.agent.model.HttpHeaders.CrossAppCallHeader;
import com.sematext.spm.client.tracing.agent.model.annotation.HTTPRequestAnnotation;
import com.sematext.spm.client.tracing.agent.pointcuts.solrj5.HttpClient3SolrRequest;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;

@LoggerPointcuts(name = "http-method-director-pointcut", methods = {
    "void org.apache.commons.httpclient.HttpMethodDirector#executeMethod(org.apache.commons.httpclient.HttpMethod method)"
})
public class HttpMethodDirectorPointcut implements UnloggableLogger {
  @Override
  public void logBefore(LoggerContext context) {
    Trace trace = Tracing.current();
    trace.newCall(context.getJoinPoint());
    trace.setTag(Call.CallTag.HTTP_REQUEST);

    final HttpMethodDirectorAccess that = (HttpMethodDirectorAccess) context.getThat();
    final HttpMethodAccess httpMethod = (HttpMethodAccess) context.getAllParams()[0];
    if (httpMethod != null) {
      final String url = that._$spm_tracing$_getHostConfiguration()._$spm_tracing$_getHostURL();
      final String path = httpMethod._$spm_tracing$_getPath();
      if (httpMethod._$spm_tracing$_isSolrClient()) {
        HttpClient3SolrRequest.before(trace, url + path);
      } else {
        final HTTPRequestAnnotation annotation = HTTPRequestAnnotation.request(url + path);
        annotation.setMethod(httpMethod._$spm_tracing$_getName());
        trace.setAnnotation(annotation);
      }

      httpMethod._$spm_tracing$_setRequestHeader(HttpHeaders.SPM_TRACING_SAMPLED, String.valueOf(trace.isSampled()));
      httpMethod._$spm_tracing$_setRequestHeader(HttpHeaders.SPM_TRACING_TRACE_ID, String.valueOf(trace.getTraceId()));
      httpMethod._$spm_tracing$_setRequestHeader(HttpHeaders.SPM_TRACING_CALL_ID, String.valueOf(trace.getCallId()));
    }
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
    Trace trace = Tracing.current();
    final HttpMethodAccess httpMethod = (HttpMethodAccess) context.getAllParams()[0];
    if (httpMethod != null) {
      if (httpMethod._$spm_tracing$_isSolrClient()) {
        HttpClient3SolrRequest.after(trace, httpMethod);
      } else {
        final HTTPRequestAnnotation annotation = (HTTPRequestAnnotation) trace.getAnnotation();
        if (annotation != null) {
          annotation.setResponseCode(httpMethod._$spm_tracing$_getStatusCode());
        }
      }
      HeaderAccess header = httpMethod._$spm_tracing$_getResponseHeader(HttpHeaders.SPM_TRACING_CROSS_APP_CALL);
      if (header != null) {
        final CrossAppCallHeader crossAppHeader = HttpHeaders
            .decodeCrossAppCallHeader(header._$spm_tracing$_getValue());
        trace.setCrossAppInHeader(crossAppHeader);
      }
    }
    trace.endCall();
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
    Trace trace = Tracing.current();
    final HttpMethodAccess method = (HttpMethodAccess) context.getThat();
    if (method._$spm_tracing$_isSolrClient()) {
      HttpClient3SolrRequest.exception(trace);
    }
    trace.setFailed(true);
    trace.endCall();
  }
}
