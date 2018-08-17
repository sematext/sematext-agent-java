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
package com.sematext.spm.client.sender.config;

import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.Set;

public final class SenderConfig {
  private String host;
  private Set<String> tokens;
  private String receiverUrl;
  private String metricsEndpoint;
  private String tagsEndpoint;
  private String metainfoEndpoint;
  private String proxyHost;
  private Integer proxyPort;
  private String proxyUser;
  private String proxyPassword;
  private String contentType;
  private String flumeSubdir;
  private String sinkClass;

  private long creationTime = System.currentTimeMillis();

  private SenderConfig() {
  }

  public String getHost() {
    return host;
  }

  public Set<String> getTokens() {
    return tokens;
  }

  public String getReceiverUrl() {
    return receiverUrl;
  }

  public String getProxyHost() {
    return proxyHost;
  }

  public Integer getProxyPort() {
    return proxyPort;
  }

  public String getProxyUser() {
    return proxyUser;
  }

  public String getProxyPassword() {
    return proxyPassword;
  }

  public String getContentType() {
    return contentType;
  }

  public String getFlumeSubdir() {
    return flumeSubdir;
  }

  public String getSinkClass() {
    return sinkClass;
  }

  /*CHECKSTYLE:OFF*/
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SenderConfig that = (SenderConfig) o;

    if (host != null ? !host.equals(that.host) : that.host != null) return false;
    if (proxyHost != null ? !proxyHost.equals(that.proxyHost) : that.proxyHost != null) return false;
    if (proxyPassword != null ? !proxyPassword.equals(that.proxyPassword) : that.proxyPassword != null) return false;
    if (proxyPort != null ? !proxyPort.equals(that.proxyPort) : that.proxyPort != null) return false;
    if (proxyUser != null ? !proxyUser.equals(that.proxyUser) : that.proxyUser != null) return false;
    if (receiverUrl != null ? !receiverUrl.equals(that.receiverUrl) : that.receiverUrl != null) return false;
    if (metricsEndpoint != null ? !metricsEndpoint.equals(that.metricsEndpoint) : that.metricsEndpoint != null) return false;
    if (tagsEndpoint != null ? !tagsEndpoint.equals(that.tagsEndpoint) : that.tagsEndpoint != null) return false;
    if (metainfoEndpoint != null ? !metainfoEndpoint.equals(that.metainfoEndpoint) : that.metainfoEndpoint != null) return false;
    if (tokens != null ? !tokens.equals(that.tokens) : that.tokens != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = host != null ? host.hashCode() : 0;
    result = 31 * result + (tokens != null ? tokens.hashCode() : 0);
    result = 31 * result + (receiverUrl != null ? receiverUrl.hashCode() : 0);
    result = 31 * result + (metricsEndpoint != null ? metricsEndpoint.hashCode() : 0);
    result = 31 * result + (tagsEndpoint != null ? tagsEndpoint.hashCode() : 0);
    result = 31 * result + (metainfoEndpoint != null ? metainfoEndpoint.hashCode() : 0);
    result = 31 * result + (proxyHost != null ? proxyHost.hashCode() : 0);
    result = 31 * result + (proxyPort != null ? proxyPort.hashCode() : 0);
    result = 31 * result + (proxyUser != null ? proxyUser.hashCode() : 0);
    result = 31 * result + (proxyPassword != null ? proxyPassword.hashCode() : 0);
    return result;
  }
  /*CHECKSTYLE:ON*/

  @Override
  public String toString() {
    return "SenderConfig{" +
        "host='" + host + '\'' +
        ", tokens=" + tokens +
        ", receiverUrl='" + receiverUrl + '\'' +
        ", metricsEndpoint='" + metricsEndpoint + '\'' +
        ", tagsEndpoint='" + tagsEndpoint + '\'' +
        ", metainfoEndpoint='" + metainfoEndpoint + '\'' +
        ", proxyHost='" + proxyHost + '\'' +
        ", proxyPort=" + proxyPort +
        ", proxyUser='" + proxyUser + '\'' +
        ", proxyPassword='" + proxyPassword + '\'' +
        ", sinkClass=\'" + sinkClass + '\'' +
        '}';
  }

  public static class Builder {
    private final SenderConfig config = new SenderConfig();

    public Builder setHost(String host) {
      this.config.host = host;
      return this;
    }

    public Builder setToken(String token) {
      this.config.tokens = Collections.singleton(token);
      return this;
    }

    public Builder setTokens(Set<String> tokens) {
      this.config.tokens = Sets.newHashSet(tokens);
      return this;
    }

    public Builder setReceiverUrl(String receiverUrl) {
      this.config.receiverUrl = receiverUrl;
      return this;
    }

    public Builder setMetricsEndpoint(String metricsEndpoint) {
      this.config.metricsEndpoint = metricsEndpoint;
      return this;
    }

    public Builder setTagsEndpoint(String tagsEndpoint) {
      this.config.tagsEndpoint = tagsEndpoint;
      return this;
    }

    public Builder setMetainfoEndpoint(String metainfoEndpoint) {
      this.config.metainfoEndpoint = metainfoEndpoint;
      return this;
    }

    public Builder setProxyHost(String proxyHost) {
      this.config.proxyHost = proxyHost;
      return this;
    }

    public Builder setProxyPort(Integer proxyPort) {
      this.config.proxyPort = proxyPort;
      return this;
    }

    public Builder setProxyUser(String proxyUser) {
      this.config.proxyUser = proxyUser;
      return this;
    }

    public Builder setProxyPassword(String proxyPassword) {
      this.config.proxyPassword = proxyPassword;
      return this;
    }

    public Builder setContentType(String contentType) {
      this.config.contentType = contentType;
      return this;
    }

    public Builder setFlumeSubdir(String flumeSubDir) {
      this.config.flumeSubdir = flumeSubDir;
      return this;
    }

    public Builder setSinkClass(String sinkClass) {
      this.config.sinkClass = sinkClass;
      return this;
    }

    public SenderConfig config() {
      return config;
    }
  }

  public long getCreationTime() {
    return creationTime;
  }

  public String getMetricsEndpoint() {
    return metricsEndpoint;
  }

  public String getTagsEndpoint() {
    return tagsEndpoint;
  }

  public String getMetainfoEndpoint() {
    return metainfoEndpoint;
  }
}
