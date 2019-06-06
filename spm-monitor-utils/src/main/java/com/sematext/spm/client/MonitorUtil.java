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
package com.sematext.spm.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sematext.spm.client.config.CollectorFileConfig;
import com.sematext.spm.client.jmx.JmxServiceContext;
import com.sematext.spm.client.util.FileUtil;
import com.sematext.spm.client.util.IOUtils;

// TODO Very similar class existing in sender module (SenderUtil), consider unifying them
public final class MonitorUtil {
  public static final int SANITY_LOCK_OBTAIN_LIMIT = 1000;
  private static Log log;

  public static final AtomicBoolean MONITOR_RUNTIME_SETUP_JAVAAGENT = new AtomicBoolean(true);

  public static final String PATH_SEPARATOR = File.separator;
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private static final String TOKEN_EXTRACTOR_PATTERN_STRING = ".*config(-?[A-Za-z0-9]*)-([a-f0-9]{8}\\-[a-f0-9]{4}\\-[a-f0-9]{4}\\-[a-f0-9]{4}\\-[a-f0-9]{12}).*";
  private static final Pattern TOKEN_EXTRACTOR_PATTERN = Pattern.compile(TOKEN_EXTRACTOR_PATTERN_STRING);

  public static final String SPM_HOME =
      (System.getProperty("spm.home") != null && (new File(System.getProperty("spm.home"))).isDirectory()) ?
          System.getProperty("spm.home") : createPathString("opt", "spm");

  public static final String MONITOR_LOCK_FILE_DIR = FileUtil.path(SPM_HOME, "spm-monitor", "run");
  public static final String MONITOR_APPLICATIONS_LOGS_BASE = createPathString(SPM_HOME, "spm-monitor", "logs", "applications");

  public static Integer MONITOR_PROCESS_ORDINAL = null;

  private static final long WAIT_FOR_SPM_MONITOR_PROPERTIES_FILE_MS = 20 * 1000;

  public static final String NAME_OF_PROPERTY_WITH_HOSTNAME = "SPM_MONITOR_HOSTNAME_CONTAINING_PROPERTY_NAME";

  public static final String SPM_CONTAINER_NAME = "SPM_CONTAINER_NAME";

  public static final File DOCKER_SETUP_FILE = new File(SPM_HOME, ".docker");

  public static final File SPM_MONITOR_COLLECTORS_CONFIG_BASE_DIR = new File(createPathString(SPM_HOME, "spm-monitor", "collectors"));

  // TODO done this way for reuse by various components, refactor in future
  public static final Map<String, CollectorFileConfig> FILE_NAME_TO_LOADED_YAML_CONFIG = new HashMap<String, CollectorFileConfig>();

  public static String JAVA_VERSION;
  public static int JAVA_MAJOR_VERSION;

  private MonitorUtil() {
  }

  static {
    log = LogFactory.getLog(MonitorUtil.class);
  }

  static {
    try {
      JAVA_VERSION = System.getProperty("java.version");
      String majorJavaVersion = JAVA_VERSION.contains(".") ?
          JAVA_VERSION.substring(0, JAVA_VERSION.indexOf(".")) :
          JAVA_VERSION;
      JAVA_MAJOR_VERSION = Integer.parseInt(majorJavaVersion.trim());
    } catch (Throwable thr) {
      log.error("Error while resolving Java version, will assume version '8' is used");
      JAVA_VERSION = "8";
      JAVA_MAJOR_VERSION = 8;
    }
  }

  /**
   * Creates path string, adds path_separator at the beginning and at the end.
   *
   * @param pathElements
   * @return
   */
  public static String createPathString(String... pathElements) {
    StringBuilder sb = new StringBuilder();

    boolean addStartingSeparator = true;
    if (pathElements != null && pathElements.length > 0 && pathElements[0] != null) {
      if (pathElements[0].startsWith(".")) {
        addStartingSeparator = false;
      }
    }

    if (addStartingSeparator) {
      sb.append(PATH_SEPARATOR);
    }

    for (String p : pathElements) {
      if (p == null || p.trim().equals("")) {
        continue;
      }

      sb.append(p);
      sb.append(PATH_SEPARATOR);
    }

    return sb.toString();
  }

  /**
   * Does things like:
   * - removing duplicate '/' or '\' characters from the path
   *
   * @param path
   * @return
   */
  public static String normalizePath(String path) {
    if (PATH_SEPARATOR.equals("/")) {
      return path.replaceAll(PATH_SEPARATOR + "+", PATH_SEPARATOR);
    } else if (PATH_SEPARATOR.equals("\\")) {
      // windows a bit different regex
      return path.replaceAll("[\\\\]+", "\\\\");
    } else {
      throw new IllegalArgumentException("Unsupported path separator " + PATH_SEPARATOR);
    }
  }

  public static String getMonitorId(File monitorPropertiesFile) {
    String token = extractToken(monitorPropertiesFile.getAbsolutePath());
    String jvmName = extractJvmName(monitorPropertiesFile.getAbsolutePath(), token);
    String subType = extractConfSubtype(monitorPropertiesFile.getAbsolutePath());
    return token + ":" + jvmName + ":" + subType;
  }

  public static String extractToken(String configLocation) {
    Matcher m = TOKEN_EXTRACTOR_PATTERN.matcher(configLocation);
    if (!m.find()) {
      throw new IllegalArgumentException("Token could not be extracted from: " + configLocation +
                                             ". Pattern used: " + TOKEN_EXTRACTOR_PATTERN_STRING);
    }

    return m.group(2);
  }

  public static String extractJvmName(String configLocation, String token) {
    String tmp = configLocation.substring(configLocation.indexOf(token) + token.length() + 1);
    int indexOfExtension = tmp.lastIndexOf(".");
    if (indexOfExtension != -1) {
      return tmp.substring(0, indexOfExtension);
    } else {
      return tmp;
    }
  }

  public static String extractConfSubtype(String configLocation) {
    if (configLocation.lastIndexOf(PATH_SEPARATOR) != -1) {
      configLocation = configLocation.substring(configLocation.lastIndexOf(PATH_SEPARATOR) + 1);
    }
    configLocation = configLocation.substring("spm-monitor-".length());

    if (configLocation.startsWith("config")) {
      return "";
    } else {
      return configLocation.substring(0, configLocation.indexOf("-config"));
    }
  }

  public static String getMonitorTmpDirPath() {
    return FileUtil.path(SPM_HOME, "spm-monitor", "tmp");
  }

  public static String getMonitorCommonJarDirPath() {
    return FileUtil.path(SPM_HOME, "spm-monitor", "lib", "internal", "common");
  }

  public static String getMonitorPropertiesFileName(String token, String jvmName, String confSubtype) {
    return getMonitorConfigsBaseName(token, jvmName, confSubtype) + ".properties";
  }

  public static String getMonitorConfigsBaseName(String token, String jvmName, String confSubtype) {
    return getMonitorConfigsBaseName(null, token, jvmName, confSubtype);
  }

  public static String getMonitorConfigsBaseName(String monitorType, String token, String jvmName, String confSubtype) {
    StringBuilder str = new StringBuilder();

    str.append("spm-monitor-");

    if (confSubtype != null && !confSubtype.trim().equals("")) {
      str.append(confSubtype);

      if (!confSubtype.endsWith("-")) {
        str.append("-");
      }
    }

    str.append("config-");

    if (monitorType != null && !monitorType.trim().equals("")) {
      str.append(monitorType).append("-");
    }

    str.append(token).append("-").append(jvmName);

    return str.toString();
  }

  public static String getMonitorLogDirPath(String monitorType, String token, String jvmName, String confSubtype) {
    return normalizePath(createPathString(MONITOR_APPLICATIONS_LOGS_BASE, monitorType, token, jvmName, confSubtype));
  }

  public static void createMonitorLogDirAllSubpaths(String monitorType, String token, String jvmName,
                                                    String confSubtype) {
    List<String> l = new ArrayList<String>();
    l.add(MONITOR_APPLICATIONS_LOGS_BASE);
    l.add(normalizePath(createPathString(MONITOR_APPLICATIONS_LOGS_BASE, monitorType)));
    l.add(normalizePath(createPathString(MONITOR_APPLICATIONS_LOGS_BASE, monitorType, token)));
    l.add(normalizePath(createPathString(MONITOR_APPLICATIONS_LOGS_BASE, monitorType, token, jvmName)));
    l.add(normalizePath(createPathString(MONITOR_APPLICATIONS_LOGS_BASE, monitorType, token, jvmName, confSubtype)));

    for (String d : l) {
      File dir = new File(d);
      if (!dir.exists()) {
        dir.setReadable(true, false);
        dir.setWritable(true, false);
        dir.mkdirs();
        MonitorUtil.adjustPermissions(d, "777");
      }
    }
  }

  public static String getMonitorStatsLogFileName(Integer processOrdinal, DataFormat format) {
    if (processOrdinal == null) {
      return "spm-monitor-stats." + format.getFileExtension();
    }
    return String.format("spm-monitor-stats-%s.%s", processOrdinal, format.getFileExtension());
  }

  public static File fetchSpmMonitorPropertiesFileObject(String token, String jvmName, String confSubtype) {
    File propsFile = new File(FileUtil
                                  .path(SPM_HOME, "spm-monitor", "conf", getMonitorPropertiesFileName(token, jvmName, confSubtype)));
    long beforeLoadTime = System.currentTimeMillis();

    while (!propsFile.exists() && (beforeLoadTime + WAIT_FOR_SPM_MONITOR_PROPERTIES_FILE_MS < System
        .currentTimeMillis())) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        // just continue with the logic
      }
    }

    return propsFile;
  }

  private static FileLock obtainMonitorLock(String token, String jvmName, String confSubtype, Integer processOrdinal)
      throws ConfigurationFailedException {
    File lockFileObject = getLockFileObject(token, jvmName, confSubtype, processOrdinal);
    log.info("Obtaining lock on file: " + lockFileObject);

    FileLock lock;
    try {
      RandomAccessFile lockFile = getLockFile(lockFileObject);
      lock = lockFile.getChannel().tryLock();

      if (lock == null) {
        log.info("Lock on file: " + lockFileObject + " is in use.");
        return null;
      }

      adjustPermissions(lockFileObject.getAbsolutePath(), "777");
    } catch (IOException ioe) {
      log.error("Error while obtaining a lock to a file " + lockFileObject.getAbsolutePath());
      throw new ConfigurationFailedException(
          "Error while obtaining a lock to a file " + lockFileObject.getAbsolutePath(), ioe);
    }

    log.info("Lock obtained on file: " + lockFileObject);
    return lock;
  }

  public static synchronized Integer obtainMonitorLock(String token, String jvmName, String confSubtype)
      throws ConfigurationFailedException {
    if (MONITOR_PROCESS_ORDINAL != null) {
      return MONITOR_PROCESS_ORDINAL;
    } else {
      for (int i = 0; i < SANITY_LOCK_OBTAIN_LIMIT; i++) {
        FileLock fileLock = obtainMonitorLock(token, jvmName, confSubtype, i);
        if (fileLock != null) {
          MONITOR_PROCESS_ORDINAL = i;
          return i;
        }
      }
      throw new ConfigurationFailedException("Can't obtain lock, tried " + SANITY_LOCK_OBTAIN_LIMIT + " times.");
    }
  }

  private static File getLockFileObject(String token, String jvmName, String confSubtype, Integer processOrdinal) {
    return new File(MonitorUtil.MONITOR_LOCK_FILE_DIR,
                    token + "-" + confSubtype + "-" + jvmName + "-monitor-" + processOrdinal + ".lock");
  }

  private static RandomAccessFile getLockFile(File lockFileObject) throws FileNotFoundException {
    return new RandomAccessFile(lockFileObject, "rw");
  }

  public static void checkRuntimeSetup(String token, String jvmName, String confSubtype)
      throws ConfigurationFailedException {
    File propsFile = fetchSpmMonitorPropertiesFileObject(token, jvmName, confSubtype);

    if (!propsFile.exists()) {
      String msg = "This monitor's properties file (" + propsFile + ") doesn't exist yet, please create it first!";
      log.error(msg);
      throw new ConfigurationFailedException(msg);
    } else {
      Properties props = new Properties();
      try {
        props.load(new FileInputStream(propsFile));

        String inProcess = props.getProperty("SPM_MONITOR_IN_PROCESS");
        // remove quotes if they exist
        if (inProcess != null) {
          inProcess = inProcess.replace("\"", "");
        }

        if (inProcess == null ||
            (!"true".equalsIgnoreCase(inProcess) && !"false".equalsIgnoreCase(inProcess))) {
          String msg =
              "Property SPM_MONITOR_IN_PROCESS has to be populated with either 'true' or 'false' in " + propsFile +
                  ". Value was : " + inProcess;
          log.error(msg);
          throw new ConfigurationFailedException(msg);
        }

        boolean inProcessBool = "true".equalsIgnoreCase(inProcess);

        String errMsg = null;

        if (inProcessBool && !MONITOR_RUNTIME_SETUP_JAVAAGENT.get()) {
          errMsg = "In-process monitor (defined by SPM_MONITOR_IN_PROCESS property in " + propsFile +
              ") can't be started with standalone SPM monitor";
        } else if (!inProcessBool && MONITOR_RUNTIME_SETUP_JAVAAGENT.get()) {
          errMsg = "Standalone monitor (defined by SPM_MONITOR_IN_PROCESS property in " + propsFile +
              ") can't be started with javaagent based SPM monitor";
        }

        if (errMsg != null) {
          log.error(errMsg);
          throw new ConfigurationFailedException(errMsg);
        }
      } catch (IOException e) {
        String msg = "This monitor's properties file (" + propsFile + ") can't be read!";
        log.error(msg, e);
        throw new ConfigurationFailedException(msg, e);
      }
    }
  }

  public static void adjustPermissions(String logPath, String permissions) {
    try {
      Runtime.getRuntime().exec("chmod -R " + permissions + " " + logPath);
    } catch (IOException e) {
      log.error("Error while adjusting permissions on : " + logPath + ", error: " + e.getMessage());
      log.printStackTrace(e);
      log.error("Error while executing '" + "chmod -R " + permissions + " " + logPath + "'", e);
      // just ignore the error if it happens - some OS will not support this and it is not crucial (only important when
      // switching between users who are running the monitor)
    }
  }

  public static String findProcessOwner() {
    return cleanUsername(System.getProperty("user.name"));
  }

  public static String findProcessOwnerAlternative() {
    try {
      Process p = Runtime.getRuntime().exec("whoami");
      String res = IOUtils.toString(p.getInputStream());
      return cleanUsername(res);
    } catch (Throwable thr) {
      log.debug("Can't find owner alternative", thr);
      return null;
    }
  }

  private static String cleanUsername(String username) {
    if (username != null) {
      return username.trim().replaceAll("[^A-Za-z0-9]", "_");
    }
    return null;
  }

  public static MonitorArgs extractMonitorArgs(String agentArgs) {
    String token;
    String confSubtype = "";
    String jvmName;
    String confPath = null;

    if (agentArgs.endsWith(".xml")) {
      confPath = agentArgs;
      token = MonitorUtil.extractToken(confPath);
      jvmName = MonitorUtil.extractJvmName(confPath, token);
      confSubtype = MonitorUtil.extractConfSubtype(confPath);
    } else if (agentArgs.endsWith(".properties")) {
      String tmp = agentArgs.substring(agentArgs.lastIndexOf("spm-monitor-") + "spm-monitor-".length());
      tmp = tmp.substring(0, tmp.indexOf(".properties"));

      confSubtype = "";

      if (!tmp.startsWith("config-")) {
        confSubtype = tmp.substring(0, tmp.indexOf("-config-"));
        tmp = tmp.substring(tmp.indexOf("config-"));
      }

      tmp = tmp.substring("config-".length());

      token = tmp.substring(0, 36);
      jvmName = tmp.substring(tmp.lastIndexOf("-") + 1);

    } else {
      // we assume the format will be token:conf_subtype:jvm_name, where conf_subtype shouldn't contain ":"
      // conf subtype will in most cases be empty string, except in cases where one monitor may install
      // more than one monitor conf file to monitor more processes at the same time (for instance, hbase, one for
      // master, the other for region server)
      if (agentArgs.contains(":")) {
        token = agentArgs.substring(0, agentArgs.indexOf(":"));
        String rest = agentArgs.substring(agentArgs.indexOf(":") + 1);
        confSubtype = rest.substring(0, rest.indexOf(":"));
        jvmName = rest.substring(rest.indexOf(":") + 1);
      } else {
        throw new IllegalArgumentException("Cannot parse agentArgument " + agentArgs);
      }
    }

    return new MonitorArgs(token, jvmName, confSubtype);
  }

  public static String stripQuotesIfEnclosed(String value) {
    if (value == null) {
      return null;
    }

    String tmpValue = value.trim();
    if (tmpValue.startsWith("\"") && tmpValue.endsWith("\"")) {
      value = tmpValue.substring(1, tmpValue.length() - 1);
      // no trimming in case quotes were present
      return value;
    }

    return value.trim();
  }

  public static String stripQuotes(String value) {
    if (value == null) {
      return null;
    }

    value = value.trim();

    if (value.startsWith("\"")) {
      value = value.substring(1);
    }
    if (value.endsWith("\"")) {
      value = value.substring(0, value.length() - 1);
    }

    return value;
  }

  public static class MonitorArgs {
    private String token;
    private String jvmName;
    private String subType;

    public MonitorArgs(String token, String jvmName, String subType) {
      this.token = token;
      this.jvmName = jvmName;
      this.subType = subType;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public String getJvmName() {
      return jvmName;
    }

    public void setJvmName(String jvmName) {
      this.jvmName = jvmName;
    }

    public String getSubType() {
      return subType;
    }

    public void setSubType(String subType) {
      this.subType = subType;
    }

  }

  // pre-defined list of all possible property names which could contain the hostname
  private static final List<String> HOSTNAME_CONTAINING_PROPERTIES_NAMES = Arrays.asList(
      "SPM_MONITOR_ES_NODE_HOSTPORT",
      "SPM_MONITOR_MYSQL_DB_HOST_PORT",
      "REDIS_HOST",
      "SPM_MONITOR_HAPROXY_STATS_URL",
      "SPM_MONITOR_NGINX_PLUS_STATUS_URL"

      // following properties should not be used, they may not to point to the host being monitored
      // "SPARK_API_HOST",
      // "NIMBUS_HOST"
  );

  private static long hostnameReadIntervalMs = 30 * 1000l;

  private static long lastContainerHostnameCalculationTime = -1l;
  private static String lastContainerHostname = null;

  public static String getContainerHostname(File monitorPropertiesFile, boolean container) throws FileNotFoundException, IOException {
    long currentTime = System.currentTimeMillis();
    if (lastContainerHostnameCalculationTime != -1
        && (lastContainerHostnameCalculationTime + hostnameReadIntervalMs) > currentTime) {
      return lastContainerHostname;
    }

    Properties monitorProperties = new Properties();
    monitorProperties.load(new FileInputStream(monitorPropertiesFile));
    return getContainerHostname(monitorPropertiesFile, monitorProperties, container);
  }

  public static String getContainerHostname(File monitorPropertiesFile, Properties monitorProperties, boolean container) {
    long currentTime = System.currentTimeMillis();
    if (lastContainerHostnameCalculationTime != -1
        && (lastContainerHostnameCalculationTime + hostnameReadIntervalMs) > currentTime) {
      return lastContainerHostname;
    }

    lastContainerHostnameCalculationTime = currentTime;

    if (!DOCKER_SETUP_FILE.exists() && !container) {
      lastContainerHostname = null;
      return lastContainerHostname;
    }

    if (MonitorUtil.MONITOR_RUNTIME_SETUP_JAVAAGENT.get()) {
      // if in-process, just use plain java to resolve
      lastContainerHostname = resolveHostnameInJava();
      return lastContainerHostname;
    } else {
      if (monitorProperties != null) {
        String containerHostname = monitorProperties.getProperty(SPM_CONTAINER_NAME);
        if (containerHostname != null && !containerHostname.trim().equals("")) {
          lastContainerHostname = containerHostname;
          return lastContainerHostname;
        }
      }

      JmxServiceContext jmxCtx = JmxServiceContext.getContext(monitorPropertiesFile);

      // otherwise we have to look into various configuration properties to find the hostname (by looking at what agent actually attaches)
      if (jmxCtx != null && jmxCtx.isRemote() && jmxCtx.getUrl() != null && !jmxCtx.getUrl().trim().equals("")) {
        lastContainerHostname = extractHostFromUrl(jmxCtx.getUrl());
        return lastContainerHostname;
      } else {
        if (monitorProperties != null) {
          // handle app types like ES, MySQL, Redis, Nginx+...
          String hostnameProperty = monitorProperties.getProperty(MonitorUtil.NAME_OF_PROPERTY_WITH_HOSTNAME);
          if (hostnameProperty != null) {
            hostnameProperty = hostnameProperty.trim();
            if (!HOSTNAME_CONTAINING_PROPERTIES_NAMES.contains(hostnameProperty)) {
              HOSTNAME_CONTAINING_PROPERTIES_NAMES.add(0, hostnameProperty);
            }
          }

          // now go through all such properties and try to find one which contains the hostname
          for (String propName : HOSTNAME_CONTAINING_PROPERTIES_NAMES) {
            String propVal = monitorProperties.getProperty(propName);
            if (propVal != null && !propVal.trim().equals("")) {
              propVal = propVal.trim();
              String hostname = extractHostFromUrl(propVal);

              if (hostname != null && !hostname.trim().equals("")) {
                // special logic for REDIS_HOST/PORT and possible "localhost" value
                if (hostname.equalsIgnoreCase("localhost") && propName.equals("REDIS_HOST")) {
                  String port = monitorProperties.getProperty("REDIS_PORT", null);
                  if (port != null && !port.trim().equals("")) {
                    hostname = hostname + ":" + port;
                  }
                }

                lastContainerHostname = hostname;
                return lastContainerHostname;
              }
            }
          }

          // look for properties ending with _HOST_PORT
          for(String name : monitorProperties.stringPropertyNames()) {
            if (name.endsWith("_HOST_PORT")) {
              String value = monitorProperties.getProperty(name);
              String hostname = extractHostFromUrl(value);
              if (hostname != null && hostname.length() > 0) {
                lastContainerHostname = hostname;
                return lastContainerHostname;
              }
            }
          }
        }

        // if we got here without returning a hostname, print an error to log, return "unknown"
        log.error("Couldn't resolve the hostname based on monitor properties file, " +
                      "properties were: " + monitorProperties);
        lastContainerHostname = null;
        return lastContainerHostname;
      }
    }
  }

  public static String extractHostFromUrl(String url) {
    url = url.trim();

    if (url.equals("")) {
      return null;
    }

    while (url.startsWith("\"")) {
      url = url.substring(1);
    }
    while (url.endsWith("\"")) {
      url = url.substring(0, url.length() - 1);
    }

    if (url.startsWith("http://") || url.startsWith("https://")) {
      url = url.substring(0, url.indexOf("/"));
      url = url.substring(1);
    }

    if (url.toLowerCase().startsWith("localhost")) {
      // include port
      int indexOfLimit = url.indexOf("/");
      if (indexOfLimit != -1) {
        url = url.substring(0, indexOfLimit);
      }
      return url;
    } else {
      int indexOfLimit = url.indexOf(":");
      if (indexOfLimit != -1) {
        url = url.substring(0, indexOfLimit);
      } else {
        indexOfLimit = url.indexOf("/");
        if (indexOfLimit != -1) {
          url = url.substring(0, indexOfLimit);
        }
      }
      return url;
    }
  }

  public static String resolveHostnameInJava() {
    try {
      InetAddress addr = InetAddress.getLocalHost();
      return addr.getHostName();
    } catch (UnknownHostException e) {
      log.error("Can't resolve the hostname, using 'unknown'. Restart SPM monitor after fixing machine's hostname configuration", e);
      return "unknown";
    }
  }
}
