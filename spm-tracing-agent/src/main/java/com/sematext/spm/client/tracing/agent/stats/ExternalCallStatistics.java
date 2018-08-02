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
package com.sematext.spm.client.tracing.agent.stats;

import static com.sematext.spm.client.tracing.agent.model.Call.CallTag.ES;
import static com.sematext.spm.client.tracing.agent.model.Call.CallTag.HTTP_REQUEST;
import static com.sematext.spm.client.tracing.agent.model.Call.CallTag.SOLR;
import static com.sematext.spm.client.tracing.agent.model.Call.CallTag.SQL_QUERY;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.model.SolrAnnotation;
import com.sematext.spm.client.tracing.agent.model.annotation.ESAnnotation;
import com.sematext.spm.client.tracing.agent.model.annotation.HTTPRequestAnnotation;
import com.sematext.spm.client.tracing.agent.model.annotation.SQLAnnotation;
import com.sematext.spm.client.tracing.agent.util.JdbcURL;
import com.sematext.spm.client.util.TTLCache;

public final class ExternalCallStatistics implements StatisticsProcessor {
  private static class ExternalCallImpl implements ExternalCall {
    private final String srcToken;
    private final String srcHostname;
    private final String dstHostname;
    private final String tag;
    private final DiffCounterVar<Long> callsCountVar;
    private final DiffCounterVar<Long> durationVar;

    public ExternalCallImpl(MutableVarProvider varProvider, Id id) {
      this.srcToken = id.getSrcToken();
      this.srcHostname = id.getSrcHostname();
      this.dstHostname = id.getDstHostname();
      this.tag = id.getTag();
      this.callsCountVar = varProvider.newCounter(Long.class);
      this.durationVar = varProvider.newCounter(Long.class);
    }

    @Override
    public String srcToken() {
      return srcToken;
    }

    @Override
    public String srcHostname() {
      return srcHostname;
    }

    @Override
    public String dstHostname() {
      return dstHostname;
    }

    @Override
    public Long callsCount() {
      return callsCountVar.get();
    }

    @Override
    public Long duration() {
      return durationVar.get();
    }

    @Override
    public String tag() {
      return tag;
    }

    public void update(Long duration) {
      this.durationVar.increment(duration);
      this.callsCountVar.increment(1L);
    }
  }

  private static class Id {
    private final String srcToken;
    private final String srcHostname;
    private final String dstHostname;
    private final String tag;

    public Id(String srcToken, String srcHostname, String dstHostname, String tag) {
      this.srcToken = srcToken;
      this.srcHostname = srcHostname;
      this.dstHostname = dstHostname;
      this.tag = tag;
    }

    public String getSrcToken() {
      return srcToken;
    }

    public String getSrcHostname() {
      return srcHostname;
    }

    public String getDstHostname() {
      return dstHostname;
    }

    public String getTag() {
      return tag;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Id id = (Id) o;

      if (dstHostname != null ? !dstHostname.equals(id.dstHostname) : id.dstHostname != null) return false;
      if (srcHostname != null ? !srcHostname.equals(id.srcHostname) : id.srcHostname != null) return false;
      if (srcToken != null ? !srcToken.equals(id.srcToken) : id.srcToken != null) return false;
      if (tag != null ? !tag.equals(id.tag) : id.tag != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = srcToken != null ? srcToken.hashCode() : 0;
      result = 31 * result + (srcHostname != null ? srcHostname.hashCode() : 0);
      result = 31 * result + (dstHostname != null ? dstHostname.hashCode() : 0);
      result = 31 * result + (tag != null ? tag.hashCode() : 0);
      return result;
    }
  }

  private final MutableVarProvider varProvider;
  private final TTLCache<Id, ExternalCall> calls = new TTLCache<Id, ExternalCall>(TimeUnit.MINUTES.toMillis(2));

  public ExternalCallStatistics(MutableVarProvider varProvider) {
    this.varProvider = varProvider;
  }

  public boolean update(PartialTransaction transaction, Call call) {
    if (call.isSkipExternalTracingStatistics()) {
      return false;
    }

    final String dstHostname = getDstHostname(call);
    if (dstHostname == null) {
      return false;
    }
    String tag = null;
    if (call.getCallTag() != null) {
      tag = call.getCallTag().name();
    }
    final Id id = new Id(transaction.getToken(), transaction.getEndpoint().getHostname(), dstHostname, tag);
    ExternalCallImpl externalCall = (ExternalCallImpl) calls.get(id);
    if (externalCall == null) {
      externalCall = new ExternalCallImpl(varProvider, id);
      ExternalCallImpl existing = (ExternalCallImpl) calls.putIfAbsent(id, externalCall);
      if (existing != null) {
        externalCall = existing;
      }
    }
    externalCall.update(call.getDuration());
    return true;
  }

  public Collection<ExternalCall> getExternalCalls() {
    return calls.values();
  }

  private String getDstHostname(Call call) {
    if (call.getCrossAppCallId() != null) {
      return null;
    }

    if (HTTP_REQUEST == call.getCallTag()) {
      HTTPRequestAnnotation annotation = (HTTPRequestAnnotation) call.getAnnotation();
      if (annotation == null) {
        return null;
      }
      try {
        return new URL(annotation.getUrl()).getHost();
      } catch (Exception e) {
        return null;
      }
    } else if (ES == call.getCallTag()) {
      ESAnnotation annotation = (ESAnnotation) call.getAnnotation();
      if (annotation == null) {
        return null;
      }
      if (annotation.getAddresses().isEmpty()) {
        return null;
      }
      return annotation.getAddresses().get(0).getHost();
    } else if (SQL_QUERY == call.getCallTag()) {
      SQLAnnotation annotation = (SQLAnnotation) call.getAnnotation();
      if (annotation == null) {
        return null;
      }
      if (annotation.getUrl() == null) {
        return null;
      }
      return JdbcURL.getHostname(annotation.getUrl());
    } else if (SOLR == call.getCallTag()) {
      SolrAnnotation annotation = (SolrAnnotation) call.getAnnotation();
      if (annotation == null) {
        return null;
      }
      if (annotation.getUrl() == null) {
        return null;
      }
      try {
        return new URL(annotation.getUrl()).getHost();
      } catch (MalformedURLException e) {
        return null;
      }
    }
    return null;
  }

  @Override
  public void process(PartialTransaction transaction) {
    for (Call call : transaction.getCalls()) {
      if (call.isSkipExternalTracingStatistics()) {
        continue;
      }

      final String dstHostname = getDstHostname(call);
      if (dstHostname == null) {
        continue;
      }
      String tag = null;
      if (call.getCallTag() != null) {
        tag = call.getCallTag().name();
      }
      final Id id = new Id(transaction.getToken(), transaction.getEndpoint().getHostname(), dstHostname, tag);
      ExternalCallImpl externalCall = (ExternalCallImpl) calls.get(id);
      if (externalCall == null) {
        externalCall = new ExternalCallImpl(varProvider, id);
        ExternalCallImpl existing = (ExternalCallImpl) calls.putIfAbsent(id, externalCall);
        if (existing != null) {
          externalCall = existing;
        }
      }
      externalCall.update(call.getDuration());
    }
  }
}
