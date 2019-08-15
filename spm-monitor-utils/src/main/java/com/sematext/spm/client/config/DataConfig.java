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
package com.sematext.spm.client.config;

public class DataConfig {
  private String query;
  private String dbUrl;
  private String[] dbDriverClass;
  private String dbUser;
  private String dbPassword;
  private String dbAdditionalConnectionParams;
  private boolean dbVerticalModel = false;

  private String url;
  private String server;
  private String basicHttpAuthUsername;
  private String basicHttpAuthPassword;
  private boolean smileFormat = false;
  private boolean async = false;
  private String jsonHandlerClass;

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getDbUrl() {
    return dbUrl;
  }

  public void setDbUrl(String dbUrl) {
    this.dbUrl = dbUrl;
  }

  public String[] getDbDriverClass() {
    return dbDriverClass;
  }

  public void setDbDriverClass(String[] dbDriverClass) {
    this.dbDriverClass = dbDriverClass;
  }

  public String getDbUser() {
    return dbUser;
  }

  public void setDbUser(String dbUser) {
    this.dbUser = dbUser;
  }

  public String getDbPassword() {
    return dbPassword;
  }

  public void setDbPassword(String dbPassword) {
    this.dbPassword = dbPassword;
  }

  public String getDbAdditionalConnectionParams() {
    return dbAdditionalConnectionParams;
  }

  public void setDbAdditionalConnectionParams(String dbAdditionalConnectionParams) {
    this.dbAdditionalConnectionParams = dbAdditionalConnectionParams;
  }

  public boolean isDbVerticalModel() {
    return dbVerticalModel;
  }

  public void setDbVerticalModel(String dbVerticalModel) {
    if ("true".equalsIgnoreCase(dbVerticalModel)) {
      this.dbVerticalModel = true;
    } else if ("false".equalsIgnoreCase(dbVerticalModel)) {
      this.dbVerticalModel = false;
    } else {
      throw new IllegalArgumentException(
          "Unrecognized boolean value: " + dbVerticalModel + " for field 'dbVerticalModel'");
    }
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getServer() {
    return server;
  }

  public void setServer(String server) {
    this.server = server;
  }

  public String getBasicHttpAuthUsername() {
    return basicHttpAuthUsername;
  }

  public void setBasicHttpAuthUsername(String basicHttpAuthUsername) {
    this.basicHttpAuthUsername = basicHttpAuthUsername;
  }

  public String getBasicHttpAuthPassword() {
    return basicHttpAuthPassword;
  }

  public void setBasicHttpAuthPassword(String basicHttpAuthPassword) {
    this.basicHttpAuthPassword = basicHttpAuthPassword;
  }

  public boolean isSmileFormat() {
    return smileFormat;
  }

  public void setSmileFormat(String smileFormat) {
    if ("true".equalsIgnoreCase(smileFormat)) {
      this.smileFormat = true;
    } else if ("false".equalsIgnoreCase(smileFormat)) {
      this.smileFormat = false;
    } else {
      throw new IllegalArgumentException("Unrecognized boolean value: " + smileFormat + " for field 'smileFormat'");
    }
  }

  public boolean isAsync() {
    return async;
  }

  public void setAsync(String async) {
    if ("true".equalsIgnoreCase(async)) {
      this.async = true;
    } else if ("false".equalsIgnoreCase(async)) {
      this.async = false;
    } else {
      throw new IllegalArgumentException("Unrecognized boolean value: " + async + " for field 'async'");
    }
  }

  public String getJsonHandlerClass() {
    return jsonHandlerClass;
  }

  public void setJsonHandlerClass(String jsonHandlerClass) {
    this.jsonHandlerClass = jsonHandlerClass;
  }
}
