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
package com.sematext.spm.client.tracing.agent.tomcat;

import java.lang.reflect.Method;

import com.sematext.spm.client.tracing.agent.ResponseHeaders;
import com.sematext.spm.client.util.ReflectionUtils;

public class TomcatResponseHeaders implements ResponseHeaders {
  private final Object response;

  public TomcatResponseHeaders(Object response) {
    this.response = response;
  }

  @Override
  public void addHeader(String name, String value) {
    final Method method = ReflectionUtils.getMethod(response.getClass(), "setHeader", String.class, String.class);
    ReflectionUtils.silentInvoke(method, response, name, value);
  }
}
