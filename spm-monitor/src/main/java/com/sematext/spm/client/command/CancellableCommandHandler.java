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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.monitor.thrift.TCancelRequest;
import com.sematext.spm.client.monitor.thrift.TCommand;
import com.sematext.spm.client.monitor.thrift.TCommandResponse;
import com.sematext.spm.client.monitor.thrift.TCommandResponseStatus;
import com.sematext.spm.client.monitor.thrift.TCommandType;
import com.sematext.spm.client.util.Preconditions;

public final class CancellableCommandHandler {
  private static final Log LOG = LogFactory.getLog(CancellableCommandHandler.class);

  private final Map<TCommandType, CommandHandler> handlers;
  private final ResponseCallback responseCallback;
  private final Map<Long, Cancellable> cancellableHandlers = new ConcurrentHashMap<Long, Cancellable>();

  CancellableCommandHandler(Map<TCommandType, CommandHandler> handlers, ResponseCallback responseCallback) {
    Preconditions.checkNotNull(handlers, "Handlers should be defined.");
    Preconditions.checkNotNull(responseCallback, "ResponseCallback should be defined.");

    this.handlers = handlers;
    this.responseCallback = responseCallback;
  }

  private class WrappedCallback implements ResponseCallback {
    private volatile boolean respond = false;

    @Override
    public void respond(TCommandResponse response) {
      if (response != null) {
        respond = true;
        cancellableHandlers.remove(response.getId());
        responseCallback.respond(response);
      } else {
        LOG.error("Got null response.");
      }
    }
  }

  public void handle(final TCommand command) {
    Preconditions.checkNotNull(command, "Command should be defined");

    if (command.getType() == TCommandType.CANCEL) {
      final TCancelRequest request = new TCancelRequest();

      LOG.info("Got cancel request for command " + request.getId() + ".");

      final Cancellable cancellable = cancellableHandlers.get(request.getId());
      if (cancellable == null) {
        LOG.warn("Can't cancel command for id: " + request.getId() + ".");

        final TCommandResponse failedResponse = new TCommandResponse(TCommandResponseStatus.FAILURE, command.getId());
        failedResponse
            .setFailureReason("Command with id '" + request.getId() + "' is either not running or not cancellable.");
        responseCallback.respond(failedResponse);
      } else {
        LOG.info("Cancelling command " + request.getId() + ".");
        cancellable.cancel(new CancelledCallback() {
          @Override
          public void cancelled() {
            cancellableHandlers.remove(request.getId());
            responseCallback.respond(new TCommandResponse(TCommandResponseStatus.SUCCESS, command.getId()));
            LOG.info("Command " + request.getId() + " successfully cancelled.");
          }
        });
      }
    } else {
      final CommandHandler handler = handlers.get(command.getType());
      if (handler != null) {
        final WrappedCallback wrappedCallback = new WrappedCallback();
        final Cancellable cancellable = handler.handle(command, wrappedCallback);
        if (cancellable != null && !wrappedCallback.respond) {
          cancellableHandlers.put(command.getId(), cancellable);
        }
      } else {
        LOG.warn("Unhandled command '" + command.getType() + "'.");
      }
    }
  }
}
