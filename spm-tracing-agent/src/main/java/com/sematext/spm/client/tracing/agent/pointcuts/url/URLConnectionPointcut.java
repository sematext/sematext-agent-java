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
package com.sematext.spm.client.tracing.agent.pointcuts.url;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.HttpHeaders;
import com.sematext.spm.client.tracing.agent.model.HttpHeaders.CrossAppCallHeader;
import com.sematext.spm.client.tracing.agent.model.annotation.HTTPRequestAnnotation;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;
import com.sematext.spm.client.util.ReflectionUtils;

@LoggerPointcuts(name = "logger:java-net-url-connection", methods = {
    "java.io.InputStream sun.net.www.protocol.http.HttpURLConnection#getInputStream()",
    "java.io.OutputStream sun.net.www.protocol.http.HttpURLConnection#getOutputStream()",
    "java.io.InputStream sun.net.www.protocol.https.DelegateHttpsURLConnection#getInputStream()",
    "java.io.OutputStream sun.net.www.protocol.https.DelegateHttpsURLConnection#getOutputStream()"
})
public class URLConnectionPointcut implements UnloggableLogger {

  private static final Log LOG = LogFactory.getLog(URLConnectionPointcut.class);
  /**
   * Workaround to avoid stackoverlfow, because of recursive calls.
   * HttpURLConnection designed in way, that post-connection method calls getInputStream, but
   * getInputStream also can be called itself.
   * After getInputStream call need to get cross-app headers, as well as response code, but each of those calls
   * also calls getInputStream as well.
   */
  private static ThreadLocal<Boolean> LOCK = new ThreadLocal<Boolean>() {
    @Override
    protected Boolean initialValue() {
      return false;
    }
  };

  private static boolean isConnected(URLConnection connection) {
    final Boolean connected = (Boolean) ReflectionUtils.silentFieldGet(connection, "connected");
    return connected != null && connected;
  }

  private static boolean isConnecting(URLConnection connection) {
    final Boolean connecting = (Boolean) ReflectionUtils.silentFieldGet(connection, "connecting");
    return connecting != null && connecting;
  }

  private static String getUrl(URLConnection connection) {
    return connection.getURL().toString();
  }

  @Override
  public void logBefore(LoggerContext context) {
    if (LOCK.get()) {
      return;
    }

    final URLConnection connection = (URLConnection) context.getThat();

    Tracing.current().newCall(context.getJoinPoint());
    Tracing.current().setExternal(true);
    /**
     * Don't track 'getOutputStream' as external call on map, because cross-application call headers are accessed
     * after 'getInputStream' call (which should be called after 'getOutputStream' call).
     * In opposite situation in map will be displayed two kind of calls - external
     * and cross-application call what is confusing.
     */
    Tracing.current().setSkipExternalTracingStatistics(context.getJoinPoint().getShortName().equals("getOutputStream"));
    Tracing.current().setTag(Call.CallTag.HTTP_REQUEST);
    Tracing.current().setAnnotation(HTTPRequestAnnotation.request(getUrl(connection)));

    if (!isConnected(connection) && !isConnecting(connection)) {
      connection.setRequestProperty(HttpHeaders.SPM_TRACING_SAMPLED, String.valueOf(Tracing.current().isSampled()));
      connection.setRequestProperty(HttpHeaders.SPM_TRACING_TRACE_ID, String.valueOf(Tracing.current().getTraceId()));
      connection.setRequestProperty(HttpHeaders.SPM_TRACING_CALL_ID, String.valueOf(Tracing.current().getCallId()));
    }
  }

  private void after(LoggerContext context, Throwable throwable) {
    if (LOCK.get()) {
      return;
    }

    if (context.getJoinPoint().getShortName().contains("getInputStream")) {
      final HttpURLConnection connection = (HttpURLConnection) context.getThat();
      try {
        LOCK.set(true);
        try {
          final int responseCode = connection.getResponseCode();
          HTTPRequestAnnotation annotation = (HTTPRequestAnnotation) Tracing.current().getAnnotation();
          if (annotation != null) {
            annotation.setResponseCode(responseCode);
          }
        } catch (IOException e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Can't get response code.", e);
          }
        }

        final String crossAppCallHeader = connection.getHeaderField(HttpHeaders.SPM_TRACING_CROSS_APP_CALL);
        if (crossAppCallHeader != null) {
          final CrossAppCallHeader header = HttpHeaders.decodeCrossAppCallHeader(crossAppCallHeader);
          if (header == null) {
            if (LOG.isTraceEnabled()) {
              LOG.trace("Cross app call header is empty.");
            }
          } else {
            Tracing.current().setCrossAppInHeader(header);
            if (LOG.isDebugEnabled()) {
              LOG.debug("Cross app call header present: " + header + ".");
            }
          }
        }
      } finally {
        LOCK.set(false);
      }
    }
    if (throwable != null) {
      Tracing.current().setFailed(true);
    }
    Tracing.current().endCall();
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
