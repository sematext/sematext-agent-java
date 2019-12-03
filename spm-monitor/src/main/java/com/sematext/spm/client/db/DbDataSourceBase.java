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

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

/**
 * For each monitored DB, for each metrics query, use one DbDataSourceBase instance.
 */
public class DbDataSourceBase {
  private static final Log LOG = LogFactory.getLog(DbDataSourceBase.class);

  private static final int DEFAULT_VERTICAL_MODEL_ENTITIES_COUNT = 1;
  private static final int DEFAULT_VERTICAL_MODEL_EXPECTED_ATTRIBUTE_COUNT = 20;
  private static final int DEFAULT_HORIZONTAL_MODEL_EXPECTED_ROWS_COUNT = 10;
  private static final int DEFAULT_HORIZONTAL_MODEL_EXPECTED_ATTRIBUTE_COUNT = 20;

  // TODO consider if it would make sense to make this value configurable or maybe it can be calculated
  // based on Monitor's interval between two StatsCollector cycles
  private static final long FRESH_DATA_TIMEOUT_MS = 5000;
  private static final long MAX_SUCCESSIVE_FAILED_TRIES = 5;
  private static final long ERROR_STATE_INACTIVITY_PAUSE = 2 * 60 * 1000; // 2 minutes
  private static final int CONNECTION_VALID_CHECK_TIMEOUT = 30 * 1000;

  private DbConnectionManager dbConnectionManager;
  private String dbQuery;

  // vertical model is the one where each attribute of some entity is a new row; traditional model (horizontal one) is where
  // one row represents an entity where its attributes are each in its own column of that row
  private boolean verticalDataModel;

  // in case data source encountered an error while executing request, it will be moved to inactive state
  // which means that particular DataSource and its StatsCollectors will be ignored; after the inactivity
  // period lasting ERROR_STATE_INACTIVITY_PAUSE ms passes, the DataSource will again be tested for
  // MAX_SUCCESSIVE_FAILED_TRIES attempts.
  private boolean active = true;
  private long inactivityPeriodStartTime = 0L;
  private int successiveFailedTries = 0;

  private volatile List<Map<String, Object>> freshDbData;
  private long lastDataFetchTime = 0L;

  public DbDataSourceBase(DbConnectionManager dbConnectionManager, String dbQuery, boolean verticalDataModel) {
    this.dbConnectionManager = dbConnectionManager;
    this.dbQuery = dbQuery;
    this.verticalDataModel = verticalDataModel;
  }

  /**
   * @return Map with fresh data, or null if that map would be empty or there was some error while fetching
   */
  public List<Map<String, Object>> fetchData() {
    if (!active) {
      long currentTime = System.currentTimeMillis();

      // if inactivity period passed, activate the datasource again
      if ((currentTime - inactivityPeriodStartTime) > ERROR_STATE_INACTIVITY_PAUSE) {
        activate();
      } else {
        freshDbData = null;
        return freshDbData;
      }
    }

    // if refresh interval still hasn't passed, return existing data
    if (!((System.currentTimeMillis() - lastDataFetchTime) > FRESH_DATA_TIMEOUT_MS)) {
      return freshDbData;
    }

    synchronized (this) {
      // if refresh interval still hasn't passed, return existing data
      if (!((System.currentTimeMillis() - lastDataFetchTime) > FRESH_DATA_TIMEOUT_MS)) {
        return freshDbData;
      }

      Statement stmt = null;
      ResultSet rs = null;
      Connection conn = null;
      
      try {
        conn = dbConnectionManager.getConnection();

        if (conn == null) {
          LOG.error("DB connection not available, skipping query");
          freshDbData = null;
          return freshDbData;
        } else {
          LOG.info("Executing '" + dbQuery + "' on db: '" + dbConnectionManager.getDbUrl() + "'");
          stmt = conn.createStatement();
          rs = stmt.executeQuery(dbQuery);

          if (freshDbData == null) {
            // use slightly bigger map than needed, just in case
            if (verticalDataModel) {
              freshDbData = new ArrayList<Map<String, Object>>(DEFAULT_VERTICAL_MODEL_ENTITIES_COUNT);
            } else {
              freshDbData = new ArrayList<Map<String, Object>>(DEFAULT_HORIZONTAL_MODEL_EXPECTED_ROWS_COUNT);
            }
          }

          if (verticalDataModel) {
            // expect N rows for single resulting Map; reuse the map that was possibly previously used
            Map<String, Object> data;
            if (freshDbData.size() == 1) {
              data = freshDbData.get(0);
              data.clear();
            } else {
              freshDbData.clear();
              data = new UnifiedMap<String, Object>(DEFAULT_VERTICAL_MODEL_EXPECTED_ATTRIBUTE_COUNT);
              freshDbData.add(data);
            }

            while (rs.next()) {
              data.put(rs.getString(1).trim(), rs.getObject(2));
            }
          } else {
            // expect N rows for N resulting Maps
            // we'll try to reuse Map objects that exist from previous collections
            int currentRow = 0;
            while (rs.next()) {
              Map<String, Object> data;
              if (currentRow < freshDbData.size()) {
                data = freshDbData.get(currentRow);
                data.clear();
              } else {
                data = new UnifiedMap<String, Object>(DEFAULT_HORIZONTAL_MODEL_EXPECTED_ATTRIBUTE_COUNT);
                freshDbData.add(data);
              }

              ResultSetMetaData rsmd = rs.getMetaData();
              for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                String name = rsmd.getColumnLabel(i);
                Object value = rs.getObject(name);
                data.put(name, value);
              }
              currentRow++;
            }

            // in the end remove any rows that were left in freshDbData from before
            if (currentRow < freshDbData.size()) {
              // if there are 4 rows, value of "currentRow" at this point will be 4 (equal to count of rows found in this iteration)
              freshDbData.subList(currentRow, freshDbData.size()).clear();
            }
          }

          if (freshDbData.isEmpty()) {
            LOG.warn("Empty result set found for query: " + dbQuery + ", dbUrl: " + dbConnectionManager.getDbUrl());
            freshDbData = null;
          }

          lastDataFetchTime = System.currentTimeMillis();
          successiveFailedTries = 0;

          return freshDbData;
        }
      } catch (Throwable thr) {
        LOG.error("Error while fetching data with " + this.getClass().getCanonicalName() +
                      " for query " + dbQuery + ". Message: " + thr.getMessage());

        if (LOG.isDebugEnabled()) {
          LOG.debug(thr);
        }

        recordFailedRequest();
        
        if (conn != null) {
          try {
            if (!conn.isValid(CONNECTION_VALID_CHECK_TIMEOUT)) {
              dbConnectionManager.reconnect();
            }
          } catch (Throwable thr1) {
            LOG.error("Error while checking connection validity", thr1);
          }
        }

        freshDbData = null;
        return freshDbData;
      } finally {
        try {
          if (rs != null) {
            rs.close();
          }
        } catch (Throwable thr) {
          LOG.error("Error while closing ResultSet for query: " + dbQuery, thr);
        }
        try {
          if (stmt != null) {
            stmt.close();
          }
        } catch (Throwable thr) {
          LOG.error("Error while closing Statement for query: " + dbQuery, thr);
        }
      }
    }
  }

  protected void activate() {
    LOG.info("Activating data source for query " + dbQuery);
    active = true;
    inactivityPeriodStartTime = 0L;
    successiveFailedTries = 0;
  }

  protected void recordFailedRequest() {
    successiveFailedTries++;
    freshDbData = null;
    lastDataFetchTime = System.currentTimeMillis();

    if (successiveFailedTries >= MAX_SUCCESSIVE_FAILED_TRIES) {
      LOG.error("Max allowed successive failed calls reached " + successiveFailedTries + " for query " +
                    dbQuery + ". Setting data collector to inactive state for " + ERROR_STATE_INACTIVITY_PAUSE +
                    " ms");

      active = false;
      inactivityPeriodStartTime = System.currentTimeMillis();
      successiveFailedTries = 0;
    }
  }

  public void close() {
    if (dbConnectionManager != null) {
      dbConnectionManager.close();
    }
  }
}
