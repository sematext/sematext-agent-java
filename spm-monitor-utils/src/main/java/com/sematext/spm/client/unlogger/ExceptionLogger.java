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
package com.sematext.spm.client.unlogger;

import java.util.Map;

import com.sematext.spm.client.unlogger.errors.ExceptionHandler;

/**
 * Catches the raising of predefined exceptions.
 * Simple store exception in context of call chain,
 * the output to log perfromed in "frontal" pointcut.
 */
public abstract class ExceptionLogger extends DefaultLogger {

  protected ExceptionLogger(Map<String, Object> params) {
    super(params);
  }

  @Override
  public void logBefore(LoggerContext context) {
    getExceptionHandler(context).store((Throwable) context.getThat());
  }

  private static final String EXCEPTION_HANDLER_KEY = "EXCEPTION_HANDLER";

  public static ExceptionHandler getExceptionHandler(LoggerContext context) {
    ExceptionHandler exceptionHandler = context.getSequenceLevelParam(EXCEPTION_HANDLER_KEY);
    if (exceptionHandler == null) {
      exceptionHandler = new ExceptionHandler();
      context.setSequenceLevelParam(EXCEPTION_HANDLER_KEY, exceptionHandler);
    }
    return exceptionHandler;
  }

  public static void clearExceptionHandler(LoggerContext context) {
    context.setSequenceLevelParam(EXCEPTION_HANDLER_KEY, null);
  }

}
