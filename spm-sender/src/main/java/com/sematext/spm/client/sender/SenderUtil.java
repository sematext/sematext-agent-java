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
package com.sematext.spm.client.sender;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorUtil;

public final class SenderUtil {
  private static final Log LOG = LogFactory.getLog(SenderUtil.class);

  public static final String PATH_SEPARATOR = File.separator;

  public static final Properties INSTALLATION_PROPERTIES = new Properties();

  public static final String SPM_HOME =
      (System.getProperty("spm.home") != null && (new File(System.getProperty("spm.home"))).isDirectory()) ?
          System.getProperty("spm.home") : createPathString("opt", "spm");

  public static final String SPM_SETUP_PROPERTIES_DIR = createPathString(SPM_HOME, "properties");

  // TODO - read from some internal configuration; used only when for some reason app configs don't have this property
  public static final String DEFAULT_SAAS_PROD_RECEIVER_URL = "http://spm-receiver.sematext.com";
  public static final String DEFAULT_SAAS_PROD_RECEIVER_METRICS_ENDPOINT = "/write?db=metrics";
  public static final String DEFAULT_SAAS_PROD_RECEIVER_TAGS_ENDPOINT = "/write?db=tags";
  public static final String DEFAULT_SAAS_PROD_RECEIVER_METAINFO_ENDPOINT = "/write?db=metainfo";

  public static final File DATA_SENDER_PROPERTIES_FILE = new File(SPM_SETUP_PROPERTIES_DIR, "agent.properties");

  public static final AtomicLong INSTALLATION_PROPERTIES_FILE_LAST_MODIFIED_TIME = new AtomicLong(-1);

  private static final String HOSTNAME_ALIAS_PROPERTY_NAME = "hostname_alias";

  public static final File DOCKER_SETUP_FILE = new File(SPM_HOME, ".docker");

  private static final String DOCKER_HOSTNAME_PROPERTY_NAME = "docker_hostname";

  static {
    loadInstallationProperties();
  }

  public static void loadInstallationProperties() {
    // use agent.properties as a source of proxy properties and receiver_url property

    File[] propsFiles = findSpmSetupPropertiesFiles();

    File proxyPropsFile = null;
    File freshestAppPropsFile = null;

    for (File propsFile : propsFiles) {
      if (propsFile.getName().equalsIgnoreCase("agent.properties")) {
        proxyPropsFile = propsFile;
      }
      if (propsFile.getName().startsWith("spm-setup") && propsFile.getName().endsWith(".properties")) {
        if (freshestAppPropsFile == null) {
          freshestAppPropsFile = propsFile;
        }
      }
    }

    try {
      if (proxyPropsFile != null) {
        INSTALLATION_PROPERTIES.load(new FileInputStream(proxyPropsFile));
        INSTALLATION_PROPERTIES_FILE_LAST_MODIFIED_TIME.set(proxyPropsFile.lastModified());
      }

      if (proxyPropsFile == null && freshestAppPropsFile != null) {
        Properties tmpProps = new Properties();
        tmpProps.load(new FileInputStream(freshestAppPropsFile));
        INSTALLATION_PROPERTIES_FILE_LAST_MODIFIED_TIME.set(freshestAppPropsFile.lastModified());

        // if no proxy props file, read proxy settings from the freshest spm-setup properties file
        INSTALLATION_PROPERTIES.setProperty("server_base_url", tmpProps.getProperty("server_base_url"));
        INSTALLATION_PROPERTIES.setProperty("metrics_endpoint", tmpProps.getProperty("metrics_endpoint"));
        INSTALLATION_PROPERTIES.setProperty("tags_endpoint", tmpProps.getProperty("tags_endpoint"));
        INSTALLATION_PROPERTIES.setProperty("metainfo_endpoint", tmpProps.getProperty("metainfo_endpoint"));

        INSTALLATION_PROPERTIES.setProperty("proxy_host", tmpProps.getProperty("proxy_host"));
        INSTALLATION_PROPERTIES.setProperty("proxy_port", tmpProps.getProperty("proxy_port"));
        INSTALLATION_PROPERTIES
            .setProperty("proxy_user_name", tmpProps.getProperty("proxy_user_name"));
        INSTALLATION_PROPERTIES
            .setProperty("proxy_password", tmpProps.getProperty("proxy_password"));
      }
    } catch (Throwable thr) {
      LOG.error("Error while reading properties files!", thr);
      throw new IllegalStateException("Error while reading properties files!", thr);
    }

    if (INSTALLATION_PROPERTIES.get("server_base_url") == null) {
      INSTALLATION_PROPERTIES.setProperty("server_base_url", DEFAULT_SAAS_PROD_RECEIVER_URL);
    }
    if (INSTALLATION_PROPERTIES.get("metrics_endpoint") == null) {
      INSTALLATION_PROPERTIES.setProperty("metrics_endpoint", DEFAULT_SAAS_PROD_RECEIVER_METRICS_ENDPOINT);
    }
    if (INSTALLATION_PROPERTIES.get("tags_endpoint") == null) {
      INSTALLATION_PROPERTIES.setProperty("tags_endpoint", DEFAULT_SAAS_PROD_RECEIVER_TAGS_ENDPOINT);
    }
    if (INSTALLATION_PROPERTIES.get("metainfo_endpoint") == null) {
      INSTALLATION_PROPERTIES.setProperty("metainfo_endpoint", DEFAULT_SAAS_PROD_RECEIVER_METAINFO_ENDPOINT);
    }
  }

  /**
   * @return properties files ordered by last modified date in descending order
   */
  @SuppressWarnings("unchecked")
  public static File[] findSpmSetupPropertiesFiles() {
    File[] propsFiles = new File(SPM_SETUP_PROPERTIES_DIR).listFiles();

    if (propsFiles != null) {
      Arrays.sort(propsFiles, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
    }

    return propsFiles;
  }

  private SenderUtil() {
  }

  /**
   * Creates path string, adds path_separator at the beginning and at the end.
   *
   * @param pathElements
   * @return
   */
  public static String createPathString(String... pathElements) {
    StringBuilder sb = new StringBuilder();
    sb.append(PATH_SEPARATOR);

    for (String p : pathElements) {
      sb.append(p);
      sb.append(PATH_SEPARATOR);
    }

    return sb.toString();
  }

  public synchronized static String getDockerHostname() {
    long currentTime = System.currentTimeMillis();
    if (lastDockerHostCalculationTime != -1 && (lastDockerHostCalculationTime + hostnameReadIntervalMs) > currentTime) {
      return lastDockerHostname;
    }

    lastDockerHostCalculationTime = currentTime;

    if (!DOCKER_SETUP_FILE.exists()) {
      lastDockerHostname = null;
      return lastDockerHostname;
    }

    try {
      Properties tmpProps = new Properties();
      tmpProps.load(new FileInputStream(DOCKER_SETUP_FILE));
      String hostname = tmpProps.getProperty(DOCKER_HOSTNAME_PROPERTY_NAME);

      if (hostname != null) {
        hostname = hostname.trim();
      }
      if ("".equals(hostname)) {
        hostname = null;
      }

      lastDockerHostname = hostname;

      return lastDockerHostname;
    } catch (Throwable thr) {
      // return null in case of problem here
      LOG.error("Error while reading " + DOCKER_SETUP_FILE, thr);
      lastDockerHostname = null;
      return lastDockerHostname;
    }
  }

  private static long hostnameReadIntervalMs = 30 * 1000l;

  private static long lastDockerHostCalculationTime = -1l;
  private static String lastDockerHostname = null;

  private static long lastHostCalculationTime = -1l;
  private static String lastHostname = "unknown";

  public synchronized static String calculateHostParameterValue(File monitorPropertiesFile)
      throws FileNotFoundException, IOException {
    long currentTime = System.currentTimeMillis();
    if (lastHostCalculationTime != -1 && (lastHostCalculationTime + hostnameReadIntervalMs) > currentTime) {
      return lastHostname;
    }

    Properties monitorProperties = new Properties();
    monitorProperties.load(new FileInputStream(monitorPropertiesFile));
    return calculateHostParameterValue(monitorPropertiesFile, monitorProperties);
  }

  public synchronized static String calculateHostParameterValue(File monitorPropertiesFile,
                                                                Properties monitorProperties) {
    long currentTime = System.currentTimeMillis();
    if (lastHostCalculationTime != -1 && (lastHostCalculationTime + hostnameReadIntervalMs) > currentTime) {
      return lastHostname;
    }

    lastHostCalculationTime = currentTime;

    if (DOCKER_SETUP_FILE.exists()) {
      String containerHostname = MonitorUtil.getContainerHostname(monitorPropertiesFile, monitorProperties);
      if (containerHostname != null && !containerHostname.trim().equals("")) {
        LOG.info("Resolved hostname to " + containerHostname + " based on calculated container hostname");
        lastHostname = containerHostname;
        return containerHostname;
      } else {
        LOG.warn("Couldn't resolve container hostname, returning value 'unknown'");
        lastHostname = "unknown";
        return lastHostname;
      }
    } else {
      String hostnameAlias = getHostnameAlias();
      if (hostnameAlias != null && !hostnameAlias.trim().equals("")) {
        LOG.info("Resolved hostname to " + hostnameAlias + " based on alias property");
        lastHostname = hostnameAlias;
        return lastHostname;
      }

      // otherwise, resolve the hostname
      String hostname = MonitorUtil.resolveHostnameInJava();

      if (hostname == null) {
        hostname = "unknown";
      }

      lastHostname = hostname;

      LOG.info("Resolved hostname to " + hostname);

      return lastHostname;
    }
  }

  private static String getHostnameAlias() {
    if (DATA_SENDER_PROPERTIES_FILE.exists()) {
      try {
        Properties tmpProps = new Properties();
        tmpProps.load(new FileInputStream(DATA_SENDER_PROPERTIES_FILE));
        return tmpProps.getProperty(HOSTNAME_ALIAS_PROPERTY_NAME);
      } catch (Throwable thr) {
        // return null in case of problem here
        LOG.error("Error while reading " + DATA_SENDER_PROPERTIES_FILE, thr);
        return null;
      }
    }

    return null;
  }
}
