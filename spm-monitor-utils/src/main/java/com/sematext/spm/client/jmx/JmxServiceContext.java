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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.util.FileUtil;

/**
 * Describes monitored service. In case JMX should not be read in in-process mode, remote will be set to true and
 * properties like url, username, password... will be populated.
 */
public class JmxServiceContext {
  private static final Log LOG = LogFactory.getLog(JmxServiceContext.class);

  // URL should be in format "localhost:3000"
  public static final String MONITORED_SERVICE_URL_PARAM_NAME = "spm.remote.jmx.url";
  public static final String MONITORED_SERVICE_PASSWORD_FILE_PARAM_NAME = "spm.remote.jmx.password.file";
  public static final String MONITORED_SERVICE_USERNAME_PARAM_NAME = "spm.remote.jmx.username";
  public static final String MONITORED_SERVICE_PASSWORD_PARAM_NAME = "spm.remote.jmx.password";

  private static final Map<String, JmxServiceContext> JMX_CONTEXTS = new HashMap<String, JmxServiceContext>();

  public static JmxServiceContext getContext(String token, String jvmName, String subtype) {
    String key = getKey(token, jvmName, subtype);
    return JMX_CONTEXTS.get(key);
  }

  public static JmxServiceContext getContext(File monitorPropertiesFile) {
    String token = MonitorUtil.extractToken(monitorPropertiesFile.getAbsolutePath());
    String jvmName = MonitorUtil.extractJvmName(monitorPropertiesFile.getAbsolutePath(), token);
    String subtype = MonitorUtil.extractConfSubtype(monitorPropertiesFile.getAbsolutePath());

    String key = getKey(token, jvmName, subtype);
    return JMX_CONTEXTS.get(key);
  }

  public static JmxServiceContext init(Properties agentProperties, String token, String jvmName, String subtype) {
    String key = getKey(token, jvmName, subtype);
    if (JMX_CONTEXTS.containsKey(key)) {
      throw new IllegalStateException(
          "JmxServiceContext for token=" + token + ", jvmName=" + jvmName + ", subtype=" + subtype +
              " already initialized");
    } else {
      JmxServiceContext ctx = new JmxServiceContext();
      JMX_CONTEXTS.put(key, ctx);

      String jmxParamsPropertyVal = agentProperties.getProperty("SPM_MONITOR_JMX_PARAMS");

      if (jmxParamsPropertyVal != null) {
        String spmJmxParams = MonitorUtil.stripQuotes(jmxParamsPropertyVal.trim()).trim();
        Map<String, String> spmJmxParamsProperties = extractProperties(spmJmxParams);

        String jmxUrl = spmJmxParamsProperties.get(MONITORED_SERVICE_URL_PARAM_NAME);

        if (jmxUrl != null) {
          ctx.setRemote(true);
          ctx.setUrl(jmxUrl);

          if (spmJmxParamsProperties.get(MONITORED_SERVICE_PASSWORD_FILE_PARAM_NAME) != null) {
            List<String> lines;
            try {
              File passwdFile = new File(spmJmxParamsProperties.get(MONITORED_SERVICE_PASSWORD_FILE_PARAM_NAME));
              lines = FileUtil.readLines(passwdFile);
            } catch (IOException e) {
              throw new IllegalArgumentException("Password file " +
                                                     spmJmxParamsProperties
                                                         .get(MONITORED_SERVICE_PASSWORD_FILE_PARAM_NAME)
                                                     + " can't be read", e);
            }

            if (lines.size() != 1) {
              throw new IllegalArgumentException("Password file " +
                                                     spmJmxParamsProperties
                                                         .get(MONITORED_SERVICE_PASSWORD_FILE_PARAM_NAME)
                                                     + " must have exactly 1 line");
            }

            if (lines.get(0).trim().equals("")) {
              throw new IllegalArgumentException("Password file " +
                                                     spmJmxParamsProperties
                                                         .get(MONITORED_SERVICE_PASSWORD_FILE_PARAM_NAME)
                                                     + " can't be empty");
            }

            String line = lines.get(0).trim();
            int indexOfSplit = line.indexOf(" ");

            if (indexOfSplit == -1) {
              throw new IllegalArgumentException("Password file " +
                                                     spmJmxParamsProperties
                                                         .get(MONITORED_SERVICE_PASSWORD_FILE_PARAM_NAME)
                                                     + " must have format : \"username password\"");
            }

            ctx.setUsername(line.substring(0, indexOfSplit).trim());
            ctx.setPassword(line.substring(indexOfSplit + 1).trim());
          } else {
            // insecure, but it doesn't hurt us to support this in case someone really needs it for some reason
            ctx.setUsername(spmJmxParamsProperties.get(MONITORED_SERVICE_USERNAME_PARAM_NAME));
            ctx.setPassword(spmJmxParamsProperties.get(MONITORED_SERVICE_PASSWORD_PARAM_NAME));
          }
        }
      }

      LOG.info("JmxServiceContext prepared for URL: " + ctx.getUrl() + " and username: " + ctx.getUsername());
      return ctx;
    }
  }

  private static Map<String, String> extractProperties(String spmJmxParams) {
    Map<String, String> props = new HashMap<String, String>();

    spmJmxParams = spmJmxParams.trim();

    if (spmJmxParams.equals("")) {
      return props;
    }

    while (true) {
      spmJmxParams = spmJmxParams.trim();

      int indexOfNext = spmJmxParams.indexOf("-D");
      if (indexOfNext == -1) {
        break;
      } else {
        String rest = spmJmxParams.substring(indexOfNext + 2);

        int indexOfNextNext = rest.indexOf("-D");
        String param = indexOfNextNext == -1 ? rest.trim() : rest.substring(0, indexOfNextNext);
        String paramName = param.substring(0, param.indexOf("=")).trim();
        String paramValue = param.substring(param.indexOf("=") + 1).trim();
        props.put(paramName, paramValue);
        spmJmxParams = indexOfNextNext == -1 ? "" : rest.substring(indexOfNextNext);
      }
    }

    return props;
  }

  public static JmxServiceContext initLocal(String token, String jvmName, String subtype) {
    String key = getKey(token, jvmName, subtype);
    if (JMX_CONTEXTS.containsKey(key)) {
      throw new IllegalStateException(
          "JmxServiceContext for token=" + token + ", jvmName=" + jvmName + ", subtype=" + subtype +
              " already initialized");
    } else {
      JmxServiceContext ctx = new JmxServiceContext();
      JMX_CONTEXTS.put(key, ctx);
      ctx.setRemote(false);
      return ctx;
    }
  }

  private static String getKey(String token, String jvmName, String subtype) {
    return token + "__" + (jvmName != null ? jvmName : "") + "__" + (subtype != null ? subtype : "");
  }

  private JmxServiceContext() {
  }

  /**
   * describes whether JMX should be accessed remotely
   */
  private boolean remote;

  private String url;
  private String username;
  private String password;
  private String passwordFile;

  public boolean isRemote() {
    return remote;
  }

  public void setRemote(boolean remote) {
    this.remote = remote;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getPasswordFile() {
    return passwordFile;
  }

  public void setPasswordFile(String passwordFile) {
    this.passwordFile = passwordFile;
  }
}
