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
package com.sematext.spm.client.db;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sematext.spm.client.StatsExtractor;
import com.sematext.spm.client.observation.ObservationBeanDump;

public class DbStatsExtractor extends StatsExtractor<DbStatsExtractorConfig, DbObservation> {
  private static final Set<ObservationBeanDump> EMPTY_OBSERVATION_BEAN_DUMP = new HashSet<ObservationBeanDump>();

  private DbDataSourceBase dataSource;

  public DbStatsExtractor(DbStatsExtractorConfig config) {
    super(config);

    dataSource = DbDataSourceCachedFactory
        .getDataSource(config.getDbUrl(), config.getDataRequestQuery(), config.isDbVerticalDataModel());
  }

  @Override
  protected Set<ObservationBeanDump> collectObservationStats(DbObservation observation) {
    Set<ObservationBeanDump> finalDump = null;

    List<Map<String, Object>> data = dataSource.fetchData();

    if (data == null) {
      return EMPTY_OBSERVATION_BEAN_DUMP;
    }

    boolean createCopies = data.size() > 1;
    for (Map<String, Object> rowData : data) {
      Set<ObservationBeanDump> singleDump = observation.collectStats(rowData);
      if (singleDump.size() == 0) {
        continue;
      } else if (singleDump.size() == 1) {
        if (finalDump == null) {
          finalDump = new HashSet<ObservationBeanDump>();
        }
        if (createCopies) {
          // if there are multiple dumps resulting from single observation bean, we have to modify the names of
          // these beans, otherwise they'll overwrite each other in resulting sets/maps
          ObservationBeanDump dump = singleDump.iterator().next();
          finalDump.add(dump.getCopyWithName(dump.getName() + "_collectedRow_" + finalDump.size()));
        } else {
          finalDump.add(singleDump.iterator().next());
        }
      } else {
        throw new IllegalStateException("DB bean dumps should contain max 1 element!");
      }
    }

    if (finalDump == null || finalDump.size() == 0) {
      return EMPTY_OBSERVATION_BEAN_DUMP;
    } else {
      return finalDump;
    }
  }

  @Override
  protected boolean checkBeanShouldBeCollected(String observationName, DbObservation obsv) {
    return true;
  }

  public DbDataSourceBase getDataSource() {
    return dataSource;
  }

  @Override
  public void close() {
    dataSource.close();
  }

}
