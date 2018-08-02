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
package com.sematext.spm.client.unlogger.agg;

import java.util.Collection;
import java.util.Map;

import com.sematext.spm.client.unlogger.LogLine;
import com.sematext.spm.client.unlogger.annotations.NoGroup;

public class AsIsAggregator extends PointcutBasedAggregator {

  public AsIsAggregator(NoGroup config, String name) {
    this(name);
  }

  public AsIsAggregator(String name) {
    super(name);
  }

  @Override
  public void aggregate(LogLine row, Collection<LogLine> ungroupedSlice, Map<Object, Aggregate> groupedSlice) {
    ungroupedSlice.add(row);
  }

  @Override
  public String toString() {
    return "AsIsAggregator [getName()=" + getName() + "]";
  }

}
