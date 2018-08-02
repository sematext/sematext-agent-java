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
package com.sematext.spm.client.tracing.agent.impl;

import org.apache.thrift.TException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.ESAction;
import com.sematext.spm.client.tracing.agent.model.InetAddress;
import com.sematext.spm.client.tracing.agent.model.SolrAnnotation;
import com.sematext.spm.client.tracing.agent.model.annotation.ESAnnotation;
import com.sematext.spm.client.tracing.agent.model.annotation.HTTPRequestAnnotation;
import com.sematext.spm.client.tracing.agent.model.annotation.JPAAnnotation;
import com.sematext.spm.client.tracing.agent.model.annotation.JSPAnnotation;
import com.sematext.spm.client.tracing.agent.model.annotation.SQLAnnotation;
import com.sematext.spm.client.tracing.thrift.*;

public final class ThriftAnnotationSerializer {
  private static interface Ser<T> {
    byte[] serialize(T annotation) throws TException;
  }

  private static class SQLAnnotationSer implements Ser<SQLAnnotation> {
    @Override
    public byte[] serialize(SQLAnnotation annotation) throws TException {
      TSQLAnnotation thrift = new TSQLAnnotation();
      thrift.setType(annotation.getType());
      if (annotation.getSql() != null) {
        thrift.setSql(annotation.getSql());
      } else {
        thrift.setSqlIsSet(false);
      }
      if (annotation.getUrl() != null) {
        thrift.setUrl(annotation.getUrl());
      } else {
        thrift.setUrlIsSet(false);
      }
      thrift.setCount(annotation.getCount());
      thrift.setTable(annotation.getTable());
      if (annotation.getOperation() != null) {
        thrift.setOperation(TSqlStatementOperation.valueOf(annotation.getOperation().name()));
      }
      return ThriftUtils.binaryProtocolSerializer().serialize(thrift);
    }
  }

  private static class JPAAnnotationSer implements Ser<JPAAnnotation> {
    @Override
    public byte[] serialize(JPAAnnotation annotation) throws TException {
      TJPAAnnotation thrift = new TJPAAnnotation();
      thrift.setType(annotation.getType());
      if (annotation.getQuery() != null) {
        thrift.setQuery(annotation.getQuery());
      } else {
        thrift.setQueryIsSet(false);
      }
      if (annotation.getObject() != null) {
        thrift.setObject(annotation.getObject());
      } else {
        thrift.setObjectIsSet(false);
      }
      thrift.setCount(annotation.getCount());
      return ThriftUtils.binaryProtocolSerializer().serialize(thrift);
    }
  }

  private static class JSPAnnotationSer implements Ser<JSPAnnotation> {
    @Override
    public byte[] serialize(JSPAnnotation annotation) throws TException {
      TJSPAnnotation thrift = new TJSPAnnotation();
      if (annotation.getPath() != null) {
        thrift.setPath(annotation.getPath());
      } else {
        thrift.setPathIsSet(false);
      }
      return ThriftUtils.binaryProtocolSerializer().serialize(thrift);
    }
  }

  private static class HTTPRequestAnnotationSer implements Ser<HTTPRequestAnnotation> {
    @Override
    public byte[] serialize(HTTPRequestAnnotation annotation) throws TException {
      THTTPRequestAnnotation thrift = new THTTPRequestAnnotation();
      if (annotation.getUrl() != null) {
        thrift.setUrl(annotation.getUrl());
      } else {
        thrift.setUrlIsSet(false);
      }
      if (annotation.getMethod() != null) {
        thrift.setMethod(annotation.getMethod());
      } else {
        thrift.setMethodIsSet(false);
      }
      thrift.setResponseCode(annotation.getResponseCode());
      return ThriftUtils.binaryProtocolSerializer().serialize(thrift);
    }
  }

  private static class ESAnnotationSer implements Ser<ESAnnotation> {
    @Override
    public byte[] serialize(ESAnnotation annotation) throws TException {
      TESAnnotation thrift = new TESAnnotation();
      final List<TInetAddress> inetAddresses = new ArrayList<TInetAddress>();
      for (InetAddress address : annotation.getAddresses()) {
        inetAddresses.add(new TInetAddress(address.getHost(), address.getPort()));
      }
      thrift.setAddresses(inetAddresses);
      if (annotation.getActions() != null) {
        List<TESAction> thriftActions = new ArrayList<TESAction>();
        for (ESAction action : annotation.getActions()) {
          final TESAction thriftAction = new TESAction();
          thriftAction.setOperationType(TESOperationType.valueOf(action.getOperationType().name()));
          thriftAction.setType(action.getType());
          thriftAction.setIndex(action.getIndex());
          thriftAction.setQuery(action.getQuery());
          thriftAction.setCount(action.getCount());

          thriftActions.add(thriftAction);
        }
        thrift.setActions(thriftActions);
      } else {
        thrift.setActions(Collections.EMPTY_LIST);
      }
      thrift.setIndex(annotation.getIndex());
      thrift.setRequestType(TESRequestType.valueOf(annotation.getRequestType().name()));
      return ThriftUtils.binaryProtocolSerializer().serialize(thrift);
    }
  }

  private static class SolrAnnotationSer implements Ser<SolrAnnotation> {
    @Override
    public byte[] serialize(SolrAnnotation annotation) throws TException {
      final TSolrAnnotation thrift = new TSolrAnnotation();
      thrift.setUrl(annotation.getUrl());
      thrift.setParams(new HashMap<String, String>(annotation.getParams()));
      thrift.setCollection(annotation.getCollection());
      thrift.setResponseStatus((short) annotation.getResponseStatus());
      thrift.setSucceed(annotation.isSucceed());
      thrift.setRequestType(TSolrRequestType.valueOf(annotation.getRequestType().name()));
      return ThriftUtils.binaryProtocolSerializer().serialize(thrift);
    }
  }

  private static enum Serializers {
    SQL(Call.CallTag.SQL_QUERY, new SQLAnnotationSer()),
    JPA(Call.CallTag.JPA, new JPAAnnotationSer()),
    JSP(Call.CallTag.JSP, new JSPAnnotationSer()),
    HTTP_REQUEST(Call.CallTag.HTTP_REQUEST, new HTTPRequestAnnotationSer()),
    ES(Call.CallTag.ES, new ESAnnotationSer()),
    SOLR(Call.CallTag.SOLR, new SolrAnnotationSer());

    private final Call.CallTag tag;
    private final Ser ser;

    Serializers(Call.CallTag tag, Ser ser) {
      this.tag = tag;
      this.ser = ser;
    }

    public static Ser find(Call.CallTag tag) {
      for (Serializers ser : Serializers.values()) {
        if (ser.tag == tag) {
          return ser.ser;
        }
      }
      return null;
    }
  }

  public static byte[] serialize(Call.CallTag tag, Object annotation) throws TException {
    Ser ser = Serializers.find(tag);
    if (ser != null) {
      return ser.serialize(annotation);
    }
    return null;
  }
}
