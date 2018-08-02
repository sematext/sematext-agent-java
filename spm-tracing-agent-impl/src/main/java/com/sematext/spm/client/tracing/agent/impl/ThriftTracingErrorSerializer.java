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

import java.util.HashMap;

import com.sematext.spm.client.tracing.agent.errors.TracingError;
import com.sematext.spm.client.tracing.thrift.TTracingError;

public class ThriftTracingErrorSerializer {
  public static TTracingError toThrift(TracingError tracingError) {
    TTracingError thrift = new TTracingError();
    thrift.setToken(tracingError.getToken());
    thrift.setTraceId(tracingError.getTraceId());
    thrift.setParentCallId(tracingError.getParentCallId());
    thrift.setCallId(tracingError.getCallId());
    thrift.setTimestamp(tracingError.getTimestamp());
    thrift.setSampled(tracingError.isSampled());
    thrift.setParameters(new HashMap<String, String>(tracingError.getParameters()));
    return thrift;
  }

  public static byte[] serialize(TracingError tracingError) throws TException {
    return ThriftUtils.binaryProtocolSerializer().serialize(toThrift(tracingError));
  }
}
