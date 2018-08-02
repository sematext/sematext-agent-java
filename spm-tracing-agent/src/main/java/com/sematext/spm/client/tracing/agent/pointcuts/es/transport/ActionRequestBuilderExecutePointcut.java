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

import static com.sematext.spm.client.util.StringUtils.join;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.sematext.spm.client.tracing.agent.Trace;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.es.transport.SpmActionRequestBuilderAccess;
import com.sematext.spm.client.tracing.agent.es.transport.SpmBulkRequestAccess;
import com.sematext.spm.client.tracing.agent.es.transport.SpmDeleteRequestAccess;
import com.sematext.spm.client.tracing.agent.es.transport.SpmGetRequestAccess;
import com.sematext.spm.client.tracing.agent.es.transport.SpmIndexRequestAccess;
import com.sematext.spm.client.tracing.agent.es.transport.SpmSearchRequestAccess;
import com.sematext.spm.client.tracing.agent.es.transport.SpmUpdateRequestAccess;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.ESAction;
import com.sematext.spm.client.tracing.agent.model.ESAction.OperationType;
import com.sematext.spm.client.tracing.agent.model.InetAddress;
import com.sematext.spm.client.tracing.agent.model.annotation.ESAnnotation;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;
import com.sematext.spm.client.util.ReflectionUtils;

@LoggerPointcuts(name = "es-transport:action-request-builder-execute-pointcut", methods = {
    "org.elasticsearch.action.ListenableActionFuture org.elasticsearch.action.ActionRequestBuilder#execute()"
})
public class ActionRequestBuilderExecutePointcut implements UnloggableLogger {

  @SuppressWarnings("unchecked")
  private static List<InetAddress> getAddresses(Object builder) {
    final List<InetAddress> inetAddresses = new ArrayList<InetAddress>();

    Object client = ((SpmActionRequestBuilderAccess) builder)._$spm_tracing$_getClient();
    if (client == null) {
      return inetAddresses;
    }
    if (client.getClass().getCanonicalName().equals("org.elasticsearch.client.transport.TransportClient")) {
      Method transportAddresses = ReflectionUtils.getMethod(client.getClass(), "transportAddresses");
      List<Object> addresses = (List<Object>) ReflectionUtils.silentInvoke(transportAddresses, client);
      if (addresses == null || addresses.isEmpty()) {
        return inetAddresses;
      }

      for (final Object address : addresses) {
        if (address.getClass().getCanonicalName()
            .equals("org.elasticsearch.common.transport.InetSocketTransportAddress")) {
          Method getAddress = ReflectionUtils.getMethod(address.getClass(), "address");
          InetSocketAddress inetAddr = (InetSocketAddress) ReflectionUtils.silentInvoke(getAddress, address);
          inetAddresses.add(new InetAddress(inetAddr.getHostName(), inetAddr.getPort()));
        }
      }
    }
    return inetAddresses;
  }

  private static boolean isBulk(Object request) {
    return request.getClass().getCanonicalName().equals("org.elasticsearch.action.bulk.BulkRequestBuilder");
  }

  private static void buildESActions(Object request, List<ESAction> actions) {
    final String requestType = request.getClass().getCanonicalName();

    if (requestType.equals("org.elasticsearch.action.index.IndexRequest")) {
      final SpmIndexRequestAccess indexRequest = (SpmIndexRequestAccess) request;
      actions.add(new ESAction(OperationType.INDEX, indexRequest._$spm_tracing$_getIndex(), indexRequest
          ._$spm_tracing$_getType(), null));
    } else if (requestType.equals("org.elasticsearch.action.delete.DeleteRequest")) {
      final SpmDeleteRequestAccess deleteRequest = (SpmDeleteRequestAccess) request;
      actions.add(new ESAction(OperationType.DELETE, deleteRequest._$spm_tracing$_getIndex(), deleteRequest
          ._$spm_tracing$_getType(), null));
    } else if (requestType.equals("org.elasticsearch.action.search.SearchRequest")) {
      final SpmSearchRequestAccess searchRequest = (SpmSearchRequestAccess) request;
      String query = null;
      if (searchRequest._$spm_tracing$_getSearchRequestBuilder() != null) {
        query = String.valueOf(searchRequest._$spm_tracing$_getSearchRequestBuilder());
      }
      actions.add(new ESAction(OperationType.SEARCH, join(searchRequest._$spm_tracing$_getIndices(), ","),
                               join(searchRequest._$spm_tracing$_getTypes(), ","), query));
    } else if (requestType.equals("org.elasticsearch.action.get.GetRequest")) {
      final SpmGetRequestAccess getRequest = (SpmGetRequestAccess) request;
      actions.add(new ESAction(OperationType.GET, getRequest._$spm_tracing$_getIndex(), getRequest
          ._$spm_tracing$_getType(), null));
    } else if (requestType.equals("org.elasticsearch.action.update.UpdateRequest")) {
      final SpmUpdateRequestAccess updateRequest = (SpmUpdateRequestAccess) request;
      actions.add(new ESAction(OperationType.UPDATE, updateRequest._$spm_tracing$_getIndex(), updateRequest
          ._$spm_tracing$_getType(), null));
    } else if (requestType.equals("org.elasticsearch.action.bulk.BulkRequest")) {
      final SpmBulkRequestAccess bulkRequest = (SpmBulkRequestAccess) request;
      for (Object actionRequest : bulkRequest._$spm_tracing$_getRequests()) {
        buildESActions(actionRequest, actions);
      }
    }
  }

  public void logBefore(LoggerContext context) {
    Trace trace = Tracing.current();

    trace.newCall(context.getJoinPoint());
    trace.setTag(Call.CallTag.ES);

    final SpmActionRequestBuilderAccess builder = (SpmActionRequestBuilderAccess) context.getThat();
    final List<ESAction> actions = new ArrayList<ESAction>();
    buildESActions(builder._$spm_tracing$_getRequest(), actions);

    try {
      trace.setAnnotation(ESAnnotation.make(getAddresses(context.getThat()), isBulk(context.getThat()), actions));
    } catch (IllegalArgumentException e) {
      trace.setTag(Call.CallTag.REGULAR);
    }
  }

  @Override
  public void logAfter(LoggerContext context, Object returnValue) {
    Tracing.current().endCall();
  }

  @Override
  public void logThrow(LoggerContext context, Throwable throwable) {
    Tracing.current().setFailed(true);
    Tracing.current().endCall();
  }
}
