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

import static com.sematext.spm.client.unlogger.LogLine.Key.CLASS_SIMPLE_NAME;
import static com.sematext.spm.client.unlogger.LogLine.Key.DURATION_CPU;
import static com.sematext.spm.client.unlogger.LogLine.Key.DURATION_OWN_CPU;
import static com.sematext.spm.client.unlogger.LogLine.Key.DURATION_OWN_TOTAL;
import static com.sematext.spm.client.unlogger.LogLine.Key.DURATION_TOTAL;
import static com.sematext.spm.client.unlogger.LogLine.Key.ENTER_TIME;
import static com.sematext.spm.client.unlogger.LogLine.Key.METHOD_SIMPLE_NAME;
import static com.sematext.spm.client.unlogger.LogLine.Key.RELATIVE_CALL_DEPTH;
import static com.sematext.spm.client.unlogger.LogLine.Key.RETURN_TYPE;
import static com.sematext.spm.client.unlogger.LogLine.Key.SEQUENCE_ID;
import static com.sematext.spm.client.unlogger.LogLine.Key.THREAD_NAME;

import java.util.Map;

import com.sematext.spm.client.unlogger.LogLine.Key;
import com.sematext.spm.client.unlogger.LoggerContext.Timer;
import com.sematext.spm.client.unlogger.LoggerContext.Timer.Measure;

public class DefaultTraceLogger extends DefaultLogger {

  private static final Key[] LOG_KEYS = new Key[] { SEQUENCE_ID, RELATIVE_CALL_DEPTH, ENTER_TIME, DURATION_TOTAL,
      DURATION_OWN_TOTAL, DURATION_CPU, DURATION_OWN_CPU, RETURN_TYPE, CLASS_SIMPLE_NAME, METHOD_SIMPLE_NAME,
      THREAD_NAME };

  protected DefaultTraceLogger(Map<String, Object> params, Key... possibleLogKeys) {
    super(params, append(LOG_KEYS, possibleLogKeys));
  }

  protected void logAtExit(LogLine line, LoggerContext context, String exitType) {
    line.put(SEQUENCE_ID, context.getVmCallSequenceId());
    line.put(RELATIVE_CALL_DEPTH, context.getRelativeCallDepth());
    Timer timer = context.getTimer();
    line.put(ENTER_TIME, timer.getStartTimeMs());
    line.put(DURATION_TOTAL, timer.getDurationNs(Measure.TOTAL));
    line.put(DURATION_OWN_TOTAL, timer.getDurationNs(Measure.OWN_TOTAL));
    line.put(DURATION_CPU, timer.getDurationNs(Measure.CPU));
    line.put(DURATION_OWN_CPU, timer.getDurationNs(Measure.OWN_CPU));

    line.put(RETURN_TYPE, exitType);
    line.put(CLASS_SIMPLE_NAME, getCaughtClassSimpleName(context));
    line.put(METHOD_SIMPLE_NAME, getCaughtMethodSimpleName(context));
    line.put(THREAD_NAME, Thread.currentThread());
  }

  @Override
  public void logBefore(LoggerContext context) {
    context.getTimer().begin();
  }

  @Override
  public final void logAfter(LoggerContext context, Object returnValue) {
    context.getTimer().end();
    LogLine line = makeLogLine(context.getSection());
    logAtExit(line, context, "exit");
    log(line);
  }

  @Override
  public final void logThrow(LoggerContext context, Throwable throwable) {
    context.getTimer().end();
    LogLine line = makeLogLine(context.getSection());
    logAtExit(line, context, "exitWithThrow" + ":" + throwable.getClass());
    log(line);
  }

}
