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

import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.WebTransactionSummary;
import com.sematext.spm.client.tracing.thrift.TWebTransactionSummary;

public final class ThriftTransactionSummarySerializer {
  private ThriftTransactionSummarySerializer() {
  }

  private static interface Ser<T> {
    byte[] serialize(T summary) throws TException;
  }

  private static class WebTransactionSummarySer implements Ser<WebTransactionSummary> {
    @Override
    public byte[] serialize(WebTransactionSummary summary) throws TException {
      TWebTransactionSummary thrift = new TWebTransactionSummary();
      thrift.setRequest(summary.getRequest());
      thrift.setQueryString(summary.getQueryString());
      thrift.setRequestMethod(summary.getRequestMethod());
      thrift.setResponseCode(summary.getResponseCode());
      return ThriftUtils.binaryProtocolSerializer().serialize(thrift);
    }
  }

  private static enum Serializers {
    WEB(Call.TransactionType.WEB, new WebTransactionSummarySer());

    private final Call.TransactionType type;
    private final Ser ser;

    Serializers(Call.TransactionType type, Ser ser) {
      this.type = type;
      this.ser = ser;
    }

    public static Ser find(Call.TransactionType type) {
      for (Serializers ser : Serializers.values()) {
        if (ser.type == type) {
          return ser.ser;
        }
      }
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  public static byte[] serialize(Call.TransactionType type, Object summary) throws TException {
    Ser ser = Serializers.find(type);
    if (ser != null) {
      return ser.serialize(summary);
    }
    return null;
  }

}
