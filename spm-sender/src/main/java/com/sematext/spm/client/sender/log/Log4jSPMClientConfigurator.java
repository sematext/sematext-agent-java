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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;

import java.util.Enumeration;

public final class Log4jSPMClientConfigurator {
  private Log4jSPMClientConfigurator() {
  }

  public static void configure() {
    Enumeration loggers = LogManager.getCurrentLoggers();
    LogManager.getRootLogger().removeAllAppenders();
    while (loggers.hasMoreElements()) {
      final Logger logger = (Logger) loggers.nextElement();
      logger.removeAllAppenders();
    }

    LogManager.getRootLogger().addAppender(new SPMMonitorLogAppender("SPMMonitorLogAppender", null, null, true));
  }
}
