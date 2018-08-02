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
package com.sematext.spm.client.command;

import org.apache.thrift.TException;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.monitor.thrift.TCommand;
import com.sematext.spm.client.monitor.thrift.TCommandResponse;
import com.sematext.spm.client.monitor.thrift.TCommandResponseStatus;
import com.sematext.spm.client.monitor.thrift.TIsTracingEnabledResponse;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.impl.ThriftUtils;

public final class IsTracingEnabledHandler implements CommandHandler {

  private final Log log = LogFactory.getLog(IsTracingEnabledHandler.class);

  @Override
  public Cancellable handle(TCommand command, ResponseCallback callback) {
    try {
      final TIsTracingEnabledResponse isEnabledResp = new TIsTracingEnabledResponse(ServiceLocator
                                                                                        .getTracingAgentControl()
                                                                                        .isEnabled());
      byte[] serializedIsEnabledResp = ThriftUtils.binaryProtocolSerializer().serialize(isEnabledResp);
      final TCommandResponse resp = new TCommandResponse(TCommandResponseStatus.SUCCESS, command.getId());
      resp.setResponse(serializedIsEnabledResp);
      callback.respond(resp);
    } catch (TException e) {
      log.error("Can't serialize response.", e);
      final TCommandResponse resp = new TCommandResponse(TCommandResponseStatus.SUCCESS, command.getId());
      resp.setFailureReason("Can't serialize response");
      callback.respond(resp);
    }
    return null;
  }
}
