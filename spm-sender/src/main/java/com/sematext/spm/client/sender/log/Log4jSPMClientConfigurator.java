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
package com.sematext.spm.client.sender.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.Map;

public final class Log4jSPMClientConfigurator {
  private Log4jSPMClientConfigurator() {
  }

  public static void configure() {
    final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    final Configuration config = ctx.getConfiguration();
    Map<String, LoggerConfig> loggers = config.getLoggers();
    for (String name : loggers.keySet()){
      LoggerConfig loggerConfig = loggers.get(name);
      for (String appender : loggerConfig.getAppenders().keySet()) {
        loggerConfig.removeAppender(appender);
      }
    }
    ctx.updateLoggers();
    ctx.getRootLogger().addAppender(new SPMMonitorLogAppender("SPMMonitorLogAppender", null, null, true));
    ctx.updateLoggers();
  }
}
