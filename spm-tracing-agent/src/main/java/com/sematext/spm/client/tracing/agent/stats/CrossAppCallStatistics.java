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

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.util.TTLCache;

public final class CrossAppCallStatistics implements StatisticsProcessor {
  private static class CrossAppCallImpl implements CrossAppCall {
    private final String srcToken;
    private final String dstToken;
    private final String srcHostname;
    private final String dstHostname;
    private final DiffCounterVar<Long> srcDurationVar;
    private final DiffCounterVar<Long> dstDurationVar;
    private final DiffCounterVar<Long> callsCountVar;
    private final String tag;
    private final String request;

    public CrossAppCallImpl(MutableVarProvider provider, Id id) {
      this.srcToken = id.getSrcToken();
      this.dstToken = id.getDstToken();
      this.srcHostname = id.getSrcHostname();
      this.dstHostname = id.getDstHostname();
      this.tag = id.getTag();
      this.request = id.getRequest();
      this.srcDurationVar = provider.newCounter(Long.class);
      this.dstDurationVar = provider.newCounter(Long.class);
      this.callsCountVar = provider.newCounter(Long.class);
    }

    @Override
    public String srcToken() {
      return srcToken;
    }

    @Override
    public String dstToken() {
      return dstToken;
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
    public Long srcDuration() {
      return srcDurationVar.get();
    }

    @Override
    public Long dstDuration() {
      return dstDurationVar.get();
    }

    @Override
    public Long callsCount() {
      return callsCountVar.get();
    }

    @Override
    public String request() {
      return request;
    }

    @Override
    public String tag() {
      return tag;
    }

    public void update(Long srcDuration, Long dstDuration) {
      this.srcDurationVar.increment(srcDuration);
      this.dstDurationVar.increment(dstDuration);
      this.callsCountVar.increment(1L);
    }
  }

  private static class Id {
    private final String srcToken;
    private final String dstToken;
    private final String srcHostname;
    private final String dstHostname;
    private final String tag;
    private final String request;

    public Id(String request, String tag, String dstHostname, String srcHostname, String dstToken, String srcToken) {
      this.request = request;
      this.tag = tag;
      this.dstHostname = dstHostname;
      this.srcHostname = srcHostname;
      this.dstToken = dstToken;
      this.srcToken = srcToken;
    }

    public String getSrcToken() {
      return srcToken;
    }

    public String getDstToken() {
      return dstToken;
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

    public String getRequest() {
      return request;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Id id = (Id) o;

      if (dstHostname != null ? !dstHostname.equals(id.dstHostname) : id.dstHostname != null) return false;
      if (dstToken != null ? !dstToken.equals(id.dstToken) : id.dstToken != null) return false;
      if (request != null ? !request.equals(id.request) : id.request != null) return false;
      if (srcHostname != null ? !srcHostname.equals(id.srcHostname) : id.srcHostname != null) return false;
      if (srcToken != null ? !srcToken.equals(id.srcToken) : id.srcToken != null) return false;
      if (tag != null ? !tag.equals(id.tag) : id.tag != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = srcToken != null ? srcToken.hashCode() : 0;
      result = 31 * result + (dstToken != null ? dstToken.hashCode() : 0);
      result = 31 * result + (srcHostname != null ? srcHostname.hashCode() : 0);
      result = 31 * result + (dstHostname != null ? dstHostname.hashCode() : 0);
      result = 31 * result + (tag != null ? tag.hashCode() : 0);
      result = 31 * result + (request != null ? request.hashCode() : 0);
      return result;
    }
  }

  private final MutableVarProvider mutableVarProvider;
  private final TTLCache<Id, CrossAppCall> calls = new TTLCache<Id, CrossAppCall>(TimeUnit.MINUTES.toMillis(2));

  public CrossAppCallStatistics(MutableVarProvider mutableVarProvider) {
    this.mutableVarProvider = mutableVarProvider;
  }

  public boolean update(PartialTransaction transaction, Call call) {
    if (call.getCrossAppCallId() == null || call.getCrossAppEndpoint() == null) {
      return false;
    }

    String tag = null;
    if (call.getCallTag() != null) {
      tag = call.getCallTag().name();
    }

    final Id id = new Id(call.getCrossAppRequest(), tag, call.getCrossAppEndpoint().getHostname(), transaction
        .getEndpoint().getHostname(),
                         call.getCrossAppToken(), transaction.getToken());

    CrossAppCallImpl crossAppCall = (CrossAppCallImpl) calls.get(id);
    if (crossAppCall == null) {
      crossAppCall = new CrossAppCallImpl(mutableVarProvider, id);

      CrossAppCallImpl existing = (CrossAppCallImpl) calls.putIfAbsent(id, crossAppCall);
      if (existing != null) {
        crossAppCall = existing;
      }
    }

    crossAppCall.update(call.getDuration(), call.getCrossAppDuration());
    return true;
  }

  @Override
  public void process(PartialTransaction transaction) {
    for (Call call : transaction.getCalls()) {
      if (call.getCrossAppCallId() == null || call.getCrossAppEndpoint() == null) {
        continue;
      }

      String tag = null;
      if (call.getCallTag() != null) {
        tag = call.getCallTag().name();
      }

      final Id id = new Id(call.getCrossAppRequest(), tag, call.getCrossAppEndpoint().getHostname(), transaction
          .getEndpoint().getHostname(),
                           call.getCrossAppToken(), transaction.getToken());

      CrossAppCallImpl crossAppCall = (CrossAppCallImpl) calls.get(id);
      if (crossAppCall == null) {
        crossAppCall = new CrossAppCallImpl(mutableVarProvider, id);

        CrossAppCallImpl existing = (CrossAppCallImpl) calls.putIfAbsent(id, crossAppCall);
        if (existing != null) {
          crossAppCall = existing;
        }
      }

      crossAppCall.update(call.getDuration(), call.getCrossAppDuration());
    }
  }

  public Collection<CrossAppCall> getCrossAppCalls() {
    return calls.values();
  }
}
