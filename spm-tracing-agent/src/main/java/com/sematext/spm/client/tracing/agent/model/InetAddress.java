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
package com.sematext.spm.client.tracing.agent.model;

public final class InetAddress {
  private final String host;
  private final int port;

  public InetAddress(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    InetAddress that = (InetAddress) o;

    if (port != that.port) return false;
    if (host != null ? !host.equals(that.host) : that.host != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = host != null ? host.hashCode() : 0;
    result = 31 * result + port;
    return result;
  }

  @Override
  public String toString() {
    return "InetAddress{" +
        "host='" + host + '\'' +
        ", port=" + port +
        '}';
  }
}
