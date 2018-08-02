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

/*CHECKSTYLE:OFF*/
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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
package com.sematext.spm.client.sender.flume.es;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.sink.elasticsearch.ElasticSearchEventSerializer;
import org.apache.flume.sink.elasticsearch.IndexNameBuilder;
import org.apache.flume.sink.elasticsearch.client.ElasticSearchClient;
import org.apache.flume.sink.elasticsearch.client.ElasticSearchRestClient;
import org.apache.flume.sink.elasticsearch.client.RoundRobinList;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.elasticsearch.common.bytes.BytesReference;

import java.security.KeyStore;
import java.util.Arrays;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.sender.bootstrap.AcceptAllSslCertificatesSocketFactory;

/**
 * Custom variation of ElasticSearchRestClient. The difference is in additional parameters added to
 * bulk URLs:
 * - host
 * - token
 * - version (v parameter)
 * - content type (sct parameter, value OS or APP)
 */
public class CustomElasticSearchRestClient implements ElasticSearchClient {
  private static final String INDEX_OPERATION_NAME = "index";
  private static final String INDEX_PARAM = "_index";
  private static final String TYPE_PARAM = "_type";
  private static final String TTL_PARAM = "_ttl";
  private static final String BULK_ENDPOINT = "_bulk";

  public static final String URL_PARAM_HOST = "host";
  public static final String URL_PARAM_TOKEN = "token";
  public static final String URL_PARAM_VERSION = "v";

  public static final String URL_PARAM_DOCKER_HOSTNAME = "dockerHostname";
  public static final String URL_PARAM_CONTAINER_HOSTNAME = "containerHostname";

  private static final Log logger = LogFactory.getLog(ElasticSearchRestClient.class);

  private final ElasticSearchEventSerializer serializer;
  private final RoundRobinList<String> serversList;

  private StringBuilder bulkBuilder;
  private HttpClient httpClient;

  private String contextRoot;

  private Map<String, String> urlParams;

  private String urlParamsString;
  private boolean tokenParamDefined = false;

  /*
   * optimization when there is just a single hostname to which requests are sent; if there are N hostnames, this
   * value will remain null
   */
  private String fixedFullUrl;

  public CustomElasticSearchRestClient(String[] hostNames, String contextRoot,
                                       ElasticSearchEventSerializer serializer, Map<String, String> urlParams,
                                       ProxyContext proxyContext) {

    boolean useHttps = false;

    for (int i = 0; i < hostNames.length; ++i) {
      if (!hostNames[i].contains("http://") && !hostNames[i].contains("https://")) {
        hostNames[i] = "http://" + hostNames[i];
      }

      if (hostNames[i].contains("https://")) {
        useHttps = true;

        // when https is used without speficying port, we have to append port 443 manually (otherwise
        // "connection refused" errors can happen
        String tmp = hostNames[i].substring("https://".length());

        String host = null;
        String rest = null;

        if (tmp.indexOf("/") != -1) {
          host = tmp.substring(0, tmp.indexOf("/"));
          rest = tmp.substring(tmp.indexOf("/"));
        } else {
          host = tmp;
          rest = "";
        }

        if (host.indexOf(":") == -1) {
          host = host + ":443";
        }

        hostNames[i] = "https://" + host + rest;
      }
    }
    this.serializer = serializer;

    serversList = new RoundRobinList<String>(Arrays.asList(hostNames));

    httpClient = createHttpClient(useHttps);

    configureProxy(proxyContext);

    bulkBuilder = new StringBuilder();

    this.urlParams = urlParams;
    this.contextRoot = contextRoot;

    intializeUrlVariables();
  }

  private HttpClient createHttpClient(boolean useHttps) {
    HttpParams httpParams = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
    HttpConnectionParams.setSoTimeout(httpParams, 15000);
    if (!useHttps) {
      return new DefaultHttpClient(httpParams);
    } else {
      SchemeRegistry registry = null;

      try {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        SSLSocketFactory sf = new AcceptAllSslCertificatesSocketFactory(trustStore);
        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", sf, 443));
        registry.register(new Scheme("https", sf, 8443));
        registry.register(new Scheme("https", sf, 4431));
      } catch (Throwable thr) {
        throw new RuntimeException("Can't set ssl env", thr);
      }

      ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(registry);

      return new DefaultHttpClient(manager, httpParams);
    }
  }

  private void configureProxy(ProxyContext proxyContext) {
    boolean useProxy =
        (proxyContext.getHost() != null && !proxyContext.getHost().trim().equals("")) && proxyContext.getPort() != null;
    if (useProxy) {
      HttpHost proxy = new HttpHost(proxyContext.getHost(), proxyContext.getPort());
      httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
    } else {
      httpClient.getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
    }

    boolean useAuth = (proxyContext.getUsername() != null && !proxyContext.getUsername().trim().equals("")) &&
        (proxyContext.getPassword() != null && !proxyContext.getPassword().trim().equals(""));

    if (useProxy && useAuth) {
      ((DefaultHttpClient) httpClient).getCredentialsProvider().setCredentials(
          new AuthScope(proxyContext.getHost(), proxyContext.getPort()),
          new UsernamePasswordCredentials(proxyContext.getUsername(), proxyContext.getPassword()));
    } else {
      ((DefaultHttpClient) httpClient).getCredentialsProvider().clear();
    }
  }

  @VisibleForTesting
  public CustomElasticSearchRestClient(String[] hostNames, String contextRoot,
                                       ElasticSearchEventSerializer serializer, Map<String, String> urlParams,
                                       ProxyContext proxyContext, HttpClient client) {
    this(hostNames, contextRoot, serializer, urlParams, proxyContext);
    httpClient = client;
  }

  @Override
  public void configure(Context context) {
  }

  @Override
  public void close() {
  }

  @Override
  public void addEvent(Event event, IndexNameBuilder indexNameBuilder, String indexType, long ttlMs) throws Exception {
    if (!tokenParamDefined) {
      logger.debug("No token defined, skipping event");
      return;
    }

    BytesReference content = serializer.getContentBuilder(event).bytes();
    Map<String, Map<String, String>> parameters = new UnifiedMap<String, Map<String, String>>();
    Map<String, String> indexParameters = new UnifiedMap<String, String>();
    indexParameters.put(INDEX_PARAM, indexNameBuilder.getIndexName(event));
    indexParameters.put(TYPE_PARAM, indexType);
    if (ttlMs > 0) {
      indexParameters.put(TTL_PARAM, Long.toString(ttlMs));
    }
    parameters.put(INDEX_OPERATION_NAME, indexParameters);

    Gson gson = new Gson();
    synchronized (bulkBuilder) {
      bulkBuilder.append(gson.toJson(parameters));
      bulkBuilder.append("\n");
      bulkBuilder.append(content.toBytesArray().toUtf8());
      bulkBuilder.append("\n");
    }
  }

  @Override
  public void execute() throws Exception {
    int statusCode = 0, triesCount = 0;
    HttpResponse response = null;
    String entity;

    synchronized (bulkBuilder) {
      if (bulkBuilder.length() == 0) {
        // nothing to send
        return;
      }

      entity = bulkBuilder.toString();
      bulkBuilder = new StringBuilder();
    }

    while (statusCode != HttpStatus.SC_OK && triesCount < serversList.size()) {
      triesCount++;
      String host = serversList.get();
      String url = fixedFullUrl != null ? fixedFullUrl : getBulkUrl(host);

      HttpPost httpRequest = new HttpPost(url);
      httpRequest.setEntity(new StringEntity(entity));
      response = httpClient.execute(httpRequest);
      statusCode = response.getStatusLine().getStatusCode();
      logger.info("Status code from elasticsearch: " + statusCode + " for URL: " + url);
      if (response.getEntity() != null)
        logger.debug(
            "Status message from elasticsearch: " + EntityUtils.toString(response.getEntity(), "UTF-8") + " for URL: "
                + url);
    }

    if (statusCode != HttpStatus.SC_OK) {
      if (response.getEntity() != null) {
        throw new EventDeliveryException(EntityUtils.toString(response.getEntity(), "UTF-8"));
      } else {
        throw new EventDeliveryException("Elasticsearch status code was: " + statusCode);
      }
    }
  }

  private String createUrlParamsString() {
    StringBuilder sb = new StringBuilder();
    sb.append("?");

    for (String key : urlParams.keySet()) {
      if (sb.length() != 1) {
        sb.append("&");
      }
      sb.append(key).append("=").append(urlParams.get(key));
    }

    return sb.toString();
  }

  private String getBulkUrl(String host) {

    return host + "/" + (contextRoot != null ? contextRoot + "/" : "") + BULK_ENDPOINT + urlParamsString;
  }

  private void intializeUrlVariables() {
    this.urlParamsString = createUrlParamsString();

    if (serversList.size() == 1) {
      this.fixedFullUrl = getBulkUrl(serversList.get());
    } else {
      this.fixedFullUrl = null;
    }

    if (urlParams.get(URL_PARAM_TOKEN) != null && !urlParams.get(URL_PARAM_TOKEN).trim().equals("")) {
      tokenParamDefined = true;
    }
  }

  public void updateUrlParameter(String paramName, String paramValue) {
    this.urlParams.put(paramName, paramValue);
    intializeUrlVariables();
  }
}
/*CHECKSTYLE:ON*/
