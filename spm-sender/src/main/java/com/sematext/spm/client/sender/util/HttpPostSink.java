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
package com.sematext.spm.client.sender.util;

import org.apache.commons.codec.binary.Hex;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.sink.AbstractSink;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.sender.flume.SinkConstants;
import com.sematext.spm.client.sender.flume.es.CustomElasticSearchRestClient;

public class HttpPostSink extends AbstractSink implements DynamicUrlParamSink {
  private static final Log LOG = LogFactory.getLog(HttpPostSink.class);

  private String url;
  private String fullUrlParamsString;
  private String contentType;
  private int batchSize;

  private Map<String, String> urlParams = new UnifiedMap<String, String>();

  private long lastEventTakeTimestamp = System.currentTimeMillis();

  @Override
  public void configure(Context context) {
    url = context.getString("http.post.sink.url");
    batchSize = context.getInteger("http.post.sink.batch.size", 1);
    contentType = context.getString("http.post.sink.content.type", "application/octet-stream");

    urlParams.put(CustomElasticSearchRestClient.URL_PARAM_HOST,
                  context.getString(CustomElasticSearchRestClient.URL_PARAM_HOST));

    String dockerHostname = context.getString(CustomElasticSearchRestClient.URL_PARAM_DOCKER_HOSTNAME, null);
    String containerHostname = context.getString(CustomElasticSearchRestClient.URL_PARAM_CONTAINER_HOSTNAME, null);

    // send these args if they are populated; SPM Solr in docker env would fill both
    if (dockerHostname != null) {
      urlParams.put(CustomElasticSearchRestClient.URL_PARAM_DOCKER_HOSTNAME, dockerHostname);
    }
    if (containerHostname != null) {
      urlParams.put(CustomElasticSearchRestClient.URL_PARAM_CONTAINER_HOSTNAME, containerHostname);
    }

    urlParams.put(CustomElasticSearchRestClient.URL_PARAM_TOKEN,
                  context.getString(CustomElasticSearchRestClient.URL_PARAM_TOKEN));
    urlParams.put(CustomElasticSearchRestClient.URL_PARAM_VERSION,
                  context.getString(CustomElasticSearchRestClient.URL_PARAM_VERSION));
    urlParams.put(SinkConstants.URL_PARAM_CONTENT_TYPE,
                  context.getString(SinkConstants.URL_PARAM_CONTENT_TYPE));

    intializeUrlVariables();
  }

  protected void post(List<Event> events) throws IOException {
    final HttpURLConnection c = (HttpURLConnection) new URL(fullUrlParamsString).openConnection();
    c.setRequestProperty("Content-Type", contentType);
    c.setDoInput(true);
    c.setDoOutput(true);

    final OutputStream os = c.getOutputStream();
    for (final Event event : events) {
      os.write(event.getBody());
    }

    int rc = c.getResponseCode() - 200;
    if (rc >= 0 && rc < 100) {
      LOG.info(
          events.size() + " are posted to url " + url + " successfully, response code: " + c.getResponseCode() + ".");
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug(events.size() + " can't be posted to url " + url + ", response code: " + c.getResponseCode() + ".");
      }

      throw new IOException("Non success http response: " + c.getResponseCode() + ".");
    }
  }

  @Override
  public synchronized void start() {
    super.start();
    if (LOG.isDebugEnabled()) {
      LOG.debug("HttpPostSink started.");
    }
  }

  @Override
  public synchronized void stop() {
    super.stop();
    if (LOG.isDebugEnabled()) {
      LOG.debug("HttpPostSink stopped.");
    }
  }

  @Override
  public Status process() throws EventDeliveryException {
    Status status = Status.READY;
    final Channel channel = getChannel();
    final Transaction txn = channel.getTransaction();
    txn.begin();
    try {
      final List<Event> events = new FastList<Event>();
      while (events.size() < batchSize) {
        final Event event = channel.take();
        if (event != null) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Event: [" + Hex.encodeHexString(event.getBody()) + "]");
          }
          events.add(event);
          lastEventTakeTimestamp = System.currentTimeMillis();
        } else {
          break;
        }
      }

      if (events.isEmpty()) {
        status = Status.BACKOFF;
      } else {
        post(events);
      }

      txn.commit();
    } catch (Throwable t) {
      LOG.error("Can't post data to url '" + url + "'.", t);

      txn.rollback();

      status = Status.BACKOFF;
    } finally {
      txn.close();
    }
    return status;
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

  private void intializeUrlVariables() {
    this.fullUrlParamsString = url + createUrlParamsString();
  }

  @Override
  public void updateAdditionalUrlParam(String key, String value) {
    urlParams.put(key, value);
    intializeUrlVariables();
  }

  @Override
  public long getLastEventTakeTimestamp() {
    return lastEventTakeTimestamp;
  }

  public String getUrl() {
    return url;
  }

  public String getFullUrlParamsString() {
    return fullUrlParamsString;
  }

  public String getContentType() {
    return contentType;
  }
}
