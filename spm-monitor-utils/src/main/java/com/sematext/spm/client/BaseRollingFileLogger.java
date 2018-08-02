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

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Writes logs to file
 */
public abstract class BaseRollingFileLogger<T> implements RollingLogWriter<T> {
  private File dir;
  private String fileName;
  private long maxFileSize;
  private long currentFileSize = 0;
  private int maxBackups;
  private DateFormat dateFormat;

  public void init(String basedir, String fileName, long maxFileSize, int maxBackups, DataFormat format,
                   Integer processOrdinal) {
    dateFormat = new SimpleDateFormat(LogUtils.dataFormat);
    dateFormat.setTimeZone(TimeZone.getDefault());
    this.fileName = fileName;
    this.maxFileSize = maxFileSize;
    this.maxBackups = maxBackups;
    dir = new File(basedir);
    if (!dir.exists()) {
      if (!dir.mkdirs()) {
        throw new RuntimeException("Failed to create dir for stats log, dir: " + basedir);
      }
    }

    log("Calling rotate() while CREATING logger for basedir: " +
            basedir + ", fileName: " + fileName, LogFactory.LogLevel.INFO);
    rotate();

    MonitorUtil.adjustPermissions(MonitorUtil.normalizePath(
        basedir + MonitorUtil.PATH_SEPARATOR + MonitorUtil.getMonitorStatsLogFileName(processOrdinal, format)), "777");
  }

  public boolean isDifferent(String basedir, long maxFileSize, int maxBackups, DataFormat format,
                             Integer processOrdinal) {
    return !this.dir.getPath().equals(new File(basedir).getPath()) || this.maxFileSize != maxFileSize ||
        this.maxBackups != maxBackups || !this.fileName
        .equals(MonitorUtil.getMonitorStatsLogFileName(processOrdinal, format));
  }

  public BaseRollingFileLogger() {
  }

  private void rotate() {
    closeWritter();

    File logFile = new File(dir, fileName);
    if (logFile.exists()) {
      log("Rotating, old files found, backing up...", LogFactory.LogLevel.INFO);

      // "shifting" existing ones
      File lastAllowed = new File(logFile.getPath() + "." + maxBackups);
      if (lastAllowed.exists()) {
        if (!lastAllowed.delete()) {
          log("Unable to delete redundant backup file: " + lastAllowed, LogFactory.LogLevel.WARN);
        }
      }

      for (int i = maxBackups - 1; i >= 0; i--) {
        File existing = new File(logFile.getPath() + "." + i);
        if (existing.exists()) {
          File renameTo = new File(logFile.getPath() + "." + (i + 1));
          if (!existing.renameTo(renameTo)) {
            log("Unable to rename backup file: " + existing, LogFactory.LogLevel.WARN);
          }
        }
      }

      if (!logFile.renameTo(new File(logFile.getPath() + ".0"))) {
        log("Unable to rename backup file: " + logFile, LogFactory.LogLevel.WARN);
      }

      log("Rotating, backing up finished", LogFactory.LogLevel.INFO);
    }

    try {
      log("Creating new log file : " + logFile, LogFactory.LogLevel.INFO);

      if (!logFile.createNewFile()) {
        throw new RuntimeException("Failed to create log file, file: " + logFile);
      }

      log("New log file : " + logFile + " created, adjusting permissions...", LogFactory.LogLevel.INFO);
      MonitorUtil.adjustPermissions(MonitorUtil.normalizePath(logFile.getPath()), "777");
      log("New log file : " + logFile + " permissions adjusted...", LogFactory.LogLevel.INFO);
      createWritter(logFile);
    } catch (IOException e) {
      log("Failed to create log file, file: " + logFile, LogFactory.LogLevel.ERROR);
      throw new RuntimeException("Failed to create log file, file: " + logFile);
    }
  }

  @Override
  public synchronized void write(T logLine) {
    currentFileSize += logLineLength(logLine);
    if (currentFileSize > maxFileSize) {
      System.err.println(dateFormat.format(System.currentTimeMillis()) + " "
                             + " INFO: Calling rotate() because of log size while WRITING to log" +
                             ", fileName: " + fileName);
      System.err
          .println(dateFormat.format(System.currentTimeMillis()) + " " + " INFO: currentFileSize = " + currentFileSize +
                       ", maxFileSize = " + maxFileSize);

      rotate();
      currentFileSize = logLineLength(logLine);
    }
    writeInternal(logLine);
  }

  @Override
  public synchronized void write(T logLine, Throwable throwable) {
    currentFileSize += logLineLength(logLine);
    if (currentFileSize > maxFileSize) {
      log("Calling rotate() because of log size while WRITING to log" +
              ", fileName: " + fileName, LogFactory.LogLevel.INFO);
      log("CurrentFileSize = " + currentFileSize +
              ", maxFileSize = " + maxFileSize, LogFactory.LogLevel.INFO);
      rotate();
      currentFileSize = logLineLength(logLine);
    }
    writeInternal(logLine);
    logThrowable(throwable);
  }

  private void log(String message, LogFactory.LogLevel level) {
    System.err.println(LogUtils.format(dateFormat, level, BaseRollingFileLogger.class.getName(), message));
  }

  protected abstract void writeInternal(T logLine);

  protected abstract long logLineLength(T logLine);

  protected abstract void logThrowable(Throwable throwable);

  protected abstract void closeWritter();

  protected abstract void createWritter(File logFile) throws IOException;
}
