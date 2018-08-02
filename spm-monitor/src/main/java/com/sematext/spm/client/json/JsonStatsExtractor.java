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

import java.util.Set;

import com.sematext.spm.client.StatsExtractor;
import com.sematext.spm.client.http.CachableReliableDataSourceBase;
import com.sematext.spm.client.observation.ObservationBeanDump;

public class JsonStatsExtractor extends StatsExtractor<JsonStatsExtractorConfig, JsonObservation> {
  private CachableReliableDataSourceBase<Object, JsonDataProvider> dataSource;

  public JsonStatsExtractor(JsonStatsExtractorConfig config) {
    super(config);

    dataSource = JsonDataSourceCachedFactory
        .getDataSource(config.getJsonServerInfo(), config.getDataRequestUrl(), config.isAsync(), config
            .isUseSmile(), config.getJsonHandlerClass());
  }

  @Override
  protected Set<ObservationBeanDump> collectObservationStats(JsonObservation observation) {
    return observation.collectStats(dataSource.fetchData());
  }

  @Override
  protected boolean checkBeanShouldBeCollected(String observationName, JsonObservation obsv) {
    // our json implementation requires all beans to be collected
    return true;
  }

  public CachableReliableDataSourceBase<Object, JsonDataProvider> getDataSource() {
    return dataSource;
  }

  @Override
  public void close() {
    dataSource.close();
  }

}
