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
package com.sematext.spm.client.tracing.agent.sampling;

import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

public final class StatsReporter {
  private static final Log LOG = LogFactory.getLog(StatsReporter.class);

  public static void start(final Stats stats, final long interval, final TimeUnit timeUnit) {
    final Thread t = new Thread() {
      @Override
      public void run() {
        try {
          while (true) {
            final StringBuilder statistics = new StringBuilder();
            statistics.append("Sampled = ").append(stats.getSampled()).append(", ")
                .append("Total = ").append(stats.getTotal()).append(", ")
                .append("Avg write latency = ").append(stats.getAvgWriteLatency()).append(" ns").append(", ")
                .append("Avg sink latency = ").append(stats.getAvgSinkLatency()).append(" ns");

            LOG.info(statistics.toString());
            Thread.sleep(timeUnit.toMillis(interval));
          }
        } catch (Exception e) {
          /* */
        }
      }
    };
    t.setDaemon(true);
    t.setName("spm-tracing-stats-reporter");

    t.start();
  }
}
