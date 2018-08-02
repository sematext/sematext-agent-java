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

import com.sematext.spm.client.tracing.agent.Trace;
import com.sematext.spm.client.tracing.agent.httpclient4.HttpHostAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.HttpRequestAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.HttpResponseAccess;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.SolrAnnotation;
import com.sematext.spm.client.tracing.agent.solrj5.SolrAnnotationMaker;

public final class HttpClient4SolrRequest {
  private HttpClient4SolrRequest() {
  }

  public static void before(Trace trace, HttpHostAccess host, HttpRequestAccess request) {
    trace.setTag(Call.CallTag.SOLR);
    trace.setAnnotation(SolrAnnotationMaker.fromHttpRequest(host, request));
  }

  public static void after(Trace trace, HttpResponseAccess response) {
    final SolrAnnotation annotation = (SolrAnnotation) trace.getAnnotation();
    if (annotation != null) {
      int code = response._$spm_tracing$_getStatusLine()._$spm_tracing$_getStatusCode();
      annotation.setResponseStatus(code);
      annotation.setSucceed(code == 200);
    }
  }

  public static void exception(Trace trace) {
    final SolrAnnotation annotation = (SolrAnnotation) trace.getAnnotation();
    if (annotation != null) {
      annotation.setSucceed(false);
    }
  }
}
