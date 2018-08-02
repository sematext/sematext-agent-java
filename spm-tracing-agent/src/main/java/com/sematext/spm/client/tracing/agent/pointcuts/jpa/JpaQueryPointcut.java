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
package com.sematext.spm.client.tracing.agent.pointcuts.jpa;

import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.jpa.SpmQueryAccess;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.annotation.JPAAnnotation;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;

@LoggerPointcuts(name = "jpa:query", methods = {
    "java.util.List javax.persistence.Query#getResultList()",
    "int javax.persistence.Query#executeUpdate()",
    "int javax.persistence.Query#getFirstResult()",
    "java.lang.Object javax.persistence.Query#getSingleResult()"
})
public class JpaQueryPointcut implements UnloggableLogger {
  @Override
  public void logBefore(LoggerContext context) {
    Tracing.current().newCall(context.getJoinPoint());
    Tracing.current().setTag(Call.CallTag.JPA);

    final SpmQueryAccess query = (SpmQueryAccess) context.getThat();
    Tracing.current().setAnnotation(JPAAnnotation.query(query._$spm_tracing$_query_get(), null));
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
    Tracing.current().endCall();
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
    Tracing.current().endCall();
  }
}
