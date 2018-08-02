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

import org.eclipse.collections.impl.list.mutable.FastList;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.StatsExtractorConfig;
import com.sematext.spm.client.attributes.MetricType;
import com.sematext.spm.client.config.CollectorFileConfig;
import com.sematext.spm.client.config.DataConfig;
import com.sematext.spm.client.config.ObservationDefinitionConfig;

public class DbStatsExtractorConfig extends StatsExtractorConfig<DbObservation> {
  private String dataRequestQuery;
  private String dbUrl;
  private String dbDriverClass;
  private String dbUser;
  private String dbPassword;
  private String dbAdditionalConnectionParams;
  private boolean dbVerticalDataModel;

  public DbStatsExtractorConfig(CollectorFileConfig config, MonitorConfig monitorConfig)
      throws ConfigurationFailedException {
    super(config, monitorConfig);
  }

  public DbStatsExtractorConfig(DbStatsExtractorConfig orig, boolean createObservationDuplicates) {
    super(orig, createObservationDuplicates);
  }

  @Override
  protected void readFields(CollectorFileConfig config) throws ConfigurationFailedException {
    readConditions(config);

    setObservations(new FastList<DbObservation>());
    for (ObservationDefinitionConfig obs : config.getObservation()) {
      getObservations().add(new DbObservation(obs));
    }

    DataConfig data = config.getData();

    if (data == null) {
      throw new ConfigurationFailedException("Configuration of DB observation must contain one 'data' attribute.");
    } else {
      dataRequestQuery = data.getQuery();
      dbUrl = data.getDbUrl();
      dbDriverClass = data.getDbDriverClass();
      dbUser = data.getDbUser();
      dbPassword = data.getDbPassword();
      dbAdditionalConnectionParams = data.getDbAdditionalConnectionParams();
      dbVerticalDataModel = data.isDbVerticalModel();
    }

    if (!dbVerticalDataModel) {
      // when horizontal db model is used, allow only gauge and text attributes to be used
      for (DbObservation dbo : getObservations()) {
        for (DbAttributeObservation dba : dbo.getAttributeObservations()) {
          MetricType metricType = dba.getMetricType();
          if (metricType != MetricType.GAUGE && metricType != MetricType.TEXT) {
            throw new ConfigurationFailedException("Found metric " + dba.getFinalName() + " of type " + metricType +
                                                       " defined in combination with dbVerticalDataModel="
                                                       + dbVerticalDataModel + ". Such combination is currently" +
                                                       " not supported, only gauge and text types are allowed when dbVerticalDataModel=false");
          }
        }
      }
    }
  }

  @Override
  protected void copyFrom(StatsExtractorConfig<DbObservation> origConfig) {
    this.dataRequestQuery = ((DbStatsExtractorConfig) origConfig).dataRequestQuery;
    this.dbUrl = ((DbStatsExtractorConfig) origConfig).dbUrl;
    this.dbDriverClass = ((DbStatsExtractorConfig) origConfig).dbDriverClass;
    this.dbUser = ((DbStatsExtractorConfig) origConfig).dbUser;
    this.dbPassword = ((DbStatsExtractorConfig) origConfig).dbPassword;
    this.dbAdditionalConnectionParams = ((DbStatsExtractorConfig) origConfig).dbAdditionalConnectionParams;
    this.dbVerticalDataModel = ((DbStatsExtractorConfig) origConfig).dbVerticalDataModel;
  }

  public String getDataRequestQuery() {
    return dataRequestQuery;
  }

  public String getDbUrl() {
    return dbUrl;
  }

  public String getDbDriverClass() {
    return dbDriverClass;
  }

  public String getDbUser() {
    return dbUser;
  }

  public String getDbPassword() {
    return dbPassword;
  }

  public String getDbAdditionalConnectionParams() {
    return dbAdditionalConnectionParams;
  }

  public boolean isDbVerticalDataModel() {
    return dbVerticalDataModel;
  }
}
