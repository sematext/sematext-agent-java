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

import java.util.HashMap;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.monitor.thrift.TCommand;
import com.sematext.spm.client.monitor.thrift.TCommandResponse;
import com.sematext.spm.client.monitor.thrift.TCommandResponseStatus;
import com.sematext.spm.client.monitor.thrift.TInstrumentedMethodState;
import com.sematext.spm.client.monitor.thrift.TUpdateInstrumentationSettings;
import com.sematext.spm.client.unlogger.dynamic.BehaviorDescription;
import com.sematext.spm.client.unlogger.dynamic.BehaviorState;

public class UpdateInstrumentationSettingsHandler implements CommandHandler {

  private final Log log = LogFactory.getLog(UpdateInstrumentationSettingsHandler.class);

  @Override
  public Cancellable handle(TCommand command, ResponseCallback callback) {
    final TUpdateInstrumentationSettings thriftSettings = new TUpdateInstrumentationSettings();
    final Map<BehaviorDescription, BehaviorState> state = new HashMap<BehaviorDescription, BehaviorState>();

    try {
      for (Map.Entry<String, TInstrumentedMethodState> entry : thriftSettings.getStates().entrySet()) {
        final BehaviorDescription behDescription = new BehaviorDescription(entry.getKey());
        final BehaviorState behState = new BehaviorState(entry.getValue().isEntryPoint(), entry.getValue().isEnabled());

        state.put(behDescription, behState);
      }

      log.info("Instrumentation settings Not applied - tracing disabled");

      final TCommandResponse response = new TCommandResponse();

      response.setStatus(TCommandResponseStatus.FAILURE);
      response.setId(command.getId());
      response.setFailureReason("Tracing is disabled");
      callback.respond(response);

      return null;
    } catch (Exception e) {
      log.error("Can't update and apply instrumentation settings", e);
      final TCommandResponse response = new TCommandResponse(TCommandResponseStatus.FAILURE, command.getId());
      response.setFailureReason(e.getMessage());
      callback.respond(response);
      return null;
    }
  }
}
