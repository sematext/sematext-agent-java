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
package com.sematext.spm.client.tracing.agent.pointcuts.es.transport;

import com.sematext.spm.client.tracing.agent.es.transport.SpmActionRequestBuilderAccess;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;

@LoggerPointcuts(name = "es-transport:action-request-builder-ctor-pointcut", constructors = {
    "org.elasticsearch.action.ActionRequestBuilder(org.elasticsearch.client.ElasticsearchClient client, org.elasticsearch.action.ActionRequest request)"
})
public class ActionRequestBuilderCtorPointcut implements UnloggableLogger {
  @Override
  public void logBefore(LoggerContext context) {
    ((SpmActionRequestBuilderAccess) context.getThat())._$spm_tracing$_setClient(context.getAllParams()[0]);
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {

  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {

  }
}
