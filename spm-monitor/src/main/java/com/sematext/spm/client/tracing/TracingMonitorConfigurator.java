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

import java.util.List;

import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.Serializer;
import com.sematext.spm.client.StatsCollector;
import com.sematext.spm.client.StatsCollectorBadConfigurationException;
import com.sematext.spm.client.StatsCollectorsFactory;
import com.sematext.spm.client.util.CollectionUtils.FunctionT;

public final class TracingMonitorConfigurator {
  public void configure(final MonitorConfig monitorConfig, final List<? extends StatsCollector<?>> currentCollectors,
                        final List<StatsCollector<?>> collectors,
                        final Serializer serializer, final String appToken, final String subType, final String jvmName)
      throws StatsCollectorBadConfigurationException {
    StatsCollectorsFactory.updateCollector(currentCollectors, collectors, TracingReqCompStatsCollector.class,
                                           TracingReqCompStatsCollector.class.getName(),
                                           new FunctionT<String, TracingReqCompStatsCollector, StatsCollectorBadConfigurationException>() {
                                             @Override
                                             public TracingReqCompStatsCollector apply(String id) {
                                               return new TracingReqCompStatsCollector(serializer, appToken, jvmName, subType);
                                             }
                                           });

    StatsCollectorsFactory.updateCollector(currentCollectors, collectors, TracingReqStatsCollector.class,
                                           TracingReqStatsCollector.class.getName(),
                                           new FunctionT<String, TracingReqStatsCollector, StatsCollectorBadConfigurationException>() {
                                             @Override
                                             public TracingReqStatsCollector apply(String id) {
                                               return new TracingReqStatsCollector(serializer, appToken, jvmName, subType, monitorConfig);
                                             }
                                           });

    StatsCollectorsFactory.updateCollector(currentCollectors, collectors, TracingCrossAppCallStatsCollector.class,
                                           TracingCrossAppCallStatsCollector.class.getName(),
                                           new FunctionT<String, TracingCrossAppCallStatsCollector, StatsCollectorBadConfigurationException>() {
                                             @Override
                                             public TracingCrossAppCallStatsCollector apply(String id) {
                                               return new TracingCrossAppCallStatsCollector(serializer, appToken, jvmName, subType);
                                             }
                                           });

    StatsCollectorsFactory.updateCollector(currentCollectors, collectors, TracingExternalCallStatsCollector.class,
                                           TracingExternalCallStatsCollector.class.getName(),
                                           new FunctionT<String, TracingExternalCallStatsCollector, StatsCollectorBadConfigurationException>() {
                                             @Override
                                             public TracingExternalCallStatsCollector apply(String id) {
                                               return new TracingExternalCallStatsCollector(serializer, appToken, jvmName, subType);
                                             }
                                           });

    StatsCollectorsFactory.updateCollector(currentCollectors, collectors, TracingReqErrorsStatsCollector.class,
                                           TracingReqErrorsStatsCollector.class.getName(),
                                           new FunctionT<String, TracingReqErrorsStatsCollector, StatsCollectorBadConfigurationException>() {
                                             @Override
                                             public TracingReqErrorsStatsCollector apply(String id) {
                                               return new TracingReqErrorsStatsCollector(serializer, appToken, jvmName, subType);
                                             }
                                           });

    StatsCollectorsFactory.updateCollector(currentCollectors, collectors, TracingDatabaseOperationStatsCollector.class,
                                           TracingDatabaseOperationStatsCollector.class.getName(),
                                           new FunctionT<String, TracingDatabaseOperationStatsCollector, StatsCollectorBadConfigurationException>() {
                                             @Override
                                             public TracingDatabaseOperationStatsCollector apply(String id) {
                                               return new TracingDatabaseOperationStatsCollector(serializer, appToken, jvmName, subType);
                                             }
                                           });
  }
}
