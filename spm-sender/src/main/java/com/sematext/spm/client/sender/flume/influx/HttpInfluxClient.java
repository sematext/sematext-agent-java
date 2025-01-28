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
package com.sematext.spm.client.sender.flume.influx;

import org.apache.flume.EventDeliveryException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.SSLContext;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.sender.flume.ProxyContext;
import com.sematext.spm.client.sender.flume.SinkConstants;

public class HttpInfluxClient extends InfluxClient {
  private static final Log logger = LogFactory.getLog(HttpInfluxClient.class);
  
  private static final int CONNECTION_TIMEOUT_MS = 15000;
  private static final int CONNECTION_REQUEST_TIMEOUT_MS = 15000;
  private static final int SOCKET_TIMEOUT_MS = 10000;
  
  private HttpClient httpClient;
  private String fixedFullUrl;
  private String baseServerUrl;
  private RequestConfig requestConfig;
  private boolean isMetricsEndpoint;

  public HttpInfluxClient(String baseServerUrl, String urlPath,
                          Map<String, String> urlParams, ProxyContext proxyContext) {
    boolean useHttps = false;

    if (!baseServerUrl.contains("http://") && !baseServerUrl.contains("https://")) {
      baseServerUrl = "http://" + baseServerUrl;
    }

    if (baseServerUrl.contains("https://")) {
      useHttps = true;

      // when https is used without specifying port, we have to append port 443 manually (otherwise
      // "connection refused" errors can happen
      String tmp = baseServerUrl.substring("https://".length());

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

      baseServerUrl = "https://" + host + rest;
    }

    this.baseServerUrl = baseServerUrl;

    httpClient = createHttpClient(useHttps, proxyContext);

    this.urlParams = urlParams;
    this.urlPath = urlPath;
    
    this.isMetricsEndpoint = urlPath.contains("db=metrics");

    initializeUrlVariables();
  }

  private HttpClient createHttpClient(boolean useHttps, ProxyContext proxyContext) {
    try {
      TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
        @Override
        public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
          return true;
        }
      };

      // TODO we use weakened security here - it does provide some benefits of https, but we don't verify the host etc
      // we should improve the logic here to be more secure when needed

      SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, acceptingTrustStrategy).build();

      boolean useProxy = (proxyContext.getHost() != null && !proxyContext.getHost().trim().equals(""))
          && proxyContext.getPort() != null;
      boolean useAuth = (proxyContext.getUsername() != null && !proxyContext.getUsername().trim().equals("")) &&
          (proxyContext.getPassword() != null && !proxyContext.getPassword().trim().equals(""));

      if (useProxy) {
        CredentialsProvider credsProvider = null;
        if (useAuth) {
          credsProvider = new BasicCredentialsProvider();
          credsProvider.setCredentials(
              new AuthScope(proxyContext.getHost(), proxyContext.getPort()),
              new UsernamePasswordCredentials(proxyContext.getUsername(), proxyContext.getPassword()));
        }

        CloseableHttpClient client = HttpClients.custom()
            .setSslcontext(sslContext)
            .setSSLHostnameVerifier(new NoopHostnameVerifier())
            .setDefaultCredentialsProvider(credsProvider)
            .build();

        HttpHost proxy = new HttpHost(proxyContext.getHost(), proxyContext.getPort(), 
            proxyContext.isSecure() ? "https" : "http");
        requestConfig = getRequestConfig(proxy);

        return client;
      } else {
        CloseableHttpClient client = HttpClients.custom()
            .setSslcontext(sslContext)
            .setSSLHostnameVerifier(new NoopHostnameVerifier())
            .build();
        requestConfig = getRequestConfig(null);
        return client;
      }
    } catch (Throwable thr) {
      throw new RuntimeException("Can't create http client", thr);
    }
  }
  
  private RequestConfig getRequestConfig(HttpHost proxyHost) {
    RequestConfig.Builder requestConfig = RequestConfig.custom();
    
    requestConfig.setConnectTimeout(CONNECTION_TIMEOUT_MS);
    requestConfig.setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS);
    requestConfig.setSocketTimeout(SOCKET_TIMEOUT_MS);
    
    if (proxyHost != null) {
      requestConfig.setProxy(proxyHost);
    }
    
    return requestConfig.build();    
  }

  @Override
  public boolean sendAndHandleResponse(String entity) throws EventDeliveryException {
    try {
      // influxDB.write("database", "", InfluxDB.ConsistencyLevel.ONE, entity);
      int statusCode = 0;
      HttpResponse response = null;

      HttpPost httpRequest = new HttpPost(fixedFullUrl);

      // TODO might be useful to make charset and content type configurable
      httpRequest.setEntity(new StringEntity(entity, "utf-8"));
      httpRequest.setConfig(requestConfig);
      response = httpClient.execute(httpRequest);
      statusCode = response.getStatusLine().getStatusCode();
      logger.info("Status code from server: " + statusCode + " for URL: " + fixedFullUrl);

      String responseBody = null;

      if (response.getEntity() != null) {
        // the following call actually looks into response headers and uses charset/encoding as specified by response itself...
        // meaning, if server returned content in utf-8, it will be properly loaded in Java's unicode; if it was in iso-8859-1, again,
        // the method does the conversion on the fly properly. Value "utf-8" we pass here is just in case server response
        // didn't provide info about charset/encoding, so we by default assume it was utf-8 if server didn't say anything
        responseBody = EntityUtils.toString(response.getEntity(), "utf-8");
        if (logger.isDebugEnabled()) {
          logger.debug("Status message from server: " + responseBody + " for URL: " + fixedFullUrl + ", for sent data: "
                           + entity);
        } else {
          logger.info("Status message from server: " + responseBody + " for URL: " + fixedFullUrl);
        }
      }

      boolean sendingOk = (statusCode >= 200 && statusCode < 300);
      boolean badRequestError = (statusCode >= 400 && statusCode < 500) && statusCode != 407;

      if (!sendingOk) {
        String msg = "Batch rejected by the server with code " + statusCode + " and message: " + responseBody;
        logger.warn(msg);
        if (!badRequestError) {
          // throw (which causes retry) only if it was not a case of bad request (which would fail again)
          throw new EventDeliveryException(msg);
        }
      }
      
      logger.info("Batch of size " + entity.length() + " chars successfully sent");
      return sendingOk;
    } catch (EventDeliveryException ede) {
      throw ede;
    } catch (Throwable thr) {
      // TODO we probably need different checks here... if data was rejected for valid reasons (app disabled, format bad...), we
      // should not propagate the error (otherwise it would be resent by flume); for now, just this one check
      if (thr.getMessage().contains("unable to parse")) {
        logger.warn("Data rejected by server: " + thr.getMessage());
        logger.warn("Data will not be resent since rejection was valid");
        return false;
      } else {
        throw new EventDeliveryException("Data sending failed to endpoint " + fixedFullUrl, thr);
      }
    }
  }

  @Override
  protected void initializeUrlVariables() {
    this.urlParamsString = createUrlParamsString();
    this.fixedFullUrl = getBulkUrl(baseServerUrl, urlPath, urlParamsString);
  }

  private String createUrlParamsString() {
    StringBuilder sb = new StringBuilder();

    for (String key : urlParams.keySet()) {
      if (InfluxClient.URL_PARAM_VERSION.equals(key) || InfluxClient.URL_PARAM_CONTENT_TYPE.equals(key) ||
          SinkConstants.URL_PARAM_CONTENT_TYPE.equals(key)) {
        if (sb.length() != 1) {
          sb.append("&");
        }
        sb.append(key).append("=").append(urlParams.get(key));
      }
    }

    return sb.toString();
  }

  public static String getBulkUrl(String baseServerUrl, String urlPath, String urlParamsString) {
    String url = baseServerUrl + (baseServerUrl.endsWith("/") ? "" : (urlPath.equals("") ? "" : "/")) +
        (urlPath != null ? (urlPath.startsWith("/") ? urlPath.substring(1) : urlPath) : "");
    
    url = url + (url.indexOf("?") == -1 ? "?" + urlParamsString : 
      (url.indexOf("?") == url.length() - 1 ? urlParamsString :
        (url.endsWith("&") || urlParamsString.startsWith("&") ? urlParamsString : "&" + urlParamsString)));
   
    return url;
  }
  
  @Override
  protected boolean isMetricsEndpoint() {
    return isMetricsEndpoint;
  }

  @Override
  public void close() {
  }
}
