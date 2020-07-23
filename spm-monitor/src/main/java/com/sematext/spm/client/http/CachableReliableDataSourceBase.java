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
package com.sematext.spm.client.http;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.json.JsonDataSourceCachedFactory;
import com.sematext.spm.client.status.AgentStatusRecorder;
import com.sematext.spm.client.status.AgentStatusRecorder.ConnectionStatus;

public class CachableReliableDataSourceBase<T, D extends DataProvider<T>> {
  private static final Log LOG = LogFactory.getLog(CachableReliableDataSourceBase.class);

  private static final long MAX_SUCCESSIVE_FAILED_TRIES = 5;
  private static final long ERROR_STATE_INACTIVITY_PAUSE = 1 * 60 * 1000; // 1 minute

  private long lastDataFetchTime = 0L;

  private final CacheableData<T> data = new CacheableData<T>();

  private D dataProvider;

  // in case data source encountered an error while executing request, it will be moved to inactive state
  // which means that particular DataSource and its StatsCollectors will be ignored; after the inactivity
  // period lasting ERROR_STATE_INACTIVITY_PAUSE ms passes, the DataSource will again be tested for
  // MAX_SUCCESSIVE_FAILED_TRIES attempts.
  private boolean active = true;
  private long inactivityPeriodStartTime = 0L;
  private int successiveFailedTries = 0;
  private boolean async = false;

  public CachableReliableDataSourceBase(D dataProvider, boolean async, ServerInfo serverInfo) {
    this.async = async;
    this.dataProvider = dataProvider;

    if (async) {
      createAsyncDataFetchThread(serverInfo);
    }
  }

  private void createAsyncDataFetchThread(final ServerInfo serverInfo) {
    LOG.info("Creating Async DataFetch Thread for " + dataProvider);

    new Thread(new Runnable() {
      @Override
      public void run() {
        while (true) {
          doFetchData();

          // sleep before next fetch
          try {
            AtomicInteger delay = JsonDataSourceCachedFactory.ASYNC_DATA_SOURCE_REFRESH_INTERVAL_MS_MAP
                .get(serverInfo.getId());
            if (delay != null && delay.intValue() > 0) {
              Thread.sleep(delay.intValue());
            } else {
              Thread.sleep(9500);
            }
          } catch (InterruptedException e) {
            LOG.error("Sleeping interrupted", e);
          }
        }
      }
    }).start();
  }

  public T fetchData() {
    if (!active) {
      long currentTime = System.currentTimeMillis();

      // if inactivity period passed, activate the datasource again
      if ((currentTime - inactivityPeriodStartTime) > ERROR_STATE_INACTIVITY_PAUSE) {
        activate();
      } else {
        return null;
      }
    }

    // if refresh interval still hasn't passed, return existing data
    if (!((System.currentTimeMillis() - lastDataFetchTime) > getFreshDataTimeoutMs())) {
      return data.getData();
    }

    if (!async) {
      doFetchData();
    }

    return data.getData();
  }

  private long getFreshDataTimeoutMs() {
    // we'll use cache interval that is somewhat shorter than collection interval -> this should
    // be enough for all collectors during single measurement to use the same result (via cache)
    // and to avoid next measurement using stale data
    long collectInterval = MonitorConfig.GLOBAL_MONITOR_COLLECT_INTERVAL_MS;
    return (long) (collectInterval * 0.75d);
  }

  private synchronized void doFetchData() {
    // if refresh interval still hasn't passed, return existing data
    if (!((System.currentTimeMillis() - lastDataFetchTime) > getFreshDataTimeoutMs())) {
      return;
    }

    try {
      T newData = dataProvider.getData();

      if (newData == null) {
        AgentStatusRecorder.GLOBAL_INSTANCE.updateConnectionStatus(ConnectionStatus.FAILED, "no data in response");
        
        LOG.error("Error while fetching data with " + this.getClass().getCanonicalName() +
                      " for " + dataProvider + " - no String with response data could be created.");

        recordFailedRequest();
        data.setData(null);
        return;
      }

      lastDataFetchTime = System.currentTimeMillis();

      successiveFailedTries = 0;

      data.setData(newData);

      if (AgentStatusRecorder.GLOBAL_INSTANCE != null) {
        AgentStatusRecorder.GLOBAL_INSTANCE.updateConnectionStatus(ConnectionStatus.OK);
      }

      return;
    } catch (Throwable thr) {
      if (AgentStatusRecorder.GLOBAL_INSTANCE != null) {
        AgentStatusRecorder.GLOBAL_INSTANCE.updateConnectionStatus(ConnectionStatus.FAILED, thr.getMessage());
      }
      
      LOG.error("Error while fetching data with " + this.getClass().getCanonicalName() +
                    " for " + dataProvider + ". Message: " + thr.getMessage());

      if (LOG.isDebugEnabled()) {
        LOG.debug(thr);
      }

      recordFailedRequest();
      data.setData(null);
      return;
    }
  }

  protected void activate() {
    LOG.info("Activating data source for " + dataProvider);
    active = true;
    inactivityPeriodStartTime = 0L;
    successiveFailedTries = 0;
  }

  protected void recordFailedRequest() {
    successiveFailedTries++;
    data.setData(null);
    lastDataFetchTime = System.currentTimeMillis();

    if (successiveFailedTries >= MAX_SUCCESSIVE_FAILED_TRIES) {
      LOG.error("Max allowed successive failed calls reached " + successiveFailedTries + " for " +
                    dataProvider + ". Setting data collector to inactive state for " + ERROR_STATE_INACTIVITY_PAUSE +
                    " ms");

      active = false;
      inactivityPeriodStartTime = System.currentTimeMillis();
      successiveFailedTries = 0;
    }
  }

  public boolean isActive() {
    if (!active) {
      long currentTime = System.currentTimeMillis();

      // if inactivity period passed, activate the datasource again
      if ((currentTime - inactivityPeriodStartTime) > ERROR_STATE_INACTIVITY_PAUSE) {
        activate();
      }
    }

    return active;
  }

  public void setFreshData(T freshData) {
    data.setData(freshData);
  }

  public boolean isAsync() {
    return async;
  }

  public void close() {
    dataProvider.close();
  }

  public D getDataProvider() {
    return dataProvider;
  }

  @Override
  public String toString() {
    return "CachableReliableDataSourceBase{dataProvider=" + dataProvider + '}';
  }
}
