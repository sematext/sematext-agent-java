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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.sematext.spm.client.tracing.agent.httpclient4.HttpClientURL;
import com.sematext.spm.client.tracing.agent.httpclient4.HttpHostAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.HttpRequestAccess;
import com.sematext.spm.client.tracing.agent.model.SolrAnnotation;

public final class SolrAnnotationMaker {
  private SolrAnnotationMaker() {
  }

  private static enum RequestHandler {
    SCHEMA("/schema", SolrAnnotation.RequestType.SCHEMA, true),
    COLLECTION_ADMIN("/admin/collections", SolrAnnotation.RequestType.COLLECTION_ADMIN, true),
    CORE_ADMIN("/admin/cores", SolrAnnotation.RequestType.CORE_ADMIN, false),
    UPDATE("/update", SolrAnnotation.RequestType.UPDATE, true),
    SELECT("/select", SolrAnnotation.RequestType.QUERY, true),
    QUERY("/query", SolrAnnotation.RequestType.QUERY, true),
    DOC_ANALYSIS("/analysis/field", SolrAnnotation.RequestType.ANALYSIS, true),
    FIELD_ANALYSIS("/analysis/field", SolrAnnotation.RequestType.ANALYSIS, true);

    final SolrAnnotation.RequestType type;
    final String prefix;
    final boolean hasCollection;

    RequestHandler(String prefix, SolrAnnotation.RequestType type, boolean hasCollection) {
      this.type = type;
      this.prefix = prefix;
      this.hasCollection = hasCollection;
    }
  }

  public static SolrAnnotation fromHttpRequest(HttpHostAccess host, HttpRequestAccess request) {
    return fromHTTPRequest(HttpClientURL.makeUrl(host, request._$spm_tracing$_getRequestLine()));
  }

  public static SolrAnnotation fromHTTPRequest(String url) {
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      return null;
    }
    String path = uri.getRawPath();
    if (path == null) {
      return null;
    }
    if (path.endsWith("/")) {
      path = path.substring(0, path.length());
    }

    String collection = null;

    SolrAnnotation.RequestType requestType = SolrAnnotation.RequestType.OTHER;

    for (RequestHandler h : RequestHandler.values()) {
      if (path.endsWith(h.prefix)) {
        if (h.hasCollection) {
          String[] paths = path.substring(0, path.lastIndexOf(h.prefix)).split("/");
          if (paths.length > 0 && !paths[paths.length - 1].isEmpty()) {
            collection = paths[paths.length - 1];
          }
        }
        requestType = h.type;
      }
    }

    final SolrAnnotation annotation = new SolrAnnotation();
    annotation.setUrl(url);
    annotation.setRequestType(requestType);
    annotation.setCollection(collection);

    final Map<String, String> params = new HashMap<String, String>();

    if (uri.getRawQuery() != null) {
      String[] pairs = uri.getRawQuery().split("&");
      for (String pair : pairs) {
        String[] kv = pair.split("=");

        if (kv.length == 2) {
          params.put(kv[0].trim(), kv[1].trim());
        }
      }
    }

    annotation.setParams(params);

    return annotation;
  }
}
