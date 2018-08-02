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

public class Endpoint {
  private final String address;
  private final String hostname;

  public Endpoint(String address, String hostname) {
    this.address = address;
    this.hostname = hostname;
  }

  public String getAddress() {
    return address;
  }

  public String getHostname() {
    return hostname;
  }

  @Override
  public String toString() {
    return "Endpoint{" +
        "address='" + address + '\'' +
        ", hostname='" + hostname + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Endpoint endpoint = (Endpoint) o;

    if (address != null ? !address.equals(endpoint.address) : endpoint.address != null) return false;
    if (hostname != null ? !hostname.equals(endpoint.hostname) : endpoint.hostname != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = address != null ? address.hashCode() : 0;
    result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
    return result;
  }
}
