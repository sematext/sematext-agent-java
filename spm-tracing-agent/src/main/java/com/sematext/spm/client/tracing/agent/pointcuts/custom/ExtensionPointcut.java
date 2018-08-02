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
package com.sematext.spm.client.tracing.agent.pointcuts.custom;

import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.FailureType;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.xml.CustomPointcutOptions;

public class ExtensionPointcut implements UnloggableLogger {

  private final CustomPointcutOptions options;

  public ExtensionPointcut(CustomPointcutOptions options) {
    this.options = options;
  }

  @Override
  public void logBefore(LoggerContext context) {
    if (options.isEntryPoint()) {
      Tracing.newTrace(context.getJoinPoint().getShortName(), Call.TransactionType.BACKGROUND);
    }
    Tracing.current().getNamer().asFramework(context.getJoinPoint());
    Tracing.current().getNamer().redefined(options.getTransactionName());

    Tracing.current().newCall(context.getJoinPoint());
    Tracing.current().setTag(Call.CallTag.REGULAR);
    Tracing.current().setEntryPoint(options.isEntryPoint());
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
    Tracing.current().endCall();
    if (options.isEntryPoint()) {
      Tracing.endTrace();
    }
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
    Tracing.current().setFailed(true);
    if (options.isEntryPoint()) {
      Tracing.current().setFailureType(FailureType.EXCEPTION);
      Tracing.current().setException(throwable);
    }
    Tracing.current().endCall();
    if (options.isEntryPoint()) {
      Tracing.endTrace();
    }
  }
}
