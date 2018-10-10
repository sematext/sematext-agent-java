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
package com.sematext.spm.client;

import static com.sematext.spm.client.util.CollectionUtils.emptyIterator;

import java.lang.instrument.Instrumentation;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.util.CollectionUtils;
import com.sematext.spm.client.util.CollectionUtils.Function;

/**
 * Performs stats data collection. Defines base interface for plug-able stats collectors.
 */
public abstract class StatsCollector<T> {
  private static final Log LOG = LogFactory.getLog(StatsCollector.class);

  private final StatValuesSerializer<T> serializer;

  private String collectorId = null;

  protected StatsCollector(StatValuesSerializer<T> serializer) {
    this.serializer = serializer;
  }

  public Iterator<T> collect(Map<String, Object> outerMetrics) {
    try {
      return CollectionUtils.transform(getStatsSlice(outerMetrics), new Function<StatValues, T>() {

        @Override
        public T apply(StatValues statValues) {
          if (statValues.isNullRow()) {
            return null;
          } else {
            T tmp = serialize(statValues);

            return tmp;
          }
        }
      });

    } catch (StatsCollectionFailedException e) {
      if (e instanceof MonitoredServiceUnavailableException) {
        // no need to print the full trace in this case
        LOG.error("Data collection failed, collector name: " + getName() + ", message: " + e.getMessage());
      } else {
        LOG.error("Data collection failed, collector name: " + getName(), e);
      }

      return emptyIterator();
    }

  }

  public Iterator<StatValues> collectRawStatValues(Map<String, Object> outerMetrics)
      throws StatsCollectionFailedException {
    return getStatsSlice(outerMetrics);
  }

  protected abstract Iterator<StatValues> getStatsSlice(Map<String, Object> outerMetrics)
      throws StatsCollectionFailedException;

  protected T serialize(StatValues statValues) {
    try {
      return statValues.getAsSerialized(serializer);
    } catch (Throwable thr) {
      LOG.error("Error while serializing data for " + this, thr);
      return null;
    }
  }

  public final StatValues prepareStatValueInstance() {
    StatValues statValues = new StatValues();
    if (serializer.shouldGeneratePrefix()) {
      statValues.add(getName());
      statValues.add(System.currentTimeMillis());
    }
    return statValues;
  }

  /**
   * Gets collector name. It is used to correlate log record with parsing logic
   *
   * @return collector name
   */
  public abstract String getName();

  /**
   * Performs optional setup
   *
   * @param instr instrumentation from preMain method
   */
  public void init(Instrumentation instr) {
    // DO NOTHING by default
  }

  /**
   * Performs resources cleanup.
   */
  public void cleanup() {
    // DO NOTHING by default
  }

  public void update(Map<String, String> params) {
    // DO NOTHING by default
  }

  /**
   * All stats collectors should implement so one can find out what exactly some collector is collecting. In case of
   * core related stats collectors, they can simply return the core name here. In case of search handler related
   * handlers, coreName + handlerName will be good enough. In case of JVM related stats collectors, like
   * JvmThreadStatsCollector, empty string will be fine, since there should be only one such collector inside of JVM.
   * <p/>
   * This method shouldn't be used directly, use getId() instead!!
   *
   * @return
   */
  public abstract String getCollectorIdentifier();

  /**
   * Method which generate collector's ID which can be used for comparison.
   *
   * @return
   */
  public final String getId() {
    if (collectorId != null) {
      return collectorId;
    } else {
      collectorId = StatsCollector.calculateIdForCollector(this.getClass(), getCollectorIdentifier());
      return collectorId;
    }
  }

  /**
   * Can be used to determine which ID some stats collector would have. It can be compared to the output of getId()
   * method of some particular stats collector to see if that collector matches what is asked.
   *
   * @param collectorClass
   * @param collectorId
   * @return
   */
  public static final String calculateIdForCollector(Class<?> collectorClass, String collectorId) {
    return collectorId;
  }

  /**
   * Subclasses should provide info whether the measurement can be ignore by the monitor if all Number metrics
   * were recorded as 0. By default, "false" is assumed (to be on the safe side).
   *
   * @return
   */
  public boolean ignorableIfZeroRow() {
    return false;
  }

  /**
   * Subclasses should provide info whether the measurement can be ignore by the monitor if there is no
   * non-null numeric value. By default, "true" is assumed (unlike with zero rows, where one has to
   * analyze collector in detail to decide whether it can be ignored when all numbers have 0 values, in case
   * of null values, ignoring is default behavior. This is important for example for optimization related
   * to counters, where their values are recorded only once in a minute and otherwise get null value. Without
   * default return value "true", such rows wouldn't be ignored by default).
   *
   * @return
   */
  public boolean ignorableIfNullRow() {
    return true;
  }

  public boolean producesMetricsAndTagsMaps() {
    return false;
  }

  public StatValuesSerializer<T> getSerializer() {
    return serializer;
  }

  public int getCollectorsCount() {
    // typically just a single collector, unless we have a case of composite collector like GroupedByTagsCollector
    return 1;
  }

  public static int getCollectorsCount(List<? extends StatsCollector> collectors) {
    int count = 0;

    for (StatsCollector sc : collectors) {
      count += sc.getCollectorsCount();
    }

    return count;
  }
}
