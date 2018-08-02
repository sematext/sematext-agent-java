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

import com.sematext.spm.client.tracing.agent.jpa.SpmQueryAccess;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;

@LoggerPointcuts(name = "jpa:entity-manager-create-query", methods = {
    "javax.persistence.Query javax.persistence.EntityManager#createNamedQuery(java.lang.String query)",
    "javax.persistence.TypedQuery javax.persistence.EntityManager#createNamedQuery(java.lang.String query, java.lang.Class resultClass)",
    "javax.persistence.Query javax.persistence.EntityManager#createQuery(java.lang.String query)",
    "javax.persistence.TypedQuery javax.persistence.EntityManager#createQuery(java.lang.String query, java.lang.Class resultClass)",
    "javax.persistence.Query javax.persistence.EntityManager#createNativeQuery(java.lang.String query)",
    "javax.persistence.Query javax.persistence.EntityManager#createNativeQuery(java.lang.String query, java.lang.Class resultClass)",
    "javax.persistence.Query javax.persistence.EntityManager#createNativeQuery(java.lang.String query, java.lang.String resultSetMapping)"
})
public class JpaEntityManagerQueryPointcut implements UnloggableLogger {
  @Override
  public void logBefore(LoggerContext context) {
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
    SpmQueryAccess query = (SpmQueryAccess) returnValue;
    if (query != null) {
      query._$spm_tracing$_query_set((String) context.getMethodParam("query"));

    }
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
  }
}
