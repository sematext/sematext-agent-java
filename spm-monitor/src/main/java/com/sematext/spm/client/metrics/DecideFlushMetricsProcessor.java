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
package com.sematext.spm.client.metrics;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.StatValues;

public class DecideFlushMetricsProcessor implements MetricsProcessor {
  private static final Log LOG = LogFactory.getLog(DecideFlushMetricsProcessor.class);
  private static final long ONE_MINUTE_MS = 60 * 1000;

  private static long lastMinuteWithFlushMessage = 0;

  private long lastFlushMinute = 0;
  private long monitorCollectionInterval;

  public DecideFlushMetricsProcessor(long monitorCollectionInterval) {
    this.monitorCollectionInterval = monitorCollectionInterval;
  }

  @Override
  public void process(MetricsProcessorContext context) {
    StatValues statValues = context.statValues;

    // we have to do the logic based on what would be recorded timestamp
    long currentTime = statValues.getTimestamp();

    long currentMinute = currentTime / ONE_MINUTE_MS;
    long expectedNextCollectionTime = currentTime + monitorCollectionInterval;
    long expectedNextCollectionMinute = expectedNextCollectionTime / ONE_MINUTE_MS;

    boolean lastMeasurementInCurrentMinute = expectedNextCollectionMinute > currentMinute;
    boolean previousMinuteNotFlushed = (currentMinute - lastFlushMinute) >= 2;
    boolean flushMetrics = lastMeasurementInCurrentMinute || previousMinuteNotFlushed;

    if (previousMinuteNotFlushed) {
      if (currentMinute > lastMinuteWithFlushMessage) {
        LOG.info(
            "Previous minute data not flushed yet, adjusting the minute... for collector " + context.collectorName + ":"
                +
                " CurrentMinute=" + currentMinute + ", expectedNextCollectionMinute=" + expectedNextCollectionMinute +
                ", lastMeasurementInCurrentMinute=" + lastMeasurementInCurrentMinute + ", lastFlushMinute="
                + lastFlushMinute +
                ", previousMinuteNotFlushed=" + previousMinuteNotFlushed + ", flushMetrics=" + flushMetrics);
        lastMinuteWithFlushMessage = currentMinute;
      }

      // also adjust the timestamp to last second of previous minute
      currentTime = currentMinute * ONE_MINUTE_MS - 1000;
      currentMinute = currentMinute - 1;
      statValues.setTimestamp(currentTime);
    }

    context.shouldFlush = flushMetrics;

    if (flushMetrics) {
      lastFlushMinute = currentMinute;
    }
  }
}
