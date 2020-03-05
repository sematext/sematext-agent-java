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
package com.sematext.spm.client.jvm;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.GenericExtractor;
import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.MultipleStatsCollector;
import com.sematext.spm.client.Serializer;
import com.sematext.spm.client.StatValues;
import com.sematext.spm.client.StatValuesHelper;
import com.sematext.spm.client.jmx.JmxServiceContext;
import com.sematext.spm.client.metrics.PercentilesMetricsProcessor;

public class JvmNotifBasedGcStatsCollector
    extends MultipleStatsCollector<JvmNotifBasedGcStatsExtractor.GcAdditionalStats> {
  private static final Log LOG = LogFactory.getLog(JvmNotifBasedGcStatsCollector.class);
  private final String appToken;
  private final String jvmName;
  private final String subType;
  private final File propsFile;
  private final String finalJvmName;

  private final JvmNotifBasedGcStatsExtractor statsExtractorNotifBased;

  public JvmNotifBasedGcStatsCollector(Serializer serializer, String appToken, String jvmName, String subType) {
    super(serializer);
    this.appToken = appToken;
    this.jvmName = jvmName;
    this.subType = subType;
    if (subType == null || subType.trim().equals("")) {
      this.finalJvmName = jvmName;
    } else {
      this.finalJvmName = jvmName + "-" + subType;
    }

    this.propsFile = MonitorUtil.fetchSpmMonitorPropertiesFileObject(appToken, jvmName, subType);
    this.statsExtractorNotifBased = new JvmNotifBasedGcStatsExtractor(JmxServiceContext.getContext(propsFile));
    LOG.info("GC notification based stats collector for '" + jvmName + "' initialized");
  }

  private int currentSliceSize = 0;
  private int currentElementIndex = 0;
  private long maxDuration = 0;
  private long totalCollectionSize = 0;
  private List<Object> collectionDurations = new ArrayList<Object>(20);

  @Override
  protected Collection<JvmNotifBasedGcStatsExtractor.GcAdditionalStats> getSlice(Map<String, Object> outerMetrics) {
    Collection<JvmNotifBasedGcStatsExtractor.GcAdditionalStats> res = statsExtractorNotifBased.getStats();

    currentSliceSize = res.size();

    return res;
  }

  @Override
  protected synchronized void appendStats(JvmNotifBasedGcStatsExtractor.GcAdditionalStats protoStats,
                                          StatValues statValues) {
    maxDuration = Math.max(maxDuration, protoStats.getDuration());
    totalCollectionSize += protoStats.getCollectSize();
    collectionDurations.add(protoStats.getDuration());

    if (currentElementIndex == currentSliceSize - 1) {
      // the last one, append the stats      
      statValues.setMetrics(new UnifiedMap<String, Object>());
      statValues.getMetrics().put("gc.collection.time.max", maxDuration);
      statValues.getMetrics().put("gc.collection.size.avg", (long) (((double) totalCollectionSize) / currentSliceSize));
      statValues.getMetrics().put("gc.collection.size.total", totalCollectionSize);

      statValues.getMetrics()
          .put("gc.collection.time.pctl.50", PercentilesMetricsProcessor.calculatePctl(collectionDurations, 50l));
      statValues.getMetrics()
          .put("gc.collection.time.pctl.95", PercentilesMetricsProcessor.calculatePctl(collectionDurations, 95l));
      statValues.getMetrics()
          .put("gc.collection.time.pctl.99", PercentilesMetricsProcessor.calculatePctl(collectionDurations, 99l));

      statValues.setTags(new UnifiedMap<String, String>());
      statValues.getTags().put(GenericExtractor.JVM_NAME_TAG, finalJvmName);
      statValues.getTags().put("jvm.gc", protoStats.getGcName());

      StatValuesHelper.fillEnvTags(statValues, propsFile);
      StatValuesHelper.fillConfigTags(statValues, MonitorUtil.loadMonitorProperties(propsFile));
      statValues.setTimestamp(protoStats.getTimestamp());
      statValues.setAppToken(appToken);
      statValues.setMetricNamespace("jvm");

      // reset the state after done
      currentElementIndex = 0;
      maxDuration = 0;
      totalCollectionSize = 0;
      collectionDurations.clear();
    } else {
      currentElementIndex++;
    }
  }

  @Override
  public String getName() {
    return "jvmgd";
  }

  @Override
  public String getCollectorIdentifier() {
    return jvmName;
  }
}
