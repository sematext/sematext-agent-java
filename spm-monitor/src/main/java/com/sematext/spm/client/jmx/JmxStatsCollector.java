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
package com.sematext.spm.client.jmx;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.MultipleStatsCollector;
import com.sematext.spm.client.Serializer;
import com.sematext.spm.client.StatValues;
import com.sematext.spm.client.StatsCollectionFailedException;

/**
 * This collector knows nothing about the JMX beans it's collecting from, stats are collected in the same order that
 * they appear in the JmxStatsExtractor.
 */
public class JmxStatsCollector extends MultipleStatsCollector<Map<String, Object>> {

  private final JmxStatsExtractor statsExtractor;
  private final String name;

  /**
   * used when extractor name differs from name which should be used as key in collector output (
   * for instance, when wildcards are used in bean name definition - check hadoop-resourcemanager conf)
   */
  private final String outputEntryName;

  private final List<String> attributeNames;

  public JmxStatsCollector(String name, JmxStatsExtractor statsExtractor) {
    this(name, statsExtractor, Serializer.INFLUX);
  }

  public JmxStatsCollector(String name, JmxStatsExtractor statsExtractor, Serializer serializer) {
    super(serializer);
    this.name = name;
    this.statsExtractor = statsExtractor;
    this.attributeNames = statsExtractor.getAttributeNames(name);
    this.outputEntryName = null;
  }

  public JmxStatsCollector(String name, String outputEntryName, JmxStatsExtractor statsExtractor) {
    super(Serializer.TAB);
    this.name = name;
    this.statsExtractor = statsExtractor;
    this.attributeNames = statsExtractor.getAttributeNames(name);
    this.outputEntryName = outputEntryName;
  }

  @Override
  protected Collection<Map<String, Object>> getSlice(Map<String, Object> outerMetrics)
      throws StatsCollectionFailedException {
    Map<String, Map<String, Object>> stats = statsExtractor.extractStats(name);
    if (stats == null) {
      throw new StatsCollectionFailedException("JMX bean " + name + " not found");
    }

    return stats.values();
  }

  @Override
  protected void appendStats(Map<String, Object> protoStats, StatValues statValues) {
    for (String attribute : attributeNames) {
      statValues.add(protoStats.get(attribute));
    }
  }

  @Override
  public String getName() {
    if (outputEntryName != null) {
      return outputEntryName;
    } else {
      return name;
    }
  }

  public String toString() {
    return "JmxStatsCollector: name: " + name;
  }

  @Override
  public String getCollectorIdentifier() {
    // dummy default implementation, should be overriden in subclasses only when needed
    return getName();
  }

}
