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
package com.sematext.spm.client.tracing.agent.pointcuts.jetty8;

import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;

/**
 * Tested on 8.1.16, 7.6.16
 */
@LoggerPointcuts(name = "jetty-8:abstract-http-connection", methods = {
    "void org.eclipse.jetty.server.AbstractHttpConnection#commitResponse(boolean last)"
})
public class AbstractHttpConnectionPointcut implements UnloggableLogger {
  @Override
  public void logBefore(LoggerContext context) {
    Tracing.current().sendCrossAppOutHeaders();
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
  }
}
