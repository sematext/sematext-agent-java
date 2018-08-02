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
package com.sematext.spm.client.es;

import com.sematext.spm.client.es.info.EsClusterInfo;
import com.sematext.spm.client.http.ServerInfo;
import com.sematext.spm.client.json.JsonStatsExtractorConfig;
import com.sematext.spm.client.observation.BeanConditionCheck;

public class IsDataNodeCheck extends BeanConditionCheck {
  @Override
  protected String getConditionValue() {
    JsonStatsExtractorConfig config = (JsonStatsExtractorConfig) getExtractorConfig();
    ServerInfo serverInfo = config.getJsonServerInfo();

    return String.valueOf(EsClusterInfo.isDataHostingNode(serverInfo));
  }

  @Override
  protected boolean clauseSatisfies(String conditionValue, String expectedValue) {
    return conditionValue.equalsIgnoreCase(expectedValue.trim());
  }
}
