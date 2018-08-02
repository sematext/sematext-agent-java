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
package com.sematext.spm.client.tracing.agent.test;

import java.lang.reflect.Method;

import com.sematext.spm.client.util.ReflectionUtils;

public final class HttpService {

  private final JDBCService service;

  public HttpService(JDBCService jdbcService) {
    this.service = jdbcService;
  }

  private String executeSQL(String sql) {
    final Method execute = ReflectionUtils.getMethod(service.getClass(), "execute", String.class);
    ReflectionUtils.silentInvoke(execute, service, sql);
    return sql;
  }

  public void service(Request request, Response response) throws Exception {
    if (request.getContextPath().endsWith("/user/") && request.getMethod().equals("GET")) {
      executeSQL("select * from user");
      response.respond("ok", 200);
    } else if (request.getContextPath().endsWith("/user/1") && request.getMethod().equals("GET")) {
      executeSQL("select * from user where id = 1");
      response.respond("ok", 200);
    } else if (request.getContextPath().endsWith("/user/") && request.getMethod().equals("POST")) {
      executeSQL("insert into user(...) values (...)");
      response.respond("ok", 201);
    } else if (request.getContextPath().endsWith("/user/1") && request.getMethod().equals("POST")) {
      executeSQL("update user set ...");
      response.respond("ok", 200);
    } else if (request.getContextPath().equals("/list/")) {
      response.redirect("/user/");
    } else {
      response.respond("error", 400);
    }
  }
}
