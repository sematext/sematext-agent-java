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

import com.sematext.spm.client.monitor.thrift.TCommand;
import com.sematext.spm.client.monitor.thrift.TCommandResponse;
import com.sematext.spm.client.monitor.thrift.TCommandResponseStatus;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;

public class EnableTracingHandler implements CommandHandler {
  @Override
  public Cancellable handle(TCommand command, ResponseCallback callback) {
    final TCommandResponseStatus status = ServiceLocator.getTracingAgentControl().enable() ?
        TCommandResponseStatus.SUCCESS
        :
        TCommandResponseStatus.FAILURE;

    callback.respond(new TCommandResponse(status, command.getId()));
    return null;
  }
}
