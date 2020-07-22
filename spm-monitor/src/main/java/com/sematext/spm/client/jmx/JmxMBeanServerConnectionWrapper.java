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
package com.sematext.spm.client.jmx;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.status.AgentStatusRecorder;
import com.sematext.spm.client.status.AgentStatusRecorder.ConnectionStatus;

import java.rmi.server.RMISocketFactory;

/**
 * Provides one JMX connection to all clients. Before giving the connection, it checks if the connection is still alive.
 * If connection is not alive, it will return null. Once the connection is established, this class will keep it until
 * the connection dies for some reason.
 * <p>
 * It also has built in recovery mechanism which works like this:
 * - in case connection is not alive, it will try to obtain it again in TIME_BETWEEN_SHORT_RETRIES ms or in
 * TIME_BETWEEN_LONG_RETRIES ms
 * - this depends on whether more than MAX_SHORT_RETRIES_PERIOD ms passed since last successful check of connection health
 * (if more time passed, TIME_BETWEEN_LONG_RETRIES will be used, otherwise TIME_BETWEEN_SHORT_RETRIES)
 * - in case connection is not alive, all requests for connection which come before TIME_BETWEEN_*_RETRIES ms passes will just
 * get null as connection (JmxMBeanServerConnectionWrapper will not try to reconnect to monitored service)
 * - once fresh connection is obtained, it will be used (without closing or reconnecting) until it dies for whatever the reason,
 * in which case TIME_BETWEEN_SHORT_RETRIES ms will be again used as initial retry interval for MAX_SHORT_RETRIES_PERIOD ms time
 */
public final class JmxMBeanServerConnectionWrapper {
  private static final Log LOG = LogFactory.getLog(JmxMBeanServerConnectionWrapper.class);

  private static JmxMBeanServerConnectionWrapper instance;

  private MBeanServerConnection mbeanServer;
  private JMXConnector connector;

  private static long lastSuccessTime = System.currentTimeMillis();
  private static long lastFailedTime = 0L;
  private static int consecutiveConnErrors = 0;

  private static final long TIME_BETWEEN_SHORT_RETRIES = 10 * 1000;
  private static final long TIME_BETWEEN_LONG_RETRIES = 45 * 1000;
  private static final long MAX_SHORT_RETRIES_PERIOD = 5 * 60 * 1000;

  private JmxMBeanServerConnectionWrapper(String jmxServiceUrl, String username, String password) throws IOException {
    if (jmxServiceUrl == null) {
      mbeanServer = ManagementFactory.getPlatformMBeanServer();
      connector = null;
    } else {
      // wildfly would have something like this: service:jmx:http-remoting-jmx://localhost:9990
      // if user specified full url, we should use that, otherwise build the full url
      if (!jmxServiceUrl.startsWith("service:jmx")) {
        jmxServiceUrl = "service:jmx:rmi:///jndi/rmi://" + jmxServiceUrl + "/jmxrmi";
      }

      final JMXServiceURL jmxServiceURL = new JMXServiceURL(jmxServiceUrl);

      Map<String, Object> env = new UnifiedMap<String, Object>();

      if (username != null && password != null) {
        String[] credentials = new String[2];
        credentials[0] = username;
        credentials[1] = password;
        env.put(JMXConnector.CREDENTIALS, credentials);
      }
      String[] tokens = jmxServiceUrl.split("//");
      String host = tokens[tokens.length - 1].split(":")[0];
      // cassandra overrides rmi.server.hostname property to localhost, so localhost address is embedded in RMI stub
      // to overcome that, we override it to right host when creating socket
      if (RMISocketFactory.getSocketFactory() == null) {
        if (host.length() > 0 && !host.contains("localhost") && !host.contains("127.0.0.1")) {
          RMISocketFactory.setSocketFactory(new HostOverrideClientSocketFactory(host));
        }
      }
      connector = JMXConnectorFactory.connect(jmxServiceURL, env);
      mbeanServer = connector.getMBeanServerConnection();
      LOG.info("Created JMX connection to " + mbeanServer + " at URL: " + jmxServiceURL);
    }
  }

  public static synchronized JmxMBeanServerConnectionWrapper getInstance(JmxServiceContext ctx) {
    try {
      if (instance == null) {
        LOG.info("Initializing new instance");
        long currentTime = System.currentTimeMillis();
        long timeSinceLastFailed = currentTime - lastFailedTime;
        long timeSinceLastSucceeded = currentTime - lastSuccessTime;

        LOG.info("Current time: " + currentTime + ", timeSinceLastFailed: " + timeSinceLastFailed +
                     ", timeSinceLastSucceeded: " + timeSinceLastSucceeded);

        if (timeSinceLastSucceeded > MAX_SHORT_RETRIES_PERIOD) {
          LOG.info("Long retries period active");
          if (timeSinceLastFailed > TIME_BETWEEN_LONG_RETRIES) {
            createInstance(ctx);
          } else {
            return null;
          }
        } else {
          LOG.info("Short retries period active");
          if (timeSinceLastFailed > TIME_BETWEEN_SHORT_RETRIES) {
            createInstance(ctx);
          } else {
            return null;
          }
        }
      }

      // check if connection is still active with this light call
      instance.getMbeanServerConnection().getMBeanCount();
      lastSuccessTime = System.currentTimeMillis();
      lastFailedTime = 0L;
      consecutiveConnErrors = 0;
      AgentStatusRecorder.GLOBAL_INSTANCE.updateConnectionStatus(ConnectionStatus.OK);
    } catch (Throwable thr) {
      AgentStatusRecorder.GLOBAL_INSTANCE.updateConnectionStatus(ConnectionStatus.FAILED, thr.getMessage());
      if (consecutiveConnErrors == 0) {
        // print stacktrace only for first error, no need to fill logs with pile of exactly the same exception traces
        LOG.error("Can't connect to JMX server " + ctx.getUrl() + " with user " + ctx.getUsername(), thr);
      } else {
        LOG.error("Can't connect to JMX server " + ctx.getUrl() + " with user " + ctx.getUsername());
      }

      consecutiveConnErrors++;

      if (instance != null) {
        try {
          instance.closeConnection();
        } catch (Throwable thr2) {
          LOG.error("Error while closing JMX connection", thr2);
        }
      }

      instance = null;
      lastFailedTime = System.currentTimeMillis();

      return null;
    }

    return instance;
  }

  protected static void createInstance(JmxServiceContext ctx) {
    LOG.info("Creating instance");

    if (ctx == null) {
      throw new IllegalStateException("JmxMBeanServerConnection can't be initialized without JmxServiceContext " +
                                          "being initialized first!");
    }

    try {
      instance = new JmxMBeanServerConnectionWrapper(ctx.getUrl(), ctx.getUsername(), ctx.getPassword());

      LOG.info("New instance created");
    } catch (IOException ioe) {
      throw new RuntimeException("Can't initialize JMX server connection!", ioe);
    }
  }

  public void closeConnection() {
    if (connector != null) {
      try {
        connector.close();
      } catch (IOException e) {
        LOG.error(e);
        // DO NOTHING
      }
    }
  }

  public MBeanServerConnection getMbeanServerConnection() {
    return mbeanServer;
  }
}
