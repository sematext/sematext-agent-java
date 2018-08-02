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
package com.sematext.spm.client.http;

public class ServerInfo {
  private String server;
  private String hostname;
  private String port;
  private String basicHttpAuthUsername;
  private String basicHttpAuthPassword;
  private String id;

  public ServerInfo(String server, String hostname, String port, String basicHttpAuthUsername,
                    String basicHttpAuthPassword) {
    this.server = server;
    this.hostname = hostname;
    this.port = port;
    this.basicHttpAuthPassword = basicHttpAuthPassword;
    this.basicHttpAuthUsername = basicHttpAuthUsername;
    this.id = server + "_" + basicHttpAuthUsername + "_" + basicHttpAuthPassword;
  }

  public String getServer() {
    return server;
  }

  public String getHostname() {
    return hostname;
  }

  public String getPort() {
    return port;
  }

  public String getBasicHttpAuthUsername() {
    return basicHttpAuthUsername;
  }

  public String getBasicHttpAuthPassword() {
    return basicHttpAuthPassword;
  }

  public String getId() {
    return id;
  }
}
