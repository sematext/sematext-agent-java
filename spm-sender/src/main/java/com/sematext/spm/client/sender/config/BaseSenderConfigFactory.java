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

import java.util.Map;

public abstract class BaseSenderConfigFactory implements SenderConfigFactory {
  private final Configuration config;

  public BaseSenderConfigFactory(Configuration config) {
    this.config = config;
  }

  protected final Configuration getConfiguration() {
    return config;
  }

  protected abstract Map<String, String> getProperties();

  protected abstract boolean postProcess(SenderConfig.Builder builder);

  protected abstract String getContentType();

  protected abstract String getFlumeSubdir();

  @Override
  public final SenderConfig getConfig() throws Exception {
    final Map<String, String> properties = getProperties();

    final SenderConfig.Builder builder = new SenderConfig.Builder();
    builder.setFlumeSubdir(getFlumeSubdir()).setSinkClass(properties.get("spm_sender_sink_class")).
        setContentType(getContentType()).
        setReceiverUrl(properties.get("server_base_url")).
        setMetricsEndpoint(properties.get("metrics_endpoint")).
        setTagsEndpoint(properties.get("tag_aliases_endpoint")).
        setMetainfoEndpoint(properties.get("metainfo_endpoint")).
        setProxyHost(properties.get("proxy_host")).
        setProxyPort(properties.get("proxy_port") != null &&
                          !properties.get("proxy_port").trim().equals("") ?
                          Integer.valueOf(properties.get("proxy_port").trim()) : null).
        setProxyUser(properties.get("proxy_user_name")).
        setProxyPassword(properties.get("proxy_password"));

    postProcess(builder);

    return builder.config();
  }
}
