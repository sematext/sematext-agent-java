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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertNotNull;

import com.sematext.spm.client.util.ThriftUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TIOStreamTransport;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.monitor.thrift.TCancelRequest;
import com.sematext.spm.client.monitor.thrift.TCommand;
import com.sematext.spm.client.monitor.thrift.TCommandResponse;
import com.sematext.spm.client.monitor.thrift.TCommandResponseStatus;
import com.sematext.spm.client.monitor.thrift.TCommandType;
import com.sematext.spm.client.snap.serializer.TBinaryProto;

@RunWith(DataProviderRunner.class)
public class CommandPollingTest {
  static class EmbeddedServer {
    int port;
    HttpServer server;
    volatile boolean failOnPoll = false;
    volatile boolean failOnRespond = false;
    volatile List<TCommand> pendingCommands = new ArrayList<TCommand>();
    ArrayBlockingQueue<TCommandResponse> responses = new ArrayBlockingQueue<TCommandResponse>(20);

    byte[] serializePendingCommands() {
      List<TCommand> commands = pendingCommands;
      return TBinaryProto.toByteArray(commands);
    }

    TCommandResponse deserializeResponse(InputStream is) throws TException {
      TCommandResponse response = new TCommandResponse();
      TBinaryProtocol protocol = new TBinaryProtocol(new TIOStreamTransport(is));
      response.read(protocol);
      return response;
    }

    void start() throws Exception {
      port = 8080;
      while (true) {
        server = HttpServer.create();
        server.createContext("/command/poll", new HttpHandler() {
          @Override
          public void handle(HttpExchange httpExchange) throws IOException {
            if (failOnPoll) {
              httpExchange.sendResponseHeaders(403, 0);
              httpExchange.close();
              return;
            }

            byte[] thriftCommands;
            thriftCommands = serializePendingCommands();
            httpExchange.sendResponseHeaders(200, thriftCommands.length);
            httpExchange.getResponseBody().write(thriftCommands);
            httpExchange.getResponseBody().close();
            httpExchange.close();
          }
        });

        server.createContext("/command/respond", new HttpHandler() {
          @Override
          public void handle(HttpExchange httpExchange) throws IOException {
            if (!httpExchange.getRequestMethod().equals("POST")) {
              httpExchange.sendResponseHeaders(405, 0);
              httpExchange.close();
              return;
            }

            if (failOnRespond) {
              httpExchange.sendResponseHeaders(403, 0);
              httpExchange.close();
              return;
            }

            TCommandResponse response = new TCommandResponse();
            try {
              TBinaryProto.read(response, httpExchange.getRequestBody());
            } catch (Exception e) {
              throw new IOException("Can't derserialize response.", e);
            }
            responses.add(response);
            httpExchange.sendResponseHeaders(200, 0);
            httpExchange.close();
          }
        });
        try {
          server.bind(new InetSocketAddress("localhost", port), 0);
          server.start();
          System.out.println("Started on port: " + port + ".");
          break;
        } catch (BindException e) {
          port++;
        }
      }
    }

    void stop() {
      server.stop(0);
    }
  }

  @DataProvider
  public static Object[][] embeddedServerProvider() throws Exception {
    EmbeddedServer server = new EmbeddedServer();
    server.start();
    String pollingEndpoint = "http://localhost:" + server.port + "/command/poll";
    String responseEndpoint = "http://localhost:" + server.port + "/command/respond";
    return new Object[][] {
        new Object[] { server, pollingEndpoint, responseEndpoint },
    };
  }

  static class QueuedCommandHandler implements CommandHandler {
    final ArrayBlockingQueue<TCommand> commands = new ArrayBlockingQueue<TCommand>(20);

    @Override
    public Cancellable handle(TCommand command, ResponseCallback callback) {
      commands.add(command);
      return null;
    }
  }

  @Test
  @UseDataProvider("embeddedServerProvider")
  public void testHandlerShouldReceiveCommandsFromServer(EmbeddedServer server, String pollingEndpoint,
                                                         String responseEndpoint) throws Exception {
    QueuedCommandHandler handler = new QueuedCommandHandler();
    CommandPolling polling = CommandPolling.builder()
        .pollingEndpoint(pollingEndpoint)
        .responseEndpoint(responseEndpoint)
        .host("host-1")
        .token("1234")
        .id("4321")
        .pollingInterval(2, TimeUnit.SECONDS)
        .retryInterval(2, TimeUnit.SECONDS)
        .addHandler(TCommandType.PROFILE, handler)
        .build();
    polling.start();

    TCommand c1 = new TCommand(TCommandType.PROFILE, 1);
    server.pendingCommands = Arrays.asList(c1);

    TCommand cmd = handler.commands.poll(4, TimeUnit.SECONDS);
    assertNotNull(cmd);
    assertEquals(cmd.getId(), 1);

    TCommand c2 = new TCommand(TCommandType.PROFILE, 2);
    server.pendingCommands = Arrays.asList(c2);

    cmd = handler.commands.poll(4, TimeUnit.SECONDS);
    assertNotNull(cmd);
    assertEquals(cmd.getId(), 2);
  }

  @Test
  @UseDataProvider("embeddedServerProvider")
  public void testShouldRecoverAfterServerFailureResponses(EmbeddedServer server, String pollingEndpoint,
                                                           String responseEndpoint) throws Exception {
    QueuedCommandHandler handler = new QueuedCommandHandler();
    CommandPolling polling = CommandPolling.builder()
        .pollingEndpoint(pollingEndpoint)
        .responseEndpoint(responseEndpoint)
        .host("host-1")
        .token("1234")
        .id("4321")
        .pollingInterval(1, TimeUnit.SECONDS)
        .retryInterval(2, TimeUnit.SECONDS)
        .addHandler(TCommandType.PROFILE, handler)
        .build();
    polling.start();

    TCommand c1 = new TCommand(TCommandType.PROFILE, 1);
    server.pendingCommands = Arrays.asList(c1);

    server.failOnPoll = true;

    TimeUnit.SECONDS.sleep(5);

    TCommand cmd = handler.commands.poll(2, TimeUnit.SECONDS);
    assertNull(cmd);

    server.failOnPoll = false;

    cmd = handler.commands.poll(2, TimeUnit.SECONDS);
    assertNotNull(cmd);
  }

  @Test
  @UseDataProvider("embeddedServerProvider")
  public void testShouldForwardHandlerResponseToServer(EmbeddedServer server, String pollingEndpoint,
                                                       String responseEndpoint) throws Exception {
    CommandHandler handler = new CommandHandler() {
      @Override
      public Cancellable handle(TCommand command, ResponseCallback callback) {
        callback.respond(new TCommandResponse(TCommandResponseStatus.SUCCESS, command.getId()));
        return null;
      }
    };

    CommandPolling polling = CommandPolling.builder()
        .pollingEndpoint(pollingEndpoint)
        .responseEndpoint(responseEndpoint)
        .host("host-1")
        .token("1234")
        .id("4321")
        .pollingInterval(1, TimeUnit.SECONDS)
        .retryInterval(2, TimeUnit.SECONDS)
        .addHandler(TCommandType.PROFILE, handler)
        .build();
    polling.start();

    TCommand c1 = new TCommand(TCommandType.PROFILE, 1);

    server.pendingCommands = Arrays.asList(c1);

    TCommandResponse response = server.responses.poll(5, TimeUnit.SECONDS);
    assertNotNull(response);
    assertEquals(response.getId(), 1);
  }

  @Test
  @UseDataProvider("embeddedServerProvider")
  public void testShouldResendResponseInCaseOfFailureResponse(EmbeddedServer server, String pollingEndpoint,
                                                              String responseEndpoint) throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);

    CommandHandler handler = new CommandHandler() {
      @Override
      public Cancellable handle(TCommand command, ResponseCallback callback) {
        callback.respond(new TCommandResponse(TCommandResponseStatus.SUCCESS, command.getId()));
        latch.countDown();
        return null;
      }
    };

    CommandPolling polling = CommandPolling.builder()
        .pollingEndpoint(pollingEndpoint)
        .responseEndpoint(responseEndpoint)
        .host("host-1")
        .token("1234")
        .id("4321")
        .pollingInterval(1, TimeUnit.SECONDS)
        .retryInterval(2, TimeUnit.SECONDS)
        .addHandler(TCommandType.PROFILE, handler)
        .build();
    polling.start();

    server.failOnRespond = true;

    TCommand c1 = new TCommand(TCommandType.PROFILE, 1);

    server.pendingCommands = Arrays.asList(c1);

    latch.await(3, TimeUnit.SECONDS);

    TCommandResponse response = server.responses.poll(1, TimeUnit.SECONDS);
    assertNull(response);

    server.failOnRespond = false;

    response = server.responses.poll(4, TimeUnit.SECONDS);
    assertNotNull(response);
  }

  @Test
  @UseDataProvider("embeddedServerProvider")
  public void testShouldCancelPendingCommand(EmbeddedServer server, String pollingEndpoint, String responseEndpoint)
      throws Exception {
    final CountDownLatch startLatch = new CountDownLatch(1);

    CommandHandler handler = new CommandHandler() {
      @Override
      public Cancellable handle(final TCommand command, final ResponseCallback callback) {
        startLatch.countDown();

        final SimpleCancellable cancellable = new SimpleCancellable();

        new Thread() {
          @Override
          public void run() {
            while (!cancellable.isCancelled()) {
              try {
                TimeUnit.MILLISECONDS.sleep(10);
              } catch (InterruptedException e) {
                //e.printStackTrace();
              }
            }

            if (cancellable.isCancelled()) {
              cancellable.done();
            } else {
              callback.respond(new TCommandResponse(TCommandResponseStatus.SUCCESS, command.getId()));
            }
          }
        }.start();

        return cancellable;
      }
    };

    CommandPolling polling = CommandPolling.builder()
        .pollingEndpoint(pollingEndpoint)
        .responseEndpoint(responseEndpoint)
        .host("host-1")
        .token("1234")
        .id("4321")
        .pollingInterval(1, TimeUnit.SECONDS)
        .retryInterval(2, TimeUnit.SECONDS)
        .addHandler(TCommandType.PROFILE, handler)
        .build();
    polling.start();

    TCommand c1 = new TCommand(TCommandType.PROFILE, 1);

    server.pendingCommands = Arrays.asList(c1);

    startLatch.await(5, TimeUnit.SECONDS);

    TCommand cancelC1 = new TCommand(TCommandType.CANCEL, 2);
    cancelC1.setRequest(ThriftUtils.binaryProtocolSerializer().serialize(new TCancelRequest(1)));

    server.pendingCommands = Arrays.asList(cancelC1);

    TCommandResponse response = server.responses.poll(10, TimeUnit.SECONDS);
    assertNotNull(response);
    assertEquals(response.getId(), 2);
    assertEquals(response.getStatus(), TCommandResponseStatus.SUCCESS);
  }

}
