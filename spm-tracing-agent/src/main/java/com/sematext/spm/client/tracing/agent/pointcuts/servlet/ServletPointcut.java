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
package com.sematext.spm.client.tracing.agent.pointcuts.servlet;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.tracing.TracingParameters;
import com.sematext.spm.client.tracing.agent.CrossAppOutInfo;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.api.TransactionAccess;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.FailureType;
import com.sematext.spm.client.tracing.agent.model.Http;
import com.sematext.spm.client.tracing.agent.model.HttpHeaders;
import com.sematext.spm.client.tracing.agent.model.WebTransactionSummary;
import com.sematext.spm.client.tracing.agent.servlet.ServletResponseHeaders;
import com.sematext.spm.client.tracing.agent.servlet.SpmGenericServletAccess;
import com.sematext.spm.client.tracing.agent.servlet.SpmHttpServletRequestAccess;
import com.sematext.spm.client.tracing.agent.servlet.SpmHttpServletResponseAccess;
import com.sematext.spm.client.tracing.agent.servlet.SpmServletConfigAccess;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.MethodPointcut;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;
import com.sematext.spm.client.util.PrimitiveParsers;

@LoggerPointcuts(name = "servlet:logger", methods = {
    "void javax.servlet.Filter#doFilter(javax.servlet.ServletRequest req, javax.servlet.ServletResponse resp, javax.servlet.FilterChain chain)",
    "void javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse resp)"
})
public class ServletPointcut implements UnloggableLogger {

  private static final String TRANSACTION_NAME_INIT_PARAM_NAME = "com.sematext.spm.client.tracing.agent.TRANSACTION_NAME";

  private final Log log = LogFactory.getLog(ServletPointcut.class);

  @Override
  public void logBefore(LoggerContext context) {
    final SpmHttpServletRequestAccess request = (SpmHttpServletRequestAccess) context.getAllParams()[0];
    final SpmHttpServletResponseAccess response = (SpmHttpServletResponseAccess) context.getAllParams()[1];

    if (request != null) {
      //Don't create new trace if call stack is not empty. For example if JSP servlet is called from dispatcher
      //it shouldn't create new trace
      boolean callStackIsEmpty = Tracing.current().callStackEmpty();
      boolean crossAppCall = false;

      if (callStackIsEmpty) {
        final String traceIdHeader = request._$spm_tracing$_getHeader(HttpHeaders.SPM_TRACING_TRACE_ID);
        final String callIdHeader = request._$spm_tracing$_getHeader(HttpHeaders.SPM_TRACING_CALL_ID);
        final String sampledHeader = request._$spm_tracing$_getHeader(HttpHeaders.SPM_TRACING_SAMPLED);

        final Long traceId = PrimitiveParsers.parseLongOrNull(traceIdHeader);
        final Long callId = PrimitiveParsers.parseLongOrNull(callIdHeader);
        final boolean sampled = Boolean.parseBoolean(sampledHeader);

        if (traceId != null && callId != null) {
          Tracing.newTrace(request._$spm_tracing$_getRequestURI(), Call.TransactionType.WEB, traceId, callId, sampled);

          crossAppCall = true;
        } else {
          Tracing.newTrace(request._$spm_tracing$_getRequestURI(), Call.TransactionType.WEB);
          if (log.isDebugEnabled()) {
            log.debug("Created trace for " + request._$spm_tracing$_getRequestURI());
          }
        }
      } else {
        if (log.isDebugEnabled()) {
          log.debug("Skipping trace for " + request._$spm_tracing$_getRequestURI());
        }
      }

      Tracing.current().newCall(context.getJoinPoint());
      Tracing.current().setTag(Call.CallTag.REGULAR);

      if (((MethodPointcut) context.getPointcut()).getTypeName().equals("javax.servlet.http.HttpServlet")) {
        SpmGenericServletAccess servletAccess = (SpmGenericServletAccess) context.getThat();

        try {
          SpmServletConfigAccess servletConfig = servletAccess._$spm_tracing$_getServletConfig();
          String transactionName = servletConfig._$spm_tracing$_getInitParameter(TRANSACTION_NAME_INIT_PARAM_NAME);
          Tracing.current().getNamer().redefined(transactionName);
        } catch (Exception e) {
          //can happen if servlet context wasn't initialized
        }
      }

      Tracing.current().getNamer().asServlet(context.getJoinPoint());

      if (callStackIsEmpty) {
        Tracing.current().setResponseHeaders(new ServletResponseHeaders(response));
        Tracing.current().setEntryPoint(true);

        Tracing.current().setTransactionSummary(WebTransactionSummary.handle(
            request._$spm_tracing$_getRequestURI(),
            request._$spm_tracing$_getQueryString(),
            request._$spm_tracing$_getMethod()
        ));
      }

      if (crossAppCall) {
        final CrossAppOutInfo crossAppOutInfo = CrossAppOutInfo.create(Tracing.current());
        Tracing.current().setCrossAppOutInfo(crossAppOutInfo);
      }
    }
  }

  private void after(LoggerContext context, Throwable throwable) {
    SpmHttpServletRequestAccess request = null;
    int status = -1;
    try {
      request = (SpmHttpServletRequestAccess) context.getAllParams()[0];
      final SpmHttpServletResponseAccess response = (SpmHttpServletResponseAccess) context.getAllParams()[1];
      if (response != null) {
        WebTransactionSummary summary = (WebTransactionSummary) Tracing.current().getTransactionSummary();
        if (summary != null) {
          status = response._$spm_tracing$_getStatus();
          summary.setResponseCode(status);

          if (Http.is4xx(status) || Http.is5xx(status)) {
            Tracing.current().setFailed(true);
            Tracing.current().setFailureType(FailureType.HTTP_RESPONSE);
          }
        }

        Tracing.current().sendCrossAppOutHeaders();

        if (Tracing.current().isLastCall()) {
          //reset status after response is handled, some application servers using responses pool instead of creating new object each time
          //they are reset internally, but variable added by SpmHttpServletResponseAccess mixin don't reset
          response._$spm_tracing$_setStatus(0);
        }
      }
    } finally {
      if (throwable != null) {
        Tracing.current()
            .setTransactionParameter(TracingParameters.ERROR_CLASS.getKey(), throwable.getClass().getName());
        if (request != null) {
          Tracing.current()
              .setTransactionParameter(TracingParameters.REQUEST.getKey(), request._$spm_tracing$_getRequestURI());
        }
        if (status != -1) {
          Tracing.current().setTransactionParameter(TracingParameters.STATUS_CODE.getKey(), String.valueOf(status));
        }
        if (Tracing.current().getCurrentCall() != null) {
          long duration = System.currentTimeMillis() - Tracing.current().getCurrentCall().getStartTimestamp();
          Tracing.current().setTransactionParameter(TracingParameters.DURATION.getKey(), String.valueOf(duration));
        }

        Tracing.current().setFailed(true);
        Tracing.current().setFailureType(FailureType.EXCEPTION);
        Tracing.current().setException(throwable);

        TransactionAccess.noticeError(throwable);
      }
      Tracing.current().endCall();
      if (Tracing.current().callStackEmpty()) {
        Tracing.endTrace();
      }
    }
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
    after(context, null);
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
    after(context, throwable);
  }
}
