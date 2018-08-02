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

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.monitor.thrift.TCommand;
import com.sematext.spm.client.monitor.thrift.TCommandResponse;
import com.sematext.spm.client.monitor.thrift.TCommandResponseStatus;
import com.sematext.spm.client.monitor.thrift.TCommandType;

public class PingCommandHandler implements CommandHandler {
  private final Log LOG = LogFactory.getLog(PingCommandHandler.class);

  @Override
  public Cancellable handle(TCommand command, ResponseCallback callback) {
    if (command.getType() == TCommandType.PING) {
      LOG.info("Got ping request from server.");
      TCommandResponse response = new TCommandResponse(TCommandResponseStatus.SUCCESS, command.getId());
      response.setResponse("pong".getBytes());
      callback.respond(response);
    }
    return null;
  }
}
