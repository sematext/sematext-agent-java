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
package com.sematext.spm.client.unlogger;

import java.util.ArrayList;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.unlogger.LogLine.Key;
import com.sematext.spm.client.util.CollectionUtils;

public abstract class DefaultLogger implements UnloggableLogger {

  private final LogLineCollector logLineCollector;

  private final Log log = LogFactory.getLog(getClass());

  // Now is disables, but on test show a performance boost.
  private static final int LOCAL_BUFFER_SIZE = 0;

  private static final ThreadLocal<ArrayList<LogLine>> LOCAL_BUFFER_HOLDER = new ThreadLocal<ArrayList<LogLine>>() {
    @Override
    protected ArrayList<LogLine> initialValue() {
      return new ArrayList<LogLine>(LOCAL_BUFFER_SIZE);
    }
  };

  private final LogLine.Factory logLineFactory;

  protected DefaultLogger(Map<String, Object> params, Key... possibleLogLineKeys) {
    this(params, LogLine.Factory.make(possibleLogLineKeys));
  }

  protected DefaultLogger(Map<String, Object> params, LogLine.Factory logLineFactory) {
    logLineCollector = (LogLineCollector) params.get(LogLineCollector.WIRING_NAME);
    this.logLineFactory = logLineFactory;
  }

  protected final void log(LogLine line) {
    if (LOCAL_BUFFER_SIZE == 0) {
      logLineCollector.log(line);
    } else {
      ArrayList<LogLine> localBuffer = LOCAL_BUFFER_HOLDER.get();
      localBuffer.add(line);
      if (localBuffer.size() >= LOCAL_BUFFER_SIZE) {
        logLineCollector.log(localBuffer);
      }
      localBuffer.clear();
    }
  }

  protected LogLine makeLogLine(String sectionName) {
    return logLineFactory.make(sectionName);
  }

  protected static String getCaughtClassSimpleName(LoggerContext context) {
    // TODO move `that` to JoinPoint?
    Object that = context.getThat();
    return that != null ? that.getClass().getName() : null;
  }

  protected String getCaughtMethodSimpleName(LoggerContext context) {
    JoinPoint joinPoint = context.getJoinPoint();
    return joinPoint == null ? "" : joinPoint.getShortName();
  }

  @Override
  public void logBefore(LoggerContext context) {
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
  }

  public static Key[] append(Key[] x, Key[] y) {
    return CollectionUtils.join(Key.class, x, y);
  }
}
