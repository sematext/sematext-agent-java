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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
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
  public static final String DEFAULT_SAAS_PROD_RECEIVER_TAG_ALIASES_ENDPOINT = "/write?db=tagAliases";
  public static final String DEFAULT_SAAS_PROD_RECEIVER_METAINFO_ENDPOINT = "/write?db=metainfo";

  public static final File DATA_SENDER_PROPERTIES_FILE = new File(SPM_SETUP_PROPERTIES_DIR, "agent.properties");

  public static final AtomicLong INSTALLATION_PROPERTIES_FILE_LAST_MODIFIED_TIME = new AtomicLong(-1);

  private static final String HOSTNAME_ALIAS_PROPERTY_NAME = "hostname_alias";

  public static final File DOCKER_SETUP_FILE = new File(SPM_HOME, ".docker");

  private static final String DOCKER_HOSTNAME_PROPERTY_NAME = "docker_hostname";

  private static final File RESOLVED_HOSTNAME_FILE = new File(SPM_HOME, ".resolved-hostname");

  private static final String CONTAINER_HOST_HOSTNAME_ENV_NAME = "SEMATEXT_CONTAINER_HOST_HOSTNAME";
  private static final String CONTAINER_NAME_ENV_NAME = "SEMATEXT_CONTAINER_NAME";
  private static final String CONTAINER_ID_ENV_NAME = "SEMATEXT_CONTAINER_ID";
  private static final String CONTAINER_IMAGE_ENV_NAME = "SEMATEXT_CONTAINER_IMAGE";
  private static String containerHostHostName, containerName, containerId, containerImage;

  private static final String K8S_POD_ENV_NAME = "SEMATEXT_K8S_POD_NAME";
  private static final String K8S_NAMESPACE_ENV_NAME = "SEMATEXT_K8S_NAMESPACE";
  private static final String K8S_CLUSTER_ENV_NAME = "SEMATEXT_K8S_CLUSTER";
  private static String k8sPodName, k8sNamespace, k8sCluster;

  private static boolean inContainer = false;
  private static boolean inKubernetes = false;

  static {
    loadInstallationProperties();
    loadContainerProperties();
    loadKubernetesProperties();
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
        INSTALLATION_PROPERTIES.setProperty("tag_aliases_endpoint", tmpProps.getProperty("tag_aliases_endpoint"));
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
    if (INSTALLATION_PROPERTIES.get("tag_aliases_endpoint") == null) {
      INSTALLATION_PROPERTIES.setProperty("tag_aliases_endpoint", DEFAULT_SAAS_PROD_RECEIVER_TAG_ALIASES_ENDPOINT);
    }
    if (INSTALLATION_PROPERTIES.get("metainfo_endpoint") == null) {
      INSTALLATION_PROPERTIES.setProperty("metainfo_endpoint", DEFAULT_SAAS_PROD_RECEIVER_METAINFO_ENDPOINT);
    }
  }

  private static void loadContainerProperties() {
    containerHostHostName = System.getenv(CONTAINER_HOST_HOSTNAME_ENV_NAME);
    containerName = System.getenv(CONTAINER_NAME_ENV_NAME);
    containerImage = System.getenv(CONTAINER_IMAGE_ENV_NAME);
    containerId = System.getenv(CONTAINER_ID_ENV_NAME);
    if (containerHostHostName == null &&
        containerName == null &&
        containerId == null &&
        containerImage == null) {
      // not container setup
    } else {
      checkEnvForNull(CONTAINER_HOST_HOSTNAME_ENV_NAME, containerHostHostName);
      checkEnvForNull(CONTAINER_NAME_ENV_NAME, containerName);
      checkEnvForNull(CONTAINER_ID_ENV_NAME, containerId);
      checkEnvForNull(CONTAINER_IMAGE_ENV_NAME, containerImage);
      inContainer = true;
    }
  }

  private static void loadKubernetesProperties() {
    k8sPodName = System.getenv(K8S_POD_ENV_NAME);
    k8sNamespace = System.getenv(K8S_NAMESPACE_ENV_NAME);
    k8sCluster = System.getenv(K8S_CLUSTER_ENV_NAME);
    if (k8sPodName == null &&
        k8sNamespace == null &&
        k8sCluster == null) {
      // not k8s setup
    } else {
      checkEnvForNull(K8S_POD_ENV_NAME, k8sPodName);
      checkEnvForNull(K8S_NAMESPACE_ENV_NAME, k8sNamespace);
      checkEnvForNull(K8S_CLUSTER_ENV_NAME, k8sCluster);
      inKubernetes = true;
    }
  }

  private static void checkEnvForNull(String name, String value) {
    if (value == null) {
      throw new IllegalArgumentException(String.format("Agent seems to be running in container/kubernetes, but %s is not set", name));
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

    if (containerHostHostName != null) {
      return containerHostHostName;
    }

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


  public synchronized static String calculateHostParameterValue() {
    long currentTime = System.currentTimeMillis();
    if (lastHostCalculationTime != -1 && (lastHostCalculationTime + hostnameReadIntervalMs) > currentTime) {
      return lastHostname;
    }

    lastHostCalculationTime = currentTime;

    if (containerHostHostName != null) {
      lastHostname = containerHostHostName;
      return lastHostname;
    } else if (DOCKER_SETUP_FILE.exists()) {
      String containerHostHostnameFromDockerFile = getDockerHostname();
      if (containerHostHostnameFromDockerFile != null && !containerHostHostnameFromDockerFile.trim().equals("")) {
        LOG.info("Resolved hostname to " + containerHostHostnameFromDockerFile + " based on calculated container host hostname");
        lastHostname = containerHostHostnameFromDockerFile;
        return containerHostHostnameFromDockerFile;
      } else {
        LOG.warn("Couldn't resolve container host hostname, returning value 'unknown'");
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

      String externalyResolvedHostname = readExternallyResolvedHostname();
      if (externalyResolvedHostname != null && !externalyResolvedHostname.trim().equals("")) {
        LOG.info("Resolved hostname to " + externalyResolvedHostname + " based on " + RESOLVED_HOSTNAME_FILE);
        lastHostname = externalyResolvedHostname;
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

  private static String readExternallyResolvedHostname() {
    try {
      if (RESOLVED_HOSTNAME_FILE.exists()) {
        List lines = FileUtils.readLines(RESOLVED_HOSTNAME_FILE);
        if (lines.size() > 0) {
          return (String) lines.get(0);
        }
      }      
    } catch (Throwable thr) {
      LOG.error("Error while reading from " + RESOLVED_HOSTNAME_FILE);
    }

    return null;
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

  public static String getContainerHostHostName() {
    return containerHostHostName;
  }

  public static String getContainerName() {
    return containerName;
  }

  public static String getContainerId() {
    return containerId;
  }

  public static String getContainerImage() {
    return containerImage;
  }

  public static boolean isInContainer() {
    return inContainer;
  }

  public static String getK8sPodName() {
    return k8sPodName;
  }

  public static String getK8sNamespace() {
    return k8sNamespace;
  }

  public static String getK8sCluster() {
    return k8sCluster;
  }

  public static boolean isInKubernetes() {
    return inKubernetes;
  }
}
