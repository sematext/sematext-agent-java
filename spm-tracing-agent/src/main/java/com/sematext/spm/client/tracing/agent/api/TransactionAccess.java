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
package com.sematext.spm.client.tracing.agent.api;

import java.util.Collections;
import java.util.Map;

import com.sematext.spm.client.tracing.TracingParameters;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.errors.ErrorsTracker;
import com.sematext.spm.client.tracing.agent.model.FailureType;
import com.sematext.spm.client.util.Preconditions;

public final class TransactionAccess {

  private TransactionAccess() {
  }

  public static void setName(String name) {
    Preconditions.checkNotNull(name);

    Tracing.current().getNamer().redefined(name);
  }

  public static void ignore() {
    Tracing.current().ignore();
  }

  private static void forceTransactionEnd(FailureType type, Throwable throwable, String message) {
    Tracing.current().setFailureType(type);

    if (throwable != null) {
      Tracing.current().setException(throwable);
    }

    if (message != null) {
      Tracing.current().setTransactionParameter(TracingParameters.ERROR_MESSAGE.getKey(), message);
    }

    Tracing.current().forceEnd(true);
    Tracing.endTrace();
  }

  public static void noticeError(Throwable th) {
    Preconditions.checkNotNull(th);

    ErrorsTracker.track(th, null);
    forceTransactionEnd(FailureType.EXCEPTION, th, null);
  }

  public static void noticeError(String message) {
    Preconditions.checkNotNull(message);

    ErrorsTracker.track(null, message);
    forceTransactionEnd(FailureType.CUSTOM, null, message);
  }

  public static void setTransactionParameter(String key, String value) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(value);
    Preconditions.check(key.length() > 0 && key.length() <= ServiceLocator.getConfig().getMaxCustomParameterKeyLength(),
                        "Key length should be > 0 and <= " + ServiceLocator.getConfig()
                            .getMaxCustomParameterKeyLength());
    Preconditions
        .check(value.length() > 0 && value.length() <= ServiceLocator.getConfig().getMaxCustomParameterValueLength(),
               "Value length should be > 0 and <= " + ServiceLocator.getConfig().getMaxCustomParameterValueLength());

    Preconditions.check(Tracing.current().getTransactionParameters().size() < ServiceLocator.getConfig()
                            .getMaxCustomTransactionParametersCount(),
                        "Parameters count should be <= " + ServiceLocator.getConfig()
                            .getMaxCustomTransactionParametersCount());
    Tracing.current().setTransactionParameter(key, value);
  }

  public static Map<String, String> getTransactionParameters() {
    return Collections.unmodifiableMap(Tracing.current().getTransactionParameters());
  }

  public static void setMethodParameter(String key, String value) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(value);
    Preconditions.check(key.length() > 0 && key.length() <= ServiceLocator.getConfig().getMaxCustomParameterKeyLength(),
                        "Key length should be > 0 and <= " + ServiceLocator.getConfig()
                            .getMaxCustomParameterKeyLength());
    Preconditions
        .check(value.length() > 0 && value.length() <= ServiceLocator.getConfig().getMaxCustomParameterValueLength(),
               "Value length should be > 0 and <= " + ServiceLocator.getConfig().getMaxCustomParameterValueLength());

    Preconditions.check(Tracing.current().getTransactionParameters().size() < ServiceLocator.getConfig()
                            .getMaxCustomMethodParametersCount(),
                        "Parameters count should be <= " + ServiceLocator.getConfig()
                            .getMaxCustomMethodParametersCount());
    Tracing.current().setMethodParameter(key, value);
  }

  public static Map<String, String> getMethodParameters() {
    return Collections.unmodifiableMap(Tracing.current().getMethodParameters());
  }
}
