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
package com.sematext.spm.client;

public final class StatsLoggingRegulator {
  private StatsLoggingRegulator() {
  }

  private static final long ONE_MINUTE_MS = 60 * 1000;
  private static final long EVERY_NTH_MINUTE = 10;

  private static final Log LOG = LogFactory.getLog(StatsLoggingRegulator.class);

  private static long NEXT_ENABLED_MINUTE = -1;

  public static synchronized boolean shouldLogStats() {
    String logLevel = LogFactory.getLoggingLevel();
    if (LogFactory.LogLevel.DEBUG.toString().equalsIgnoreCase(logLevel) || LogFactory.LogLevel.TRACE.toString()
        .equalsIgnoreCase(logLevel)) {
      return true;
    }

    long currentMinute = System.currentTimeMillis() / ONE_MINUTE_MS;

    if (NEXT_ENABLED_MINUTE == -1) {
      NEXT_ENABLED_MINUTE = currentMinute;
    }

    if (currentMinute < NEXT_ENABLED_MINUTE) {
      return false;
    } else if (currentMinute > NEXT_ENABLED_MINUTE) {
      NEXT_ENABLED_MINUTE = currentMinute + EVERY_NTH_MINUTE;
      LOG.info("Next stats logging minute = " + NEXT_ENABLED_MINUTE + ", current minute is = " + currentMinute);
      return false;
    } else {
      return true;
    }
  }
}
