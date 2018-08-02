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
import com.sematext.spm.client.tracing.thrift.TCall;
import com.sematext.spm.client.tracing.thrift.TCallTag;

public final class ThriftCallSerializer {
  private ThriftCallSerializer() {
  }

  public static TCall toThrift(Call call) throws TException {
    TCall thriftCall = new TCall();

    thriftCall.setCallId(call.getCallId());
    thriftCall.setParentCallId(call.getParentCallId());
    thriftCall.setLevel(call.getLevel());
    thriftCall.setStartTimestamp(call.getStartTimestamp());
    thriftCall.setEndTimestamp(call.getEndTimestamp());
    thriftCall.setDuration(call.getDuration());
    thriftCall.setSelfDuration(call.getSelfDuration());
    thriftCall.setSignature(call.getSignature());
    if (call.getFailed() != null) {
      thriftCall.setFailed(call.getFailed());
    } else {
      thriftCall.setFailedIsSet(false);
    }
    if (call.getExternal() != null) {
      thriftCall.setExternal(call.getExternal());
    } else {
      thriftCall.setExternalIsSet(false);
    }
    if (call.getCallTag() != null) {
      thriftCall.setTag(TCallTag.valueOf(call.getCallTag().name()));
    } else {
      thriftCall.setTagIsSet(false);
    }
    thriftCall.setEntryPoint(call.isEntryPoint());
    thriftCall.setCrossAppToken(call.getCrossAppToken());
    if (call.getCrossAppCallId() != null) {
      thriftCall.setCrossAppCallId(call.getCrossAppCallId());
    } else {
      thriftCall.setCrossAppCallIdIsSet(false);
    }
    if (call.getCrossAppParentCallId() != null) {
      thriftCall.setCrossAppParentCallId(call.getCrossAppParentCallId());
    } else {
      thriftCall.setCrossAppParentCallIdIsSet(false);
    }
    if (call.getCrossAppDuration() != null) {
      thriftCall.setCrossAppDuration(call.getCrossAppDuration());
    } else {
      thriftCall.setCrossAppDurationIsSet(false);
    }

    thriftCall.setCrossAppSampled(call.isCrossAppSampled());

    if (call.getAnnotation() != null) {
      byte[] ser = ThriftAnnotationSerializer.serialize(call.getCallTag(), call.getAnnotation());
      if (ser != null) {
        thriftCall.setAnnotation(ser);
      }
    }

    thriftCall.setParameters(call.getParameters());

    return thriftCall;
  }

  public static byte[] serialize(Call call) {
    try {
      TCall thriftCall = toThrift(call);
      return ThriftUtils.binaryProtocolSerializer().serialize(thriftCall);
    } catch (TException e) {
      throw new RuntimeException("Can't serialize call.", e);
    }
  }

}
