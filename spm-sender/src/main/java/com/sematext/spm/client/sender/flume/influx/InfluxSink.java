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

import com.google.common.base.Throwables;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.CounterGroup;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.instrumentation.SinkCounter;
import org.apache.flume.sink.AbstractSink;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.sender.flume.ProxyContext;
import com.sematext.spm.client.sender.flume.SinkConstants;
import com.sematext.spm.client.sender.util.DynamicUrlParamSink;

/*CHECKSTYLE:OFF*/
public class InfluxSink extends AbstractSink implements DynamicUrlParamSink {
  public static final String INFLUX_SERVER = "influxServer";
  public static final String INFLUX_ENDPOINT_PATH = "influxEndpointPath";

  public static final String PROXY_HOST = "proxyHost";
  public static final String PROXY_PORT = "proxyPort";
  public static final String PROXY_USERNAME = "proxyUsername";
  public static final String PROXY_PASSWORD = "proxyPassword";
  public static final String PROXY_SECURE = "proxySecure";

  private static final Log logger = LogFactory.getLog(InfluxSink.class);

  private final CounterGroup counterGroup = new CounterGroup();

  private static long MAX_TIME_BETWEEN_SENT_BATCHES_MS = 30 * 1000;
  private static long MAX_TIME_BEFORE_SENDING_FIRST_BATCH_MS = 15 * 1000;
  private long lastDataSendingTime = -1;

  private long lastEventTakeTimestamp = System.currentTimeMillis();

  private static final int defaultBatchSize = 100;

  private int batchSize = defaultBatchSize;

  private String influxHost = null;

  private InfluxClient client = null;

  private ProxyContext proxyContext = new ProxyContext();
  private String urlPath;

  private SinkCounter sinkCounter;

  private Map<String, String> additionalUrlParams = new UnifiedMap<String, String>();

  private Status lastSinkProcessStatus = null;

  @Override
  public Status process() throws EventDeliveryException {
    logger.debug("processing...");

    if (lastDataSendingTime == -1) {
      // if no batches were sent yet, set lastDataSendingTime in a way to ensure first data will be
      // sent max MAX_TIME_BEFORE_SENDING_FIRST_BATCH_MS ms after starting the sink.
      lastDataSendingTime = System.currentTimeMillis() -
          (MAX_TIME_BETWEEN_SENT_BATCHES_MS - MAX_TIME_BEFORE_SENDING_FIRST_BATCH_MS);
    }

    Status status = Status.READY;
    boolean timeTriggeredBatchSending = false;
    Channel channel = getChannel();
    Transaction txn = channel.getTransaction();
    try {
      txn.begin();
      int count;
      for (count = 0; count < batchSize; ++count) {
        Event event = channel.take();

        if (event == null) {
          break;
        }
        lastEventTakeTimestamp = System.currentTimeMillis();
        client.addEvent(event);

        if ((System.currentTimeMillis() - lastDataSendingTime) > MAX_TIME_BETWEEN_SENT_BATCHES_MS) {
          logger.info("Max time between two batches passed : " + MAX_TIME_BETWEEN_SENT_BATCHES_MS +
                          ", sending " + count + " events for " + additionalUrlParams);
          timeTriggeredBatchSending = true;
          count++;
          break;
        }
      }

      if (count <= 0) {
        sinkCounter.incrementBatchEmptyCount();
        counterGroup.incrementAndGet("channel.underflow");
        status = Status.BACKOFF;
      } else {
        if (count < batchSize && !timeTriggeredBatchSending) {
          sinkCounter.incrementBatchUnderflowCount();
          status = Status.BACKOFF;
        } else {
          // in case batch reached batchSize or batch sending was triggered by time
          sinkCounter.incrementBatchCompleteCount();
        }

        sinkCounter.addToEventDrainAttemptCount(count);
        lastDataSendingTime = System.currentTimeMillis();
        client.execute();
      }
      txn.commit();
      sinkCounter.addToEventDrainSuccessCount(count);
      counterGroup.incrementAndGet("transaction.success");
    } catch (Throwable ex) {
      lastDataSendingTime = System.currentTimeMillis();
      try {
        txn.rollback();
        counterGroup.incrementAndGet("transaction.rollback");
      } catch (Exception ex2) {
        logger.error(
            "Exception in rollback. Rollback might not have been successful.",
            ex2);
      }

      if (ex instanceof Error || ex instanceof RuntimeException) {
        logger.error("Failed to commit transaction. Transaction rolled back.",
                     ex);
        Throwables.propagate(ex);
      } else {
        logger.error("Failed to commit transaction. Transaction rolled back.",
                     ex);
        throw new EventDeliveryException(
            "Failed to commit transaction. Transaction rolled back.", ex);
      }
    } finally {
      txn.close();
    }

    lastSinkProcessStatus = status;

    return status;
  }

  @Override
  public void configure(Context context) {
    if (sinkCounter == null) {
      sinkCounter = new SinkCounter(getName());
    }

    influxHost = context.getString(INFLUX_SERVER);
    urlPath = context.getString(INFLUX_ENDPOINT_PATH);

    proxyContext.setHost(context.getString(PROXY_HOST, null));
    proxyContext.setPort(context.getInteger(PROXY_PORT, null));
    proxyContext.setUsername(context.getString(PROXY_USERNAME, null));
    proxyContext.setPassword(context.getString(PROXY_PASSWORD, null));
    proxyContext.setSecure(context.getBoolean(PROXY_SECURE, false));

    String version = context.getString(InfluxClient.URL_PARAM_VERSION);
    String contentType = context.getString(SinkConstants.URL_PARAM_CONTENT_TYPE);
    if (version != null) {
      additionalUrlParams.put(InfluxClient.URL_PARAM_VERSION, version);
    }
    if (contentType != null) {
      additionalUrlParams.put(SinkConstants.URL_PARAM_CONTENT_TYPE, contentType);
    }
  }

  @Override
  public void start() {
    logger.info("Influx sink {} started");
    sinkCounter.start();
    try {
      client = new HttpInfluxClient(influxHost, urlPath, additionalUrlParams, proxyContext);

      sinkCounter.incrementConnectionCreatedCount();
    } catch (Exception ex) {
      logger.error("Error while starting Influx sink", ex);
      sinkCounter.incrementConnectionFailedCount();
      if (client != null) {
        client.close();
        sinkCounter.incrementConnectionClosedCount();
      }
    }

    super.start();
  }

  @Override
  public void stop() {
    logger.info("Influx sink {} stopping");
    if (client != null) {
      client.close();
    }
    sinkCounter.incrementConnectionClosedCount();
    sinkCounter.stop();
    super.stop();
  }

  @Override
  public void updateAdditionalUrlParam(String paramName, String paramValue) {
    ((InfluxClient) client).updateUrlParameter(paramName, paramValue);
  }

  @Override
  public long getLastEventTakeTimestamp() {
    return lastEventTakeTimestamp;
  }

  public Status getLastSinkProcessStatus() {
    return lastSinkProcessStatus;
  }
}
/*CHECKSTYLE:ON*/
