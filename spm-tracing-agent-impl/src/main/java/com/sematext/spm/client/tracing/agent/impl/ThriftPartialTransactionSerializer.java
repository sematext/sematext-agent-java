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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.thrift.TCall;
import com.sematext.spm.client.tracing.thrift.TEndpoint;
import com.sematext.spm.client.tracing.thrift.TFailureType;
import com.sematext.spm.client.tracing.thrift.TPartialTransaction;
import com.sematext.spm.client.tracing.thrift.TTransactionType;

public class ThriftPartialTransactionSerializer {
  public static TPartialTransaction toThrift(PartialTransaction transaction) throws TException {
    TPartialTransaction thrift = new TPartialTransaction();
    thrift.setCallId(transaction.getCallId());
    thrift.setParentCallId(transaction.getParentCallId());
    thrift.setTraceId(transaction.getTraceId());
    thrift.setRequest(transaction.getRequest());
    thrift.setStartTimestamp(transaction.getStartTimestamp());
    thrift.setEndTimestamp(transaction.getEndTimestamp());
    thrift.setDuration(transaction.getDuration());
    thrift.setToken(transaction.getToken());
    thrift.setFailed(transaction.isFailed());
    thrift.setEntryPoint(transaction.isEntryPoint());
    thrift.setAsynchronous(transaction.isAsynchronous());
    thrift.setTransactionType(TTransactionType.valueOf(transaction.getTransactionType().name()));

    if (transaction.getTransactionSummary() != null) {
      byte[] summary = ThriftTransactionSummarySerializer.serialize(transaction.getTransactionType(),
                                                                    transaction.getTransactionSummary());
      thrift.setTransactionSummary(summary);
    }

    if (transaction.getEndpoint() != null) {
      TEndpoint endpoint = new TEndpoint();
      endpoint.setHostname(transaction.getEndpoint().getHostname());
      endpoint.setAddress(transaction.getEndpoint().getAddress());
      thrift.setEndpoint(endpoint);
    }

    final List<TCall> thriftCalls = new ArrayList<TCall>();

    for (Call call : transaction.getCalls()) {
      TCall thriftCall = ThriftCallSerializer.toThrift(call);
      thriftCalls.add(thriftCall);
    }

    thrift.setCalls(thriftCalls);

    if (transaction.getExceptionStackTrace() != null) {
      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      final PrintWriter writer = new PrintWriter(os);
      transaction.getExceptionStackTrace().printStackTrace(writer);
      writer.flush();

      thrift.setExceptionStackTrace(os.toByteArray());
    }

    if (transaction.getFailureType() != null) {
      final TFailureType type = TFailureType.valueOf(transaction.getFailureType().name());
      thrift.setFailureType(type);
    }

    thrift.setParameters(transaction.getParameters());

    return thrift;
  }

  public static byte[] serialize(PartialTransaction transaction) throws TException {
    return ThriftUtils.binaryProtocolSerializer().serialize(toThrift(transaction));
  }
}
