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
package com.sematext.spm.client.json;

import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.HashSet;
import java.util.Set;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.StatsExtractorConfig;
import com.sematext.spm.client.config.CollectorFileConfig;
import com.sematext.spm.client.config.DataConfig;
import com.sematext.spm.client.config.ObservationDefinitionConfig;
import com.sematext.spm.client.http.ServerInfo;

public class JsonStatsExtractorConfig extends StatsExtractorConfig<JsonObservation> {
  private static final Log LOG = LogFactory.getLog(JsonStatsExtractorConfig.class);

  private String dataRequestUrl;
  private boolean async;
  private boolean useSmile;
  private String jsonHandlerClass;
  private ServerInfo jsonServerInfo;

  public JsonStatsExtractorConfig(CollectorFileConfig config, MonitorConfig monitorConfig)
      throws ConfigurationFailedException {
    super(config, monitorConfig);
  }

  public JsonStatsExtractorConfig(JsonStatsExtractorConfig orig, boolean createObservationDuplicates) {
    super(orig, createObservationDuplicates);
  }

  @Override
  protected void readFields(CollectorFileConfig config) throws ConfigurationFailedException {
    DataConfig data = config.getData();

    if (data == null) {
      throw new ConfigurationFailedException("Configuration of JSON observation must contain exactly one 'data' attribute!");
    } else {
      dataRequestUrl = data.getUrl();

      if (dataRequestUrl == null) {
        throw new ConfigurationFailedException(
            "Configuration of JSON observation must contain 'url' attribute under 'data' attribute!");
      }

      async = data.isAsync();
      jsonHandlerClass = data.getJsonHandlerClass();

      String server = data.getServer();
      if (server == null || server.trim().equals("")) {
        throw new ConfigurationFailedException(
            "Configuration of JSON observation must contain 'server' attribute under 'data' attribute!");
      }
      if (server.startsWith("${")) {
        throw new ConfigurationFailedException(
            "Configuration of JSON 'server' attribute contains unresolved property: " + server);
      }
      if (!server.startsWith("http://") && !server.startsWith("https://")) {
        server = "http://" + server;
      }
      String hostPort = server.substring(server.indexOf("//") + 2);
      hostPort = hostPort.indexOf("/") != -1 ? hostPort.substring(0, hostPort.indexOf("/")) : hostPort;
      String hostname = hostPort.indexOf(":") != -1 ? hostPort.substring(0, hostPort.indexOf(":")) : hostPort;
      String port = hostPort.indexOf(":") != -1 ? hostPort.substring(hostPort.indexOf(":") + 1).trim() : "80";

      dataRequestUrl = server + (!dataRequestUrl.startsWith("/") ? "/" : "") + dataRequestUrl;

      String basicHttpAuthUsername = data.getBasicHttpAuthUsername();
      basicHttpAuthUsername =
          basicHttpAuthUsername == null || basicHttpAuthUsername.startsWith("${") || basicHttpAuthUsername.equals("") ?
              null :
              basicHttpAuthUsername.trim();
      String basicHttpAuthPassword = data.getBasicHttpAuthPassword();
      basicHttpAuthPassword =
          basicHttpAuthPassword == null || basicHttpAuthPassword.startsWith("${") || basicHttpAuthPassword.equals("") ?
              null :
              basicHttpAuthPassword;
      if ((basicHttpAuthUsername == null && basicHttpAuthPassword != null) || (basicHttpAuthUsername != null
          && basicHttpAuthPassword == null)) {
        LOG.warn(
            "JSON observation definition has either attribute 'basicHttpAuthUsername' or attribute 'basicHttpAuthPassword' undefined. "
                +
                "Agent will assume http basic auth shouldn't be used!");
        basicHttpAuthUsername = null;
        basicHttpAuthPassword = null;
      }

      useSmile = data.isSmileFormat();

      jsonServerInfo = new ServerInfo(server, hostname, port, basicHttpAuthUsername, basicHttpAuthPassword);
    }

    readConditions(config);

    setObservations(new FastList<JsonObservation>());
    Set<String> existingBeanNames = new HashSet<String>();
    for (ObservationDefinitionConfig obs : config.getObservation()) {
      String name = obs.getName();
      if (existingBeanNames.contains(name)) {
        throw new ConfigurationFailedException("Found duplicate observations with name " + name);
      }
      existingBeanNames.add(name);
      String nodePath = obs.getPath();

      if (nodePath == null) {
        throw new ConfigurationFailedException("Json observations must have 'path' attribute defined! Observation " +
                                                   name + " doesn't have it!");
      }

      getObservations().add(new JsonObservation(obs, jsonServerInfo));
    }
  }

  public String getDataRequestUrl() {
    return dataRequestUrl;
  }

  public void setDataRequestUrl(String dataRequestUrl) {
    this.dataRequestUrl = dataRequestUrl;
  }

  public boolean isAsync() {
    return async;
  }

  public void setAsync(boolean async) {
    this.async = async;
  }

  public String getJsonHandlerClass() {
    return jsonHandlerClass;
  }

  public void setJsonHandlerClass(String jsonHandlerClass) {
    this.jsonHandlerClass = jsonHandlerClass;
  }

  @Override
  protected void copyFrom(StatsExtractorConfig<JsonObservation> origConfig) {
    this.dataRequestUrl = ((JsonStatsExtractorConfig) origConfig).dataRequestUrl;
    this.async = ((JsonStatsExtractorConfig) origConfig).async;
    this.jsonHandlerClass = ((JsonStatsExtractorConfig) origConfig).jsonHandlerClass;
    this.useSmile = ((JsonStatsExtractorConfig) origConfig).useSmile;
    this.jsonServerInfo = ((JsonStatsExtractorConfig) origConfig).jsonServerInfo;
  }

  public ServerInfo getJsonServerInfo() {
    return jsonServerInfo;
  }

  public boolean isUseSmile() {
    return useSmile;
  }
}
