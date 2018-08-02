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
package com.sematext.spm.client.tracing;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import com.sematext.spm.client.GenericExtractor;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.MultipleStatsCollector;
import com.sematext.spm.client.Serializer;
import com.sematext.spm.client.StatValues;
import com.sematext.spm.client.StatValuesHelper;
import com.sematext.spm.client.tracing.agent.stats.ExternalCall;

public class TracingExternalCallStatsCollector extends MultipleStatsCollector<ExternalCall> {
  private final String appToken;
  private final String jvmName;
  private final String subType;
  private final File propsFile;
  private final String finalJvmName;

  public TracingExternalCallStatsCollector(Serializer serializer, String appToken, String jvmName, String subType) {
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
  }

  @Override
  protected Collection<ExternalCall> getSlice(Map<String, Object> outerMetrics) {
    return TracingStatsExtractors.callStatisticsView().externalCalls();
  }

  @Override
  protected void appendStats(ExternalCall call, StatValues statValues) {
    statValues.setMetrics(new UnifiedMap<String, Object>());
    statValues.getMetrics().put("external.call.duration", call.duration());
    statValues.getMetrics().put("external.calls.count", call.callsCount());
    statValues.setTags(new UnifiedMap<String, String>());
    statValues.getTags().put(GenericExtractor.JVM_NAME_TAG, finalJvmName);
    statValues.getTags().put("tracing.external.call.destination", call.dstHostname());
    statValues.getTags().put("tracing.external.call.tag", call.tag());

    StatValuesHelper.fillHostTags(statValues, propsFile);
    statValues.setTimestamp(System.currentTimeMillis());
    statValues.setAppToken(appToken);
    statValues.setMetricNamespace("tracing");
  }

  @Override
  public String getName() {
    return "tracing-external-call";
  }

  @Override
  public String getCollectorIdentifier() {
    return TracingExternalCallStatsCollector.class.getName();
  }
}
