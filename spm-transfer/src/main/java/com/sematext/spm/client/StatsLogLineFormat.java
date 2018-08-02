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

/**
 * Provides utility methods to work with spm sender stats log line format. NOTE: we could create nice object to hold spm
 * log data and provide better interface, but focus here is on performance: reduce resources usage, fast operations.
 * Reason: this is used frequently on sender and (assumed more frequently on receiver side).
 */
public final class StatsLogLineFormat {
  public static final String DELIM = "\t";
  public static final char NULL_VALUE = '!';

  private StatsLogLineFormat() {
  }

  public static String buildSpmLogLineToSend(String originalLogLine, long timestamp, boolean addTimestamp) {
    if (addTimestamp) {
      String time = String.valueOf(timestamp);

      StringBuilder sb = new StringBuilder(originalLogLine.length() + time.length() + DELIM.length() * 1);
      sb.append(time).append(DELIM);
      sb.append(originalLogLine);

      return sb.toString();
    } else {
      return originalLogLine;
    }
  }

  /**
   * Parses log line. Output array contains aray of strings: 1st - statsData 2nd - account name 3rd - account password
   *
   * @param logLine log line created by spm-sender
   * @return parsed log line
   */
  @Deprecated
  public static String[] parseSpmSenderLogLine(String logLine) {
    int firstTokenEnd = logLine.indexOf(DELIM);
    int secondTokenEnd = logLine.indexOf(DELIM, 1 + firstTokenEnd);
    int systemEnd = logLine.indexOf(DELIM, 1 + secondTokenEnd);
    String[] result = new String[4];
    result[0] = logLine.substring(systemEnd + DELIM.length());
    String token1 = logLine.substring(0, firstTokenEnd);
    result[1] = isNull(token1) ? null : token1;
    String token2 = logLine.substring(firstTokenEnd + DELIM.length(), secondTokenEnd);
    result[2] = isNull(token2) ? null : token2;
    String systemId = logLine.substring(secondTokenEnd + DELIM.length(), systemEnd);
    result[3] = isNull(systemId) ? null : systemId;

    return result;
  }

  private static boolean isNull(String val) {
    return val == null || (val.length() == 1 && val.charAt(0) == NULL_VALUE);
  }
}
