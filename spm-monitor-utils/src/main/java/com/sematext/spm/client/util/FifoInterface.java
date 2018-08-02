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
package com.sematext.spm.client.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

/**
 * Fifo debug interface (useful to change internal agent state without running servers, like tracing-receiver).
 * <p>
 * Usage:
 * <code>
 * $ mkfifo /tmp/fifo-test
 * $ echo foo >> /tmp/fifo-test
 * $ echo bar >> /tmp/fifo-test
 * </code>
 */
public final class FifoInterface {

  private final Log log = LogFactory.getLog(FifoInterface.class);

  public static interface Handler {
    void handle(String[] arguments);
  }

  private final Map<String, Handler> handlers;

  public FifoInterface(Map<String, Handler> handlers) {
    this.handlers = handlers;
  }

  public void run(final String path) {
    if (!new File(path).exists()) {
      throw new IllegalArgumentException(path + " don't exists");
    }

    new Thread() {
      @Override
      public void run() {
        try {
          final BufferedReader reader = new BufferedReader(new FileReader(path));
          final BufferedWriter writer = new BufferedWriter(new FileWriter(path));
          while (true) {
            final String argsLine = reader.readLine();
            if (argsLine == null) {
              log.info("Closing fifo interface.");
              reader.close();
            }
            final String[] cmdAndArgs = argsLine.split(":");
            final String[] args = new String[cmdAndArgs.length - 1];
            for (int i = 1; i < cmdAndArgs.length; i++) {
              args[i - 1] = cmdAndArgs[i];
            }
            try {
              for (Map.Entry<String, Handler> entry : handlers.entrySet()) {
                if (entry.getKey().equals(cmdAndArgs[0])) {
                  entry.getValue().handle(args);
                }
              }
              writer.write("ok\n");
            } catch (Exception e) {
              log.error("While handing command: " + argsLine, e);
            }
          }
        } catch (Exception e) {
          log.error("While reading from fifo.", e);
        }
      }
    }.start();
  }

  public static class Builder {
    private final Map<String, Handler> handlers = new HashMap<String, Handler>();

    private Builder() {
    }

    public Builder addHandler(String command, Handler handler) {
      handlers.put(command, handler);
      return this;
    }

    public void start(String path) {
      final FifoInterface iface = new FifoInterface(handlers);
      iface.run(path);
    }
  }

  public static Builder create() {
    return new Builder();
  }

}
