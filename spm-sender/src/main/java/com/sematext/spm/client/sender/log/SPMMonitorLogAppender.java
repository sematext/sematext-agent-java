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

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

public class SPMMonitorLogAppender extends AppenderSkeleton {

  private static final Log LOG = LogFactory.getLog(SPMMonitorLogAppender.class);

  @Override
  protected void append(LoggingEvent evt) {
    if (evt.getLevel() == Level.TRACE) {
      if (evt.getThrowableInformation() == null) {
        LOG.trace(evt.getMessage());
      } else {
        LOG.trace(evt.getMessage(), evt.getThrowableInformation().getThrowable());
      }
    } else if (evt.getLevel() == Level.DEBUG) {
      if (evt.getThrowableInformation() == null) {
        LOG.debug(evt.getMessage());
      } else {
        LOG.debug(evt.getMessage(), evt.getThrowableInformation().getThrowable());
      }
    } else if (evt.getLevel() == Level.INFO) {
      if (evt.getThrowableInformation() == null) {
        LOG.info(evt.getMessage());
      } else {
        LOG.info(evt.getMessage(), evt.getThrowableInformation().getThrowable());
      }
    } else if (evt.getLevel() == Level.WARN) {
      if (evt.getThrowableInformation() == null) {
        LOG.warn(evt.getMessage());
      } else {
        LOG.warn(evt.getMessage(), evt.getThrowableInformation().getThrowable());
      }
    } else if (evt.getLevel() == Level.ERROR) {
      if (evt.getThrowableInformation() == null) {
        LOG.error(evt.getMessage());
      } else {
        LOG.error(evt.getMessage(), evt.getThrowableInformation().getThrowable());
      }
    } else if (evt.getLevel() == Level.FATAL) {
      if (evt.getThrowableInformation() == null) {
        LOG.fatal(evt.getMessage());
      } else {
        LOG.fatal(evt.getMessage(), evt.getThrowableInformation().getThrowable());
      }
    }
  }

  @Override
  public void close() {

  }

  @Override
  public boolean requiresLayout() {
    return false;
  }
}
