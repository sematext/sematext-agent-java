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

import static com.sematext.spm.client.util.PrimitiveParsers.parseLongOrNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.solrj5.SolrHttpClientParams;

/**
 * It is a trick to handle asynchronous calls made by crazy solrj api.
 * https://gist.github.com/whiter4bbit/a0ba21e9315f1df8f9d1#file-concurrentupdatesolrclient-java-L154.
 * <p>
 * Tested just on one version may not work on other solrj version.
 * {@link com.sematext.spm.client.tracing.agent.pointcuts.solrj5.ConcurrentUpdateSolrClientPointcut} adds fake request parameters to
 * {@link org.apache.solr.client.solrj.request.UpdateRequest} to mark, that it should be called asynchronously. As consequence
 * ConcurrentUpdateSolrClient uses all parameters (including fake ones) to build URL, then it calls apache http client with given
 * url, {@link com.sematext.spm.client.tracing.agent.pointcuts.httpclient4.CloseableHttpClientPointcut} finds those fake request parameters
 * and starts new transaction.
 */
public final class ConcurrentUpdateSolrClientHack {
  private static final Log LOG = LogFactory.getLog(ConcurrentUpdateSolrClientHack.class);

  private ConcurrentUpdateSolrClientHack() {
  }

  /**
   * Starts asynchronous transaction if url contains solr-specific-update-request parameters {@link com.sematext.spm.client.tracing.agent.solrj5.SolrHttpClientParams}.
   * This method not depends on any client implementation so can be reused for other clients.
   *
   * @param request request object which will be used as asynchronous transaction maker
   * @param url     url
   * @return true if asynchronous transaction was started
   */
  public static boolean maybeStartAsyncTransaction(Object request, String url) {
    URI uri;
    try {
      uri = new URI(url);
      String query = uri.getRawQuery();
      Map<String, String> params = new HashMap<String, String>();
      if (query != null) {
        for (String pair : query.split("&")) {
          String[] kv = pair.split("=");
          if (kv.length == 2) {
            params.put(kv[0], kv[1]);
          }
        }
      }
      if (Boolean.parseBoolean(params.get(SolrHttpClientParams.SPM_SOLR_TRACING_ASYNC))) {
        Long traceId = parseLongOrNull(params.get(SolrHttpClientParams.SPM_SOLR_TRACING_TRACE_ID));
        Long callId = parseLongOrNull(params.get(SolrHttpClientParams.SPM_SOLR_TRACING_CALL_ID));
        Boolean sampled = Boolean.parseBoolean(params.get(SolrHttpClientParams.SPM_SOLR_TRACING_SAMPLED));
        if (traceId != null && callId != null) {
          Tracing.registerAsyncTrace(request);
          Tracing.newTrace("/", Call.TransactionType.BACKGROUND, traceId, callId, sampled, true);
          Tracing.current().setAsync(true);
          return true;
        }
      }
    } catch (URISyntaxException e) {
      LOG.error("Can't parse url.", e);
    }
    return false;
  }
}
