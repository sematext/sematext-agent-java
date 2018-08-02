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
package com.sematext.spm.client.tracing.agent.solrj5;

import com.sematext.spm.client.tracing.agent.httpclient3.HttpClientAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.AbstractHttpClientAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.CloseableHttpClientAccess;

public final class SolrHttpClient {
  private SolrHttpClient() {
  }

  /**
   * Mark apache httpclient 4 instance as solr client - all request will be tracked as solr requests.
   *
   * @param httpClient http client
   */
  public static void markSolrClient(CloseableHttpClientAccess httpClient) {
    httpClient._$spm_tracing$_setSolrClient(true);
  }

  public static void markSolrClient(AbstractHttpClientAccess httpClient) {
    httpClient._$spm_tracing$_setSolrClient(true);
  }

  public static void markSolrClient(HttpClientAccess httpClient) {
    httpClient._$spm_tracing$_setSolrClient(true);
  }
}
