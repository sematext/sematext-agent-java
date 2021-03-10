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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * This is a wrapper for logging systems. There are known issues with using log4j & commons-logging when adding
 * javaagent to jetty 6
 * <p/>
 * TODO Replace this logging in favour of logback, we can create bootstrapped agents without any conflicts
 */
public final class LogFactory {
  private static List<BufferedLogEntry> bufferedLogEntries = new ArrayList<BufferedLogEntry>();
  private static LogWriter<String> current = new BufferingLogWriter(bufferedLogEntries);
  private static String logLevel = LogLevel.INFO.name();

  public static enum LogLevel {
    FATAL, ERROR, WARN, INFO, DEBUG, TRACE
  }

  /**
   * used in tests
   */
  public static synchronized void init(LogWriter<String> logWriter) {
    current = logWriter;

    dumpBufferedLogEntries();
  }

  public static synchronized void initStdoutLogger(String logLevelParam, DataFormat format) {
    current = new StdOutLogWriter();

    setLoggingLevel(System.getProperty("spm.monitor.loglevel", logLevelParam));

    dumpBufferedLogEntries();
  }

  public static synchronized void initFileLogger(String basedir, long maxFileSize, int maxBackups, String logLevelParam,
                                       DataFormat format, Integer processOrdinal) {
    String logFileName = String.format("spm-monitor-%s.%s", processOrdinal, format.getFileExtension());
    RollingFileLogger newCurrent = new RollingFileLogger();
    newCurrent.init(basedir, logFileName, maxFileSize, maxBackups, format, processOrdinal);

    current = newCurrent;

    setLoggingLevel(System.getProperty("spm.monitor.loglevel", logLevelParam));

    MonitorUtil.adjustPermissions(MonitorUtil.normalizePath(
        basedir + MonitorUtil.LINE_SEPARATOR + logFileName), "777");

    dumpBufferedLogEntries();
  }

  public static synchronized void setLoggingLevel(String logLevelParam) {
    if ("reduced".equalsIgnoreCase(logLevelParam)) {
      logLevel = "ERROR";
    } else if (logLevelParam == null) {
      logLevel = LogLevel.INFO.name();
    } else {
      logLevel = logLevelParam;
    }
  }

  public static synchronized String getLoggingLevel() {
    return logLevel;
  }

  private static void dumpBufferedLogEntries() {
    if (bufferedLogEntries != null) {
      synchronized (bufferedLogEntries) {
        if (bufferedLogEntries != null) {
          for (BufferedLogEntry bufferedLogEntry : bufferedLogEntries) {
            if (bufferedLogEntry.getThrowable() == null) {
              current.write(bufferedLogEntry.getLogLine());
            } else {
              current.write(bufferedLogEntry.getLogLine(), bufferedLogEntry.getThrowable());
            }
          }

          bufferedLogEntries.clear();
          bufferedLogEntries = null;
        }
      }
    }
  }

  protected static class SimpleLogImpl implements Log {
    private String name;
    private DateFormat dateFormat;
    private LogLevel logLevel;

    protected SimpleLogImpl(String name) {
      this(name, LogLevel.INFO);
    }

    protected SimpleLogImpl(String name, LogLevel logLevel) {
      this.name = name;
      dateFormat = new SimpleDateFormat(LogUtils.dataFormat);
      dateFormat.setTimeZone(TimeZone.getDefault());
      this.logLevel = logLevel;
    }

    private boolean enabled(LogLevel level) {
      return logLevel.ordinal() >= level.ordinal();
    }

    @Override
    public boolean isDebugEnabled() {
      return enabled(LogLevel.DEBUG);
    }

    @Override
    public boolean isErrorEnabled() {
      return enabled(LogLevel.ERROR);
    }

    @Override
    public boolean isFatalEnabled() {
      return enabled(LogLevel.FATAL);
    }

    @Override
    public boolean isInfoEnabled() {
      return enabled(LogLevel.INFO);
    }

    @Override
    public boolean isTraceEnabled() {
      return enabled(LogLevel.TRACE);
    }

    @Override
    public boolean isWarnEnabled() {
      return enabled(LogLevel.WARN);
    }

    protected void write(String level, Object message) {
      current.write(LogUtils.format(dateFormat, level, name, message));
    }

    protected void write(String level, Object message, Throwable t) {
      current.write(LogUtils.format(dateFormat, level, name, message), t);
    }

    @Override
    public void trace(Object message) {
      if (isTraceEnabled()) {
        write("TRACE", message);
      }
    }

    @Override
    public void trace(Object message, Throwable t) {
      if (isTraceEnabled()) {
        write("TRACE", message, t);
      }
    }

    @Override
    public void debug(Object message) {
      if (isDebugEnabled()) {
        write("DEBUG", message);
      }
    }

    @Override
    public void debug(Object message, Throwable t) {
      if (isDebugEnabled()) {
        write("DEBUG", message, t);
      }
    }

    @Override
    public void info(Object message) {
      if (isInfoEnabled()) {
        write("INFO", message);
      }
    }

    @Override
    public void info(Object message, Throwable t) {
      if (isInfoEnabled()) {
        write("INFO", message, t);
      }
    }

    @Override
    public void warn(Object message) {
      if (isWarnEnabled()) {
        write("WARN", message);
      }
    }

    @Override
    public void warn(Object message, Throwable t) {
      if (isWarnEnabled()) {
        write("WARN", message, t);
      }
    }

    @Override
    public void error(Object message) {
      if (isErrorEnabled()) {
        write("ERROR", message);
      }
    }

    @Override
    public void error(Object message, Throwable t) {
      if (isErrorEnabled()) {
        write("ERROR", message, t);
      }
    }

    @Override
    public void fatal(Object message) {
      if (isFatalEnabled()) {
        write("FATAL", message);
      }
    }

    @Override
    public void fatal(Object message, Throwable t) {
      if (isFatalEnabled()) {
        write("FATAL", message, t);
      }
    }

    @Override
    public void printStackTrace(Exception e) {
      if (isErrorEnabled() && e != null) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));

        write("ERROR", errors.toString(), e);
      }
    }
  }

  protected static class ReducedSimpleLogImpl extends DeduplicatingErrorsLogImpl {
    protected ReducedSimpleLogImpl(String name) {
      super(name, LogLevel.ERROR);
    }
  }

  /**
   * possible solution to reduce amount of error messages written to monitor logs; not used yet
   */
  protected static class DeduplicatingErrorsLogImpl extends SimpleLogImpl {
    protected DeduplicatingErrorsLogImpl(String name, LogLevel logLevel) {
      super(name, logLevel);
    }

    @Override
    protected void write(String level, Object message, Throwable t) {
      if (ErrorTracker.INSTANCE.shouldPrintStacktrace(t)) {
        super.write(level, message, t);
        ErrorTracker.INSTANCE.rememberStacktracePrinted(t);
      } else {
        message = message + " : " + extractShortErrorMessage(t);
        super.write(level, message);
      }
    }
    
    private String extractShortErrorMessage(Throwable thr) {
      return thr != null ? thr.getClass().getName() + " - " + thr.getMessage() : null;
    }
  }

  private LogFactory() {
  }

  public static Log getLog(Class<?> clazz) {
    if ("reduced".equalsIgnoreCase(logLevel)) {
      return new ReducedSimpleLogImpl(clazz.getName());
    } else {
      LogLevel level;
      if ("standard".equalsIgnoreCase(logLevel)) {
        level = LogLevel.INFO;
      } else {
        try {
          level = LogLevel.valueOf(logLevel.toUpperCase());
        } catch (Throwable thr) {
          thr.printStackTrace();
          // fall-back to INFO level
          level = LogLevel.INFO;
        }
      }
      return new DeduplicatingErrorsLogImpl(clazz.getName(), level);
    }
  }
}
