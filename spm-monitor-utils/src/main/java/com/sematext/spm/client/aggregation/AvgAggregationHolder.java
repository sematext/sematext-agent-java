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
package com.sematext.spm.client.aggregation;

import com.sematext.spm.client.SerializableMetricValue;

public abstract class AvgAggregationHolder<T extends Number> implements SerializableMetricValue {
  T sum;
  int count;

  public AvgAggregationHolder(T initialValue) {
    sum = initialValue;
    count = 1;
  }

  public void add(T newValue) {
    addInternal(newValue);
    count++;
  }

  protected abstract void addInternal(T newValue);

  public abstract T getAverage();

  public Class<?> getType() {
    return sum.getClass();
  }

  @Override
  public String serializeToInflux() {
    // needed for serialization
    T avg = getAverage();
    return avg != null ? avg.toString() : null;
  }
}
