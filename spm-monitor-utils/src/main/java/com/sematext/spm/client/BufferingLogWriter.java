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

import java.util.List;

/**
 * Buffers all log entries into a list (instead of writing to some file or console). Useful when some logging happens
 * before logging system can be initialized and we don't want logs to spill to console or to some tmp file.
 */
public class BufferingLogWriter implements LogWriter<String> {
  private List<BufferedLogEntry> logEntries;

  public BufferingLogWriter(List<BufferedLogEntry> logEntries) {
    this.logEntries = logEntries;
  }

  @Override
  public void write(String logLine) {
    BufferedLogEntry e = new BufferedLogEntry();
    e.setLogLine(logLine);
    logEntries.add(e);
  }

  @Override
  public void write(String logLine, Throwable throwable) {
    BufferedLogEntry e = new BufferedLogEntry();
    e.setLogLine(logLine);
    e.setThrowable(throwable);
    logEntries.add(e);
  }
}

class BufferedLogEntry {
  private String logLine;
  private Throwable throwable;

  public Throwable getThrowable() {
    return throwable;
  }

  public void setThrowable(Throwable throwable) {
    this.throwable = throwable;
  }

  public String getLogLine() {
    return logLine;
  }

  public void setLogLine(String logLine) {
    this.logLine = logLine;
  }
}
