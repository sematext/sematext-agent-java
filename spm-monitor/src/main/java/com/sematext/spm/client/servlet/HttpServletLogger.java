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
package com.sematext.spm.client.servlet;

import java.util.Map;

import com.sematext.spm.client.unlogger.DefaultTraceLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;

@LoggerPointcuts(name = "core:servlets", methods = {
    "void javax.servlet.Servlet#service(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response)",
    "void javax.servlet.Servlet#init(javax.servlet.ServletConfig config)",
    "void javax.servlet.Filter#doFilter(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response, javax.servlet.FilterChain chain)",
    "void javax.servlet.Filter#init(javax.servlet.FilterConfig filterConfig)" })
public class HttpServletLogger extends DefaultTraceLogger {

  public HttpServletLogger(Map<String, Object> params) {
    super(params);
  }
}
