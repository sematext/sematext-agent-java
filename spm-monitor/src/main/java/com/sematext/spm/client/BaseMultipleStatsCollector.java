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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.sematext.spm.client.util.CollectionUtils;
import com.sematext.spm.client.util.CollectionUtils.Function;

/**
 * For stats collectors that can gather multiple StatsValues between stats requests.
 */
public abstract class BaseMultipleStatsCollector<P, T> extends StatsCollector<T> {
  public BaseMultipleStatsCollector(StatValuesSerializer<T> serializer) {
    super(serializer);
  }

  protected abstract Collection<P> getSlice(Map<String, Object> outerMetrics) throws StatsCollectionFailedException;

  protected abstract void appendStats(P protoStats, StatValues statValues);

  protected Iterator<StatValues> getStatsSlice(Map<String, Object> outerMetrics) throws StatsCollectionFailedException {
    return CollectionUtils.transform(getSlice(outerMetrics).iterator(), new Function<P, StatValues>() {
      @Override
      public StatValues apply(P proto) {
        StatValues statValues = prepareStatValueInstance();
        appendStats(proto, statValues);
        return statValues;
      }

    });
  }

}
