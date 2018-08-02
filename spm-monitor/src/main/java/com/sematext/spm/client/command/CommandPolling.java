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

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.monitor.thrift.TCommand;
import com.sematext.spm.client.monitor.thrift.TCommandResponse;
import com.sematext.spm.client.monitor.thrift.TCommandType;
import com.sematext.spm.client.snap.serializer.TBinaryProto;
import com.sematext.spm.client.util.Hex;
import com.sematext.spm.client.util.IOUtils;
import com.sematext.spm.client.util.Preconditions;
import com.sematext.spm.client.util.StringUtils;
import com.sematext.spm.client.util.Threads;

public final class CommandPolling {

  private final Log log = LogFactory.getLog(CommandPolling.class);

  private String pollingEndpoint;
  private String responseEndpoint;
  private String proxyHost;
  private int proxyPort;
  private String proxyUsername;
  private String proxyPassword;
  private long pollingInterval;
  private int retriesCount = 5;
  private TimeUnit pollingIntervalTimeUnit;
  private long retryInterval;
  private TimeUnit retryIntervalTimeUnit;
  private String token;
  private String host;
  private String processName;
  private String id;
  private CancellableCommandHandler commandHandler;
  private final ArrayBlockingQueue<ResponseAttempt> responses = new ArrayBlockingQueue<ResponseAttempt>(20);
  private volatile boolean running = false;
  private MonitorConfig monitorConfig;

  private CommandPolling() {
  }

  public static class ResponseAttempt {
    private final TCommandResponse response;
    private final int retries;

    ResponseAttempt(TCommandResponse response, int retries) {
      this.response = response;
      this.retries = retries;
    }

    ResponseAttempt decreaseRetries() {
      return new ResponseAttempt(response, retries - 1);
    }
  }

  public static class Builder {
    private final CommandPolling polling = new CommandPolling();
    private final Map<TCommandType, CommandHandler> handlers = new UnifiedMap<TCommandType, CommandHandler>();

    private Builder() {
    }

    public Builder pollingEndpoint(String pollingEndpoint) {
      polling.pollingEndpoint = pollingEndpoint;
      return this;
    }

    public Builder responseEndpoint(String responseEndpoint) {
      polling.responseEndpoint = responseEndpoint;
      return this;
    }

    public Builder proxy(String proxyHost, int port, String username, String password) {
      polling.proxyHost = proxyHost;
      polling.proxyPort = port;
      polling.proxyUsername = username;
      polling.proxyPassword = password;
      return this;
    }

    public Builder pollingInterval(long interval, TimeUnit unit) {
      polling.pollingInterval = interval;
      polling.pollingIntervalTimeUnit = unit;
      return this;
    }

    public Builder retryInterval(long interval, TimeUnit unit) {
      polling.retryInterval = interval;
      polling.retryIntervalTimeUnit = unit;
      return this;
    }

    public Builder token(String token) {
      polling.token = token;
      return this;
    }

    public Builder host(String host) {
      polling.host = host;
      return this;
    }

    public Builder id(String id) {
      polling.id = id;
      return this;
    }

    public Builder processName(String jvmName) {
      polling.processName = jvmName;
      return this;
    }

    public Builder retriesCount(int count) {
      polling.retriesCount = count;
      return this;
    }

    public Builder monitorConfig(MonitorConfig monitorConfig) {
      polling.monitorConfig = monitorConfig;
      return this;
    }

    public Builder addHandler(TCommandType type, CommandHandler handler) {
      handlers.put(type, handler);
      return this;
    }

    public CommandPolling build() {
      Preconditions.checkNotNull(polling.pollingEndpoint, "Polling endpoint should be defined.");
      Preconditions.checkNotNull(polling.responseEndpoint, "Response endpoint should be defined.");
      Preconditions.checkNotNull(polling.token, "Token should be defined");
      Preconditions.checkNotNull(polling.host, "Host should be defined");
      Preconditions.checkNotNull(polling.id, "Id should be defined");
      Preconditions.check(polling.pollingInterval > 0, "Polling interval should be defined");
      Preconditions.checkNotNull(polling.pollingIntervalTimeUnit, "Polling interval time unit should be defined");
      Preconditions.check(polling.retryInterval > 0, "Retry interval should be defined");
      Preconditions.checkNotNull(polling.retryIntervalTimeUnit, "Retry interval time unit should be defined");
      polling.commandHandler = new CancellableCommandHandler(handlers, polling.enqueueResponseCallback);
      return polling;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private URI uri(String endpoint) throws URISyntaxException {
    final URIBuilder builder = new URIBuilder(endpoint)
        .addParameter("token", token)
        .addParameter("host", host)
        .addParameter("agentId", id);
    if (processName != null) {
      builder.addParameter("processName", processName);
    }
    return builder.build();
  }

  private CloseableHttpClient newHttpClient() {
    HttpClientBuilder builder = HttpClients.custom();
    if (!StringUtils.isEmpty(proxyHost)) {
      if (proxyPort != 0) {
        builder.setProxy(new HttpHost(proxyHost, proxyPort));
      } else {
        builder.setProxy(new HttpHost(proxyHost));
      }
    }
    if (!StringUtils.isEmpty(proxyUsername)) {
      builder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
      CredentialsProvider credProvider = new BasicCredentialsProvider();
      AuthScope scope;
      if (proxyPort != 0) {
        scope = new AuthScope(proxyHost, proxyPort);
      } else {
        scope = new AuthScope(proxyHost, AuthScope.ANY_PORT);
      }
      credProvider.setCredentials(scope, new UsernamePasswordCredentials(proxyUsername, proxyPassword));
      builder.setDefaultCredentialsProvider(credProvider);
    }
    return builder.build();
  }

  private List<TCommand> poll() throws URISyntaxException, IOException {
    CloseableHttpClient client = newHttpClient();
    HttpGet poll = new HttpGet(uri(pollingEndpoint));
    CloseableHttpResponse response = null;
    try {
      response = client.execute(poll);
      if (response.getStatusLine().getStatusCode() == 200) {
        byte[] content = IOUtils.toByteArray(response.getEntity().getContent());
        try {
          return TBinaryProto.readList(TCommand.class, content);
        } catch (IllegalStateException e) {
          log.error("Can't deserialize command: " + Hex.hexDump(content) + ".", e);
          throw e;
        }
      } else {
        log.warn("Unexpected response code from " + pollingEndpoint + " " + response.getStatusLine());
      }
      return Collections.emptyList();
    } finally {
      if (response != null) {
        response.close();
      }
      client.close();
    }
  }

  private boolean respond(TCommandResponse commandResponse) throws URISyntaxException, IOException {
    CloseableHttpClient client = newHttpClient();
    HttpPost post = new HttpPost(uri(responseEndpoint));
    post.setEntity(new ByteArrayEntity(TBinaryProto.toByteArray(commandResponse)));
    CloseableHttpResponse response = null;
    try {
      response = client.execute(post);
      if (response.getStatusLine().getStatusCode() != 200) {
        log.warn("Unexpected response status from " + responseEndpoint + " " + response.getStatusLine());
        return false;
      }
      log.info("Response status from " + responseEndpoint + " " + response.getStatusLine());
      return true;
    } finally {
      if (response != null) {
        response.close();
      }
      client.close();
    }
  }

  private final ResponseCallback enqueueResponseCallback = new ResponseCallback() {
    @Override
    public void respond(TCommandResponse response) {
      if (response == null) {
        log.warn("Respond with empty response.");
        return;
      }

      if (!responses.offer(new ResponseAttempt(response, retriesCount))) {
        log.error("Responses queue is full, skipping response.");
      }
    }
  };

  private void handleCommands(List<TCommand> commands) {
    for (TCommand command : commands) {
      try {
        commandHandler.handle(command);
      } catch (Exception e) {
        log.error("Error while handling command.", e);
      }
    }
  }

  // private final Thread pollingThread = Threads.newDaemonThread("polling-thread" + MonitorUtil.getMonitorId(monitorConfig.getMonitorPropertiesFile()), new Runnable() {
  private final Thread pollingThread = Threads.newDaemonThread("polling-thread", new Runnable() {
    @Override
    public void run() {
      while (running) {
        List<TCommand> commands = null;
        try {
          commands = poll();
          log.info("Command polling got commands: " + commands);
        } catch (Exception e) {
          log.error("Can't poll commands from server.", e);
        }
        if (commands != null) {
          handleCommands(commands);
        }
        try {
          pollingIntervalTimeUnit.sleep(pollingInterval);
        } catch (InterruptedException e) {
          log.warn("Polling thread interrupted, exiting.", e);
          break;
        }
      }
    }
  });

  // private final Thread responseThread = Threads.newDaemonThread("response-thread" + MonitorUtil.getMonitorId(monitorConfig.getMonitorPropertiesFile()), new Runnable() {
  private final Thread responseThread = Threads.newDaemonThread("polling-response-thread", new Runnable() {
    @Override
    public void run() {
      while (running) {
        try {
          ResponseAttempt response = responses.poll(1, TimeUnit.SECONDS);
          if (response != null) {
            log.info("Command response found: " + response.response);
            boolean retry;
            try {
              retry = !respond(response.response);
            } catch (Exception e) {
              log.error("Can't send response to server. Response will be re-queued.", e);
              retry = true;
            }

            if (retry && response.retries > 1) {
              responses.offer(response.decreaseRetries());

              retryIntervalTimeUnit.sleep(retryInterval);
            }
          }
        } catch (InterruptedException e) {
          log.warn("Response thread interrupted, exiting.", e);
          break;
        }
      }
    }
  });

  public void start() {
    if (running) {
      throw new IllegalStateException("Polling already started");
    }
    running = true;
    pollingThread.start();
    responseThread.start();
  }

  public void stop() {
    pollingThread.interrupt();
    responseThread.interrupt();
    running = false;
  }

}
