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

import java.text.DateFormat;

public final class LogUtils {
  public static String dataFormat = "yyyy-MM-dd HH:mm:ss,SSS";
  public static String messageFormat = "%s %s [%s] %s - %s";

  private LogUtils() {
  }

  public static String format(DateFormat dateFormat, LogFactory.LogLevel level, String name, Object message) {
    return format(dateFormat, level.name(), name, message);
  }

  public static String format(DateFormat dateFormat, String level, String name, Object message) {
    return String.format(messageFormat, dateFormat.format(System.currentTimeMillis()), level, Thread.currentThread()
        .getName(), name, message);
  }

}
