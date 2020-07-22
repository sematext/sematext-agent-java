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

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.status.AgentStatusRecorder;
import com.sematext.spm.client.status.AgentStatusRecorder.ConnectionStatus;

/**
 * Handles creation of single connection to some DB, knows how to reconnect in case of problems, takes care of
 * long/short retry intervals to avoid overloading the DB in case there is some connection related issue, takes
 * care that logs are not filled with too many same stacktraces.
 */
public class DbConnectionManager {
  private static final Log LOG = LogFactory.getLog(DbConnectionManager.class);

  private static final Map<String, DbConnectionManager> DB_CONNECTION_MANAGERS = new ConcurrentHashMap<String, DbConnectionManager>();

  private static final long TIME_BETWEEN_SHORT_RETRIES = 10 * 1000;
  private static final long TIME_BETWEEN_LONG_RETRIES = 45 * 1000;
  private static final long MAX_SHORT_RETRIES_PERIOD = 5 * 60 * 1000;

  private String dbUrl;
  private String dbDriverClass;
  private String user;
  private String password;
  private String additionalConnectionParams;

  private Connection dbConnection = null;

  private long lastSuccessTime = System.currentTimeMillis();
  private long lastFailedTime = 0L;
  private int consecutiveConnErrors = 0;

  public DbConnectionManager(String dbUrl, String dbDriverClass, String user, String password,
                             String additionalConnectionParams) {
    this.dbUrl = dbUrl;
    this.dbDriverClass = dbDriverClass;
    this.user = user;
    this.password = password;
    this.additionalConnectionParams = additionalConnectionParams;

    synchronized (DB_CONNECTION_MANAGERS) {
      if (DB_CONNECTION_MANAGERS.containsKey(dbUrl)) {
        throw new IllegalStateException("DbConnectionManager for dbUrl: " + dbUrl + " already created!");
      } else {
        if (createNewConnection() == null) {
          throw new IllegalStateException("Can't create connection for url: " + dbUrl + ", user: " + user +
                                              ", additional params: " + additionalConnectionParams);
        }

        DB_CONNECTION_MANAGERS.put(dbUrl, this);
      }
    }
  }

  /**
   * Should be used if using existing connection resulted in error.
   *
   * @return connection or null
   */
  public Connection reconnect() {
    LOG.info("Reconnecting to DB...");
    return createNewConnection();
  }

  public Connection getConnection() {
    if (dbConnection != null) {
      return dbConnection;
    } else {
      return createNewConnection();
    }
  }

  private Connection createNewConnection() {
    LOG.info("Creating new connection to DB - old connection (if exists) will be closed, new one created");

    // if old connection exists, try to close it first
    if (dbConnection != null) {
      try {
        dbConnection.close();
      } catch (Throwable thr) {
        // just debug level log, nothing else
        LOG.debug("Error while closing old connection", thr);
      }

      dbConnection = null;
    }

    LOG.info("Initializing new DB connection for url: " + dbUrl + ", user: " + user);

    long currentTime = System.currentTimeMillis();
    long timeSinceLastFailed = currentTime - lastFailedTime;
    long timeSinceLastSucceeded = currentTime - lastSuccessTime;

    LOG.info("Current time: " + currentTime + ", timeSinceLastFailed: " + timeSinceLastFailed +
                 ", timeSinceLastSucceeded: " + timeSinceLastSucceeded);

    if (timeSinceLastSucceeded > MAX_SHORT_RETRIES_PERIOD) {
      LOG.info("Long retries period active");
      if (timeSinceLastFailed > TIME_BETWEEN_LONG_RETRIES) {
        LOG.info("TIME_BETWEEN_LONG_RETRIES: " + TIME_BETWEEN_LONG_RETRIES +
                     " passed, new DB connection will be created...");
      } else {
        return null;
      }
    } else {
      LOG.info("Short retries period active");
      if (timeSinceLastFailed > TIME_BETWEEN_SHORT_RETRIES) {
        LOG.info("TIME_BETWEEN_SHORT_RETRIES: " + TIME_BETWEEN_SHORT_RETRIES +
                     " passed, new DB connection will be created...");
      } else {
        return null;
      }
    }

    // create new connection
    try {
      String dbConnUrl = dbUrl;
      if (additionalConnectionParams != null && !"".equals(additionalConnectionParams.trim())) {
        if (dbConnUrl.contains("?")) {
          dbConnUrl = dbConnUrl + "&" + additionalConnectionParams;
        } else {
          dbConnUrl = dbConnUrl + "?" + additionalConnectionParams;
        }
      }

      Class.forName(dbDriverClass);
      dbConnection = DriverManager.getConnection(dbConnUrl, user, password);

      if (dbConnection != null) {
        // reset error counters
        lastSuccessTime = System.currentTimeMillis();
        lastFailedTime = 0L;
        consecutiveConnErrors = 0;
      } else {
        throw new IllegalStateException("DriverManager.getConnection didn't produce connection or error for " +
                                            "url: " + dbUrl + ", user: " + user);
      }
      
      AgentStatusRecorder.GLOBAL_INSTANCE.updateConnectionStatus(ConnectionStatus.OK);
    } catch (Throwable thr) {
      AgentStatusRecorder.GLOBAL_INSTANCE.updateConnectionStatus(ConnectionStatus.FAILED, thr.getMessage());
      if (consecutiveConnErrors == 0) {
        // print stacktrace only for first error, no need to fill logs with pile of exactly the same exception traces
        LOG.error("Error while creating DB connection to url:" + dbUrl + ", user: " + user +
                      ", additional params: " + additionalConnectionParams, thr);
      } else {
        LOG.error("Error while creating DB connection to url:" + dbUrl + ", user: " + user +
                      ", additional params: " + additionalConnectionParams + " error message: " + thr.getMessage());
      }

      consecutiveConnErrors++;
      lastFailedTime = System.currentTimeMillis();
    }

    return dbConnection;
  }

  public void close() {
    LOG.info("Closing DBConnectionManager");

    if (dbConnection != null) {
      synchronized (dbConnection) {
        try {
          dbConnection.close();
        } catch (Throwable thr) {
          LOG.error("Error while closing connection for dbUrl: " + dbUrl + ", user: " + user, thr);
        } finally {
          dbConnection = null;
        }
      }
    }
  }

  public String getDbUrl() {
    return dbUrl;
  }

  public static synchronized DbConnectionManager getConnectionManager(String dbUrl) {
    return DB_CONNECTION_MANAGERS.get(dbUrl);
  }
}
