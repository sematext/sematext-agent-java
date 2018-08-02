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

package com.sematext.spm.client.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

import java.io.IOException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

public abstract class HttpDataProvider<T> implements DataProvider<T> {
  private String dataRequestUrl;

  public HttpDataProvider(boolean https, String host, String port, String dataRequestUrl,
                          HttpDataSourceAuthentication auth) {
    this.auth = auth;
    this.dataRequestUrl = dataRequestUrl.replace("$HOST", host).replace("$PORT", port).trim();

    String firstSevenChars = this.dataRequestUrl.substring(0, Math.min(7, this.dataRequestUrl.length())).toLowerCase();

    if (firstSevenChars.equals("http://")) {
      if (https) {
        this.dataRequestUrl = "https://" + this.dataRequestUrl.substring("http://".length());
      }
    } else if (firstSevenChars.equals("https:/")) {
      if (!https) {
        this.dataRequestUrl = "http://" + this.dataRequestUrl.substring("https://".length());
      }
    } else {
      if (https) {
        this.dataRequestUrl = "https://" + this.dataRequestUrl;
      } else {
        this.dataRequestUrl = "http://" + this.dataRequestUrl;
      }
    }
  }

  public HttpDataProvider(HttpDataSourceAuthentication auth) {
    this.auth = auth;
  }

  public HttpDataProvider(String dataRequestUrl, HttpDataSourceAuthentication auth) {
    this.auth = auth;
    this.dataRequestUrl = dataRequestUrl;
  }

  private static final Log LOG = LogFactory.getLog(HttpDataProvider.class);

  private static final HttpClient HTTP_CLIENT_INSTANCE;

  static {
    SSLContext sslContext = null;
    try {
      TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
        @Override
        public boolean isTrusted(X509Certificate[] arg0, String arg1) {
          return true;
        }
      };

      //TODO - Improve SSL security by adding hostname verification, etc, when needed
      sslContext = new SSLContextBuilder().loadTrustMaterial(null, acceptingTrustStrategy).build();
    } catch (Throwable thr) {
      LOG.error("Error while setting up SSL connection context - starting up without SSL support when querying services", thr);
    }

    CloseableHttpClient client = HttpClients.custom()
        .setSslcontext(sslContext)
        .setSSLHostnameVerifier(new NoopHostnameVerifier())
        .build();
    HTTP_CLIENT_INSTANCE = client;
  }

  /* authentication method which should be used when sending requests; can be null, in which case no authentication
   * method will be used */
  private HttpDataSourceAuthentication auth;

  public T getData() throws IOException {
    HttpGet httpget = new HttpGet(dataRequestUrl);
    setHeaders(httpget);

    LOG.info("executing request: " + httpget.getRequestLine());

    if (auth != null) {
      auth.decorateRequest(httpget);
    }

    HttpResponse response = HTTP_CLIENT_INSTANCE.execute(httpget);
    LOG.info("result: " + response.getStatusLine());

    if (response.getStatusLine().getStatusCode() == 404) {
      // 404 signals no such product exist, so we'll return null
      // IMPORTANT: we must consume the body anyway, otherwise HttpClient could become "corrupted"
      handleResponse(response);
      return null;
    }

    // shutdown() should be called when the instance is no longer needed, but since we are using
    // a singleton for all requests, we will not call shutdown().
    // httpClient.getConnectionManager().shutdown();

    return handleResponse(response);
  }

  /**
   * subclasses can override this method to provide custom request headers
   */
  protected void setHeaders(HttpRequestBase request) {
  }

  /**
   * Handles any error handling if necessary, returns InputStream for response content
   *
   * @param response
   * @return
   * @throws IOException
   */
  protected abstract T handleResponse(HttpResponse response) throws IOException;

  public synchronized void close() {
//    if (HTTP_CLIENT_INSTANCE != null) {
//      HTTP_CLIENT_INSTANCE.getConnectionManager().shutdown();
//    }
  }

  @Override
  public String toString() {
    return "HttpDataProvider{dataRequestUrl='" + dataRequestUrl + "'}";
  }

  public String getDataRequestUrl() {
    return dataRequestUrl;
  }
}
