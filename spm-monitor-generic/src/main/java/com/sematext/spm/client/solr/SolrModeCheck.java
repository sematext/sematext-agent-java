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
package com.sematext.spm.client.solr;

import com.sematext.spm.client.http.ServerInfo;
import com.sematext.spm.client.json.JsonStatsExtractorConfig;
import com.sematext.spm.client.observation.BeanConditionCheck;

/**
 * Matches Solr's run mode as reported by /admin/info/system: "solrcloud" or "std" (user-managed). Metric labels
 * differ between the two, so SolrCloud and user-managed configs are kept apart by this check.
 */
public class SolrModeCheck extends BeanConditionCheck {
  @Override
  protected String getConditionValue() {
    ServerInfo serverInfo = ((JsonStatsExtractorConfig) getExtractorConfig()).getJsonServerInfo();
    return String.valueOf(SolrSystemInfo.mode(SolrSystemInfo.fetch(serverInfo)));
  }

  @Override
  protected boolean clauseSatisfies(String conditionValue, String expectedValue) {
    return conditionValue.equalsIgnoreCase(expectedValue.trim());
  }
}
