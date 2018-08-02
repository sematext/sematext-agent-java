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
package com.sematext.spm.client.tracing.agent.pointcuts.jetty9;

import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;

/**
 * Tested on jetty 9.2.2
 */
@LoggerPointcuts(name = "jetty:http-connection-pointcut", methods = {
    "void org.eclipse.jetty.server.HttpConnection#send(org.eclipse.jetty.http.HttpGenerator$ResponseInfo info, java.nio.ByteBuffer content, boolean lastContent, org.eclipse.jetty.util.Callback callback)",
    "void org.eclipse.jetty.server.HttpConnection#send(java.nio.ByteBuffer content, boolean lastContent, org.eclipse.jetty.util.Callback callback)"
})
public class HttpConnectionPointcut implements UnloggableLogger {
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
