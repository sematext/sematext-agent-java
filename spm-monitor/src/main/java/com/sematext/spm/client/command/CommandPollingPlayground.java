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

import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.monitor.thrift.TCommand;
import com.sematext.spm.client.monitor.thrift.TCommandResponse;
import com.sematext.spm.client.monitor.thrift.TCommandResponseStatus;
import com.sematext.spm.client.monitor.thrift.TCommandType;

public class CommandPollingPlayground {
  public static void main(String[] args) {
    if (args.length != 3) {
      System.err.println("Usage: " + CommandPollingPlayground.class.getName() + " [token] [agentId] [host]");
    }

    String token = args[0], agentId = args[1], host = args[2];

    CommandHandler handler = new CommandHandler() {
      @Override
      public Cancellable handle(TCommand command, ResponseCallback callback) {
        callback.respond(new TCommandResponse(TCommandResponseStatus.SUCCESS, command.getId()));
        System.out.println("Received command: " + new String(command.getRequest()));
        return null;
      }
    };

    CommandPolling polling = CommandPolling.builder()
        .host(host)
        .token(token)
        .id(agentId)
        .pollingEndpoint("http://localhost:8082/spm-receiver/command/poll")
        .responseEndpoint("http://localhost:8082/spm-receiver/command/response")
        .pollingInterval(5, TimeUnit.SECONDS)
        .addHandler(TCommandType.PING, handler)
        .build();

    polling.start();
  }
}
