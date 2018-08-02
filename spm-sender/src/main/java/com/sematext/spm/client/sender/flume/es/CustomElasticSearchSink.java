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

import static org.apache.flume.sink.elasticsearch.ElasticSearchSinkConstants.*;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import org.apache.commons.lang.StringUtils;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.CounterGroup;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.formatter.output.BucketPath;
import org.apache.flume.instrumentation.SinkCounter;
import org.apache.flume.sink.AbstractSink;
import org.apache.flume.sink.elasticsearch.ElasticSearchEventSerializer;
import org.apache.flume.sink.elasticsearch.ElasticSearchIndexRequestBuilderFactory;
import org.apache.flume.sink.elasticsearch.IndexNameBuilder;
import org.apache.flume.sink.elasticsearch.client.ElasticSearchClient;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.sender.flume.SinkConstants;
import com.sematext.spm.client.sender.util.DynamicUrlParamSink;

/**
 * Copy of flume's ElasticSearchSink, the only difference being which client is used to send data to ES.
 */
public class CustomElasticSearchSink extends AbstractSink implements DynamicUrlParamSink {
  public static final String ES_CONTEXT_ROOT_PARAM = "esContextRootParam";

  public static final String PROXY_HOST = "proxyHost";
  public static final String PROXY_PORT = "proxyPort";
  public static final String PROXY_USERNAME = "proxyUsername";
  public static final String PROXY_PASSWORD = "proxyPassword";

  private static final Log logger = LogFactory
      .getLog(CustomElasticSearchSink.class);

  // Used for testing
  private boolean isLocal = false;
  private final CounterGroup counterGroup = new CounterGroup();

  private static long MAX_TIME_BETWEEN_SENT_BATCHES_MS = 30 * 1000;
  private static long MAX_TIME_BEFORE_SENDING_FIRST_BATCH_MS = 15 * 1000;
  private long lastDataSendingTime = -1;

  private long lastEventTakeTimestamp = System.currentTimeMillis();

  private static final int defaultBatchSize = 100;

  private int batchSize = defaultBatchSize;
  private long ttlMs = DEFAULT_TTL;
  private String clusterName = DEFAULT_CLUSTER_NAME;
  private String indexName = DEFAULT_INDEX_NAME;
  private String indexType = DEFAULT_INDEX_TYPE;
  private String clientType = DEFAULT_CLIENT_TYPE;
  private final Pattern pattern = Pattern.compile(TTL_REGEX,
                                                  Pattern.CASE_INSENSITIVE);
  private Matcher matcher = pattern.matcher("");

  private String[] serverAddresses = null;

  private ElasticSearchClient client = null;
  private Context elasticSearchClientContext = null;

  private ProxyContext proxyContext = new ProxyContext();

  private ElasticSearchIndexRequestBuilderFactory indexRequestFactory;
  private ElasticSearchEventSerializer eventSerializer;
  private IndexNameBuilder indexNameBuilder;
  private SinkCounter sinkCounter;

  private Map<String, String> additionalUrlParams = new UnifiedMap<String, String>();

  private String esContextRoot = null;

  private Status lastSinkProcessStatus = null;

  public CustomElasticSearchSink() {
    this(false);
  }

  @VisibleForTesting CustomElasticSearchSink(boolean isLocal) {
    this.isLocal = isLocal;
  }

  @VisibleForTesting
  String[] getServerAddresses() {
    return serverAddresses;
  }

  @VisibleForTesting
  String getClusterName() {
    return clusterName;
  }

  @VisibleForTesting
  String getIndexName() {
    return indexName;
  }

  @VisibleForTesting
  String getIndexType() {
    return indexType;
  }

  @VisibleForTesting
  long getTTLMs() {
    return ttlMs;
  }

  @VisibleForTesting
  ElasticSearchEventSerializer getEventSerializer() {
    return eventSerializer;
  }

  @VisibleForTesting
  IndexNameBuilder getIndexNameBuilder() {
    return indexNameBuilder;
  }

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
        String realIndexType = BucketPath.escapeString(indexType, event.getHeaders());
        client.addEvent(event, indexNameBuilder, realIndexType, ttlMs);

        if ((System.currentTimeMillis() - lastDataSendingTime) > MAX_TIME_BETWEEN_SENT_BATCHES_MS) {
          logger.info("Taken event: " + realIndexType);
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
    if (!isLocal) {
      if (StringUtils.isNotBlank(context.getString(HOSTNAMES))) {
        serverAddresses = StringUtils.deleteWhitespace(
            context.getString(HOSTNAMES)).split(",");
      }
      Preconditions.checkState(serverAddresses != null
                                   && serverAddresses.length > 0, "Missing Param:" + HOSTNAMES);
    }

    if (StringUtils.isNotBlank(context.getString(INDEX_NAME))) {
      this.indexName = context.getString(INDEX_NAME);
    }

    if (StringUtils.isNotBlank(context.getString(INDEX_TYPE))) {
      this.indexType = context.getString(INDEX_TYPE);
    }

    if (StringUtils.isNotBlank(context.getString(CLUSTER_NAME))) {
      this.clusterName = context.getString(CLUSTER_NAME);
    }

    if (StringUtils.isNotBlank(context.getString(BATCH_SIZE))) {
      this.batchSize = Integer.parseInt(context.getString(BATCH_SIZE));
    }

    if (StringUtils.isNotBlank(context.getString(TTL))) {
      this.ttlMs = parseTTL(context.getString(TTL));
      Preconditions.checkState(ttlMs > 0, TTL
          + " must be greater than 0 or not set.");
    }

    if (StringUtils.isNotBlank(context.getString(CLIENT_TYPE))) {
      clientType = context.getString(CLIENT_TYPE);
    }

    elasticSearchClientContext = new Context();
    elasticSearchClientContext.putAll(context.getSubProperties(CLIENT_PREFIX));

    String serializerClazz = DEFAULT_SERIALIZER_CLASS;
    if (StringUtils.isNotBlank(context.getString(SERIALIZER))) {
      serializerClazz = context.getString(SERIALIZER);
    }

    Context serializerContext = new Context();
    serializerContext.putAll(context.getSubProperties(SERIALIZER_PREFIX));

    try {
      @SuppressWarnings("unchecked")
      Class<? extends Configurable> clazz = (Class<? extends Configurable>) Class
          .forName(serializerClazz);
      Configurable serializer = clazz.newInstance();

      if (serializer instanceof ElasticSearchIndexRequestBuilderFactory) {
        indexRequestFactory
            = (ElasticSearchIndexRequestBuilderFactory) serializer;
        indexRequestFactory.configure(serializerContext);
      } else if (serializer instanceof ElasticSearchEventSerializer) {
        eventSerializer = (ElasticSearchEventSerializer) serializer;
        eventSerializer.configure(serializerContext);
      } else {
        throw new IllegalArgumentException(serializerClazz
                                               + " is not an ElasticSearchEventSerializer");
      }
    } catch (Exception e) {
      logger.error("Could not instantiate event serializer.", e);
      Throwables.propagate(e);
    }

    if (sinkCounter == null) {
      sinkCounter = new SinkCounter(getName());
    }

    String indexNameBuilderClass = DEFAULT_INDEX_NAME_BUILDER_CLASS;
    if (StringUtils.isNotBlank(context.getString(INDEX_NAME_BUILDER))) {
      indexNameBuilderClass = context.getString(INDEX_NAME_BUILDER);
    }

    Context indexnameBuilderContext = new Context();
    serializerContext.putAll(
        context.getSubProperties(INDEX_NAME_BUILDER_PREFIX));

    try {
      @SuppressWarnings("unchecked")
      Class<? extends IndexNameBuilder> clazz
          = (Class<? extends IndexNameBuilder>) Class
          .forName(indexNameBuilderClass);
      indexNameBuilder = clazz.newInstance();
      indexnameBuilderContext.put(INDEX_NAME, indexName);
      indexNameBuilder.configure(indexnameBuilderContext);
    } catch (Exception e) {
      logger.error("Could not instantiate index name builder.", e);
      Throwables.propagate(e);
    }

    if (sinkCounter == null) {
      sinkCounter = new SinkCounter(getName());
    }

    Preconditions.checkState(StringUtils.isNotBlank(indexName),
                             "Missing Param:" + INDEX_NAME);
    Preconditions.checkState(StringUtils.isNotBlank(indexType),
                             "Missing Param:" + INDEX_TYPE);
    Preconditions.checkState(StringUtils.isNotBlank(clusterName),
                             "Missing Param:" + CLUSTER_NAME);
    Preconditions.checkState(batchSize >= 1, BATCH_SIZE
        + " must be greater than 0");

    additionalUrlParams.put(CustomElasticSearchRestClient.URL_PARAM_HOST,
                            context.getString(CustomElasticSearchRestClient.URL_PARAM_HOST));

    String dockerHostname = context.getString(CustomElasticSearchRestClient.URL_PARAM_DOCKER_HOSTNAME, null);
    String containerHostname = context.getString(CustomElasticSearchRestClient.URL_PARAM_CONTAINER_HOSTNAME, null);

    // send these args if they are populated; SPM Solr in docker env would fill both
    if (dockerHostname != null) {
      additionalUrlParams.put(CustomElasticSearchRestClient.URL_PARAM_DOCKER_HOSTNAME, dockerHostname);
    }
    if (containerHostname != null) {
      additionalUrlParams.put(CustomElasticSearchRestClient.URL_PARAM_CONTAINER_HOSTNAME, containerHostname);
    }

    additionalUrlParams.put(CustomElasticSearchRestClient.URL_PARAM_TOKEN,
                            context.getString(CustomElasticSearchRestClient.URL_PARAM_TOKEN));
    additionalUrlParams.put(CustomElasticSearchRestClient.URL_PARAM_VERSION,
                            context.getString(CustomElasticSearchRestClient.URL_PARAM_VERSION));
    additionalUrlParams.put(SinkConstants.URL_PARAM_CONTENT_TYPE,
                            context.getString(SinkConstants.URL_PARAM_CONTENT_TYPE));

    this.esContextRoot = context.getString(ES_CONTEXT_ROOT_PARAM, null);

    proxyContext.setHost(context.getString(PROXY_HOST, null));
    proxyContext.setPort(context.getInteger(PROXY_PORT, null));
    proxyContext.setUsername(context.getString(PROXY_USERNAME, null));
    proxyContext.setPassword(context.getString(PROXY_PASSWORD, null));
  }

  @Override
  public void start() {
    logger.info("ElasticSearch sink {} started");
    sinkCounter.start();
    try {
      client = new CustomElasticSearchRestClient(serverAddresses, esContextRoot, eventSerializer, additionalUrlParams, proxyContext);
      client.configure(elasticSearchClientContext);

      sinkCounter.incrementConnectionCreatedCount();
    } catch (Exception ex) {
      logger.error("Error while starting ElasticSearch sink", ex);
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
    logger.info("ElasticSearch sink {} stopping");
    if (client != null) {
      client.close();
    }
    sinkCounter.incrementConnectionClosedCount();
    sinkCounter.stop();
    super.stop();
  }

  /*
   * Returns TTL value of ElasticSearch index in milliseconds when TTL specifier
   * is "ms" / "s" / "m" / "h" / "d" / "w". In case of unknown specifier TTL is
   * not set. When specifier is not provided it defaults to days in milliseconds
   * where the number of days is parsed integer from TTL string provided by
   * user. <p> Elasticsearch supports ttl values being provided in the format:
   * 1d / 1w / 1ms / 1s / 1h / 1m specify a time unit like d (days), m
   * (minutes), h (hours), ms (milliseconds) or w (weeks), milliseconds is used
   * as default unit.
   * http://www.elasticsearch.org/guide/reference/mapping/ttl-field/.
   *
   * @param ttl TTL value provided by user in flume configuration file for the
   * sink
   *
   * @return the ttl value in milliseconds
   */
  private long parseTTL(String ttl) {
    matcher = matcher.reset(ttl);
    while (matcher.find()) {
      if (matcher.group(2).equals("ms")) {
        return Long.parseLong(matcher.group(1));
      } else if (matcher.group(2).equals("s")) {
        return TimeUnit.SECONDS.toMillis(Integer.parseInt(matcher.group(1)));
      } else if (matcher.group(2).equals("m")) {
        return TimeUnit.MINUTES.toMillis(Integer.parseInt(matcher.group(1)));
      } else if (matcher.group(2).equals("h")) {
        return TimeUnit.HOURS.toMillis(Integer.parseInt(matcher.group(1)));
      } else if (matcher.group(2).equals("d")) {
        return TimeUnit.DAYS.toMillis(Integer.parseInt(matcher.group(1)));
      } else if (matcher.group(2).equals("w")) {
        return TimeUnit.DAYS.toMillis(7 * Integer.parseInt(matcher.group(1)));
      } else if (matcher.group(2).equals("")) {
        logger.info("TTL qualifier is empty. Defaulting to day qualifier.");
        return TimeUnit.DAYS.toMillis(Integer.parseInt(matcher.group(1)));
      } else {
        logger.debug("Unknown TTL qualifier provided. Setting TTL to 0.");
        return 0;
      }
    }
    logger.info("TTL not provided. Skipping the TTL config by returning 0.");
    return 0;
  }

  @Override
  public void updateAdditionalUrlParam(String paramName, String paramValue) {
    ((CustomElasticSearchRestClient) client).updateUrlParameter(paramName, paramValue);
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
