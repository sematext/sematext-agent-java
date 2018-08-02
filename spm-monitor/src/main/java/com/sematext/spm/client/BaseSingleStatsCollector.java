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

import static com.sematext.spm.client.util.CollectionUtils.iterator;

import java.util.Iterator;
import java.util.Map;

import com.sematext.spm.client.util.CollectionUtils;

/**
 * For stats collectors that provides only one value per stats request.
 */
public abstract class BaseSingleStatsCollector<T> extends StatsCollector<T> {
  public BaseSingleStatsCollector(StatValuesSerializer<T> serializer) {
    super(serializer);
  }

  /**
   * Collects statistics data. Stats data is appended.
   *
   * @param statValues holds stats data to append to
   * @throws com.sematext.spm.client.StatsCollectionFailedException when data collection fails
   */
  protected abstract void appendStats(StatValues statValues, Map<String, Object> outerMetrics)
      throws StatsCollectionFailedException;

  @Override
  protected Iterator<StatValues> getStatsSlice(Map<String, Object> outerMetrics) throws StatsCollectionFailedException {
    if (skipStats()) {
      return CollectionUtils.emptyIterator();
    }

    StatValues statValues = prepareStatValueInstance();
    appendStats(statValues, outerMetrics);
    return iterator(statValues);
  }

  /**
   * By default, no stats should be skipped. Specific subclasses can have their own logic where measured stats
   * in some cases should not be recorded (for instance, if they are invalid).
   *
   * @return true if current stats should not be recorded for some reason
   */
  protected boolean skipStats() {
    return false;
  }
}
