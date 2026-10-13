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
package com.sematext.spm.client.solr;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanServerConnection;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.http.ServerInfo;
import com.sematext.spm.client.jmx.JmxMBeanServerConnectionWrapper;
import com.sematext.spm.client.jmx.JmxServiceContext;

/**
 * Finds the HTTP endpoint (scheme://host:port) of the monitored Solr. The SOLR_NODE_HOSTPORT monitor property wins
 * when set. Otherwise the endpoint is read from the system properties of Solr's JVM, over the same JMX connection
 * used for JVM metrics, so a Solr on any port needs no extra setup.
 */
public final class SolrEndpoint {
  private static final Log LOG = LogFactory.getLog(SolrEndpoint.class);

  public static final String HOSTPORT_PROPERTY = "SPM_MONITOR_SOLR_NODE_HOSTPORT";
  public static final String USERNAME_PROPERTY = "SPM_MONITOR_SOLR_NODE_BASICAUTH_USERNAME";
  public static final String PASSWORD_PROPERTY = "SPM_MONITOR_SOLR_NODE_BASICAUTH_PASSWORD";

  private SolrEndpoint() {
  }

  /**
   * Returns the config content with the endpoint filled in, or null when the endpoint can't be found.
   */
  public static String fillPlaceholders(String configContent, MonitorConfig monitorConfig) {
    String endpoint = resolve(MonitorUtil.loadMonitorProperties(monitorConfig.getMonitorPropertiesFile()),
                              monitorConfig.getMonitorPropertiesFile());
    return endpoint == null ? null : replacePlaceholders(configContent, endpoint);
  }

  static String replacePlaceholders(String configContent, String endpoint) {
    for (String variant : MonitorUtil.getPropertyVariants(HOSTPORT_PROPERTY)) {
      configContent = configContent.replace("${" + variant + "}", endpoint);
    }
    return configContent;
  }

  public static ServerInfo serverInfo(MonitorConfig monitorConfig) {
    File propertiesFile = monitorConfig.getMonitorPropertiesFile();
    Properties props = MonitorUtil.loadMonitorProperties(propertiesFile);
    String endpoint = resolve(props, propertiesFile);
    if (endpoint == null) {
      return null;
    }
    String username = property(props, USERNAME_PROPERTY);
    String password = property(props, PASSWORD_PROPERTY);
    if (username == null || password == null) {
      return serverInfo(endpoint, null, null);
    }
    return serverInfo(endpoint, username, password);
  }

  public static boolean isNeededBy(String configContent) {
    for (String variant : MonitorUtil.getPropertyVariants(HOSTPORT_PROPERTY)) {
      if (configContent.contains("${" + variant + "}")) {
        return true;
      }
    }
    return false;
  }

  private static String resolve(Properties props, File propertiesFile) {
    String configured = configured(props);
    if (configured != null) {
      return configured;
    }

    JmxServiceContext ctx = JmxServiceContext.getContext(propertiesFile);
    JmxMBeanServerConnectionWrapper wrapper = JmxMBeanServerConnectionWrapper.getInstance(ctx);
    if (wrapper == null || wrapper.getMbeanServerConnection() == null) {
      return null;
    }
    try {
      return fromSystemProperties(systemProperties(wrapper.getMbeanServerConnection()),
                                  ctx == null ? null : jmxHost(ctx.getUrl()));
    } catch (Throwable thr) {
      LOG.warn("Can't read Solr's system properties over JMX", thr);
      return null;
    }
  }

  static String configured(Properties props) {
    String value = property(props, HOSTPORT_PROPERTY);
    while (value != null && value.endsWith("/")) {
      value = value.substring(0, value.length() - 1);
    }
    return value == null || value.isEmpty() ? null : value;
  }

  /**
   * Builds the ServerInfo the same way JsonStatsExtractorConfig does for a yml 'server', so checks and collectors
   * share one data source.
   */
  static ServerInfo serverInfo(String endpoint, String username, String password) {
    if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
      endpoint = "http://" + endpoint;
    }
    String hostPort = endpoint.substring(endpoint.indexOf("//") + 2);
    hostPort = hostPort.indexOf('/') != -1 ? hostPort.substring(0, hostPort.indexOf('/')) : hostPort;
    String host = hostPort.indexOf(':') != -1 ? hostPort.substring(0, hostPort.indexOf(':')) : hostPort;
    String port = hostPort.indexOf(':') != -1 ? hostPort.substring(hostPort.indexOf(':') + 1).trim() : "80";
    return new ServerInfo(endpoint, host, port, username, password);
  }

  static String fromSystemProperties(Map<String, String> sysProps, String jmxHost) {
    String port = sysProps.get("solr.port.listen");
    if (port == null) {
      port = sysProps.get("jetty.port");
    }
    if (port == null || !port.trim().matches("[0-9]+")) {
      return null;
    }

    String host = host(sysProps.get("solr.host.bind"), jmxHost);
    if (host == null) {
      return null;
    }

    String command = sysProps.get("sun.java.command");
    String scheme = command != null && command.contains("--module=https") ? "https" : "http";
    return scheme + "://" + host + ":" + port.trim();
  }

  /**
   * A remote JMX host is what the agent was told to reach, so it wins. With local JMX the bind address decides; an
   * unset one means Jetty's loopback default.
   */
  private static String host(String bind, String jmxHost) {
    bind = bind == null ? "" : bind.trim();
    String host;
    if (jmxHost != null && !isLoopback(jmxHost)) {
      host = jmxHost;
    } else if (bind.isEmpty() || bind.equals("0.0.0.0") || bind.equals("::") || bind.equals("[::]")) {
      host = "localhost";
    } else {
      host = bind;
    }
    if (host.indexOf(':') != -1 && !host.startsWith("[")) {
      host = "[" + host + "]";
    }
    return host.matches("[A-Za-z0-9._\\-:\\[\\]%]+") ? host : null;
  }

  private static boolean isLoopback(String host) {
    return host.equals("localhost") || host.startsWith("127.") || host.equals("::1") || host.equals("[::1]");
  }

  static String jmxHost(String jmxUrl) {
    if (jmxUrl == null || jmxUrl.trim().isEmpty()) {
      return null;
    }
    String hostPort = jmxUrl.substring(jmxUrl.lastIndexOf("//") == -1 ? 0 : jmxUrl.lastIndexOf("//") + 2);
    if (hostPort.startsWith("[") && hostPort.indexOf(']') != -1) {
      return hostPort.substring(0, hostPort.indexOf(']') + 1);
    }
    int colon = hostPort.indexOf(':');
    return colon == -1 ? hostPort : hostPort.substring(0, colon);
  }

  private static Map<String, String> systemProperties(MBeanServerConnection conn) throws Exception {
    RuntimeMXBean runtime = ManagementFactory.newPlatformMXBeanProxy(conn, ManagementFactory.RUNTIME_MXBEAN_NAME,
                                                                     RuntimeMXBean.class);
    return runtime.getSystemProperties();
  }

  private static String property(Properties props, String name) {
    String value = MonitorUtil.getPropertyFromAnyNameVariant(props, name, null);
    if (value == null) {
      return null;
    }
    value = MonitorUtil.stripQuotes(value.trim()).trim();
    return value.isEmpty() ? null : value;
  }
}
