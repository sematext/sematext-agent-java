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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

/**
 * Creates DB datasources and caches them (so for each db-query there is only one datasource instance). Also supports
 * queries into multiple databases inside of single Monitor agent.
 */
public final class DbDataSourceCachedFactory {
  private static final Log LOG = LogFactory.getLog(DbDataSourceCachedFactory.class);
  private static final DbDataSourceCachedFactory INSTANCE = new DbDataSourceCachedFactory();

  private Map<String, DbDataSourceBase> datasources = new ConcurrentHashMap<String, DbDataSourceBase>();

  private DbDataSourceCachedFactory() {
  }

  public static DbDataSourceBase getDataSource(String dbUrl, String dbQuery, boolean verticalDataModel) {
    if (dbUrl == null || dbQuery == null) {
      return null;
    }

    if (DbConnectionManager.getConnectionManager(dbUrl) == null) {
      throw new IllegalStateException("DbDataSourceCachedFactory not initialized for dbUrl: " + dbUrl + "!");
    } else {
      String key = dbUrl + "-" + dbQuery;

      if (INSTANCE.datasources.containsKey(key)) {
        return INSTANCE.datasources.get(key);
      } else {
        synchronized (INSTANCE) {
          // avoiding DCL report from checkstyle by using a slightly different expression than before
          // synchronized block - we can avoid it here since we know that the map instance itself is
          // already safely initialized and the object we're fetching is being constructed safely
          // within larger synchronized block
          if (INSTANCE.datasources.get(key) != null) {
            return INSTANCE.datasources.get(key);
          } else {
            LOG.info("Creating DbDataSourceBased with key: '" + key + "'");

            DbDataSourceBase ds = new DbDataSourceBase(DbConnectionManager
                                                           .getConnectionManager(dbUrl), dbQuery, verticalDataModel);
            INSTANCE.datasources.put(key, ds);

            return ds;
          }
        }
      }
    }
  }
}
