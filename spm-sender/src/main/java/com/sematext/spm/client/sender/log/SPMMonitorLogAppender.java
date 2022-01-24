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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;

import java.io.Serializable;

@Plugin(name="SPMMonitorLogAppender", category="Core", elementType="appender", printObject=true)
public class SPMMonitorLogAppender extends AbstractAppender {

  private static final Log LOG = LogFactory.getLog(SPMMonitorLogAppender.class);

  protected SPMMonitorLogAppender(String name, Filter filter,
                                  Layout<? extends Serializable> layout, final boolean ignoreExceptions) {
    super(name, filter, layout, ignoreExceptions);
  }

  @Override
  protected void append(LogEvent evt) {
    if (evt.getLevel() == Level.TRACE) {
      if (evt.getThrown() == null) {
        LOG.trace(evt.getMessage());
      } else {
        LOG.trace(evt.getMessage(), evt.getThrown());
      }
    } else if (evt.getLevel() == Level.DEBUG) {
      if (evt.getThrown() == null) {
        LOG.debug(evt.getMessage());
      } else {
        LOG.debug(evt.getMessage(), evt.getThrown());
      }
    } else if (evt.getLevel() == Level.INFO) {
      if (evt.getThrown() == null) {
        LOG.info(evt.getMessage());
      } else {
        LOG.info(evt.getMessage(), evt.getThrown());
      }
    } else if (evt.getLevel() == Level.WARN) {
      if (evt.getThrown() == null) {
        LOG.warn(evt.getMessage());
      } else {
        LOG.warn(evt.getMessage(), evt.getThrown());
      }
    } else if (evt.getLevel() == Level.ERROR) {
      if (evt.getThrown() == null) {
        LOG.error(evt.getMessage());
      } else {
        LOG.error(evt.getMessage(), evt.getThrown());
      }
    } else if (evt.getLevel() == Level.FATAL) {
      if (evt.getThrown() == null) {
        LOG.fatal(evt.getMessage());
      } else {
        LOG.fatal(evt.getMessage(), evt.getThrown());
      }
    }
  }
}
