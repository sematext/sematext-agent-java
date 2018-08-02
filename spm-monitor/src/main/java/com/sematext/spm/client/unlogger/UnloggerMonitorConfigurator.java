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
package com.sematext.spm.client.unlogger;

import java.util.Collection;
import java.util.Map;

import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.MonitorConfigurator;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.StatsCollector;

public class UnloggerMonitorConfigurator extends MonitorConfigurator {

  private final Class<? extends BaseUnloggerStatsCollector>[] collectorTypes;

  public UnloggerMonitorConfigurator(Class<? extends BaseUnloggerStatsCollector>... collectorTypes) {
    this.collectorTypes = collectorTypes;
  }

  @Override
  public void configure(Map<String, String> paramsMap, MonitorConfig monitorConfig,
                        Collection<? extends StatsCollector<?>> currentCollectors,
                        Collection<? super StatsCollector<?>> newCollectors) throws Exception {
    if (BaseUnloggerStatsCollector.Config.isHardOff(paramsMap)) {
      return;
    }

    if (!MonitorUtil.MONITOR_RUNTIME_SETUP_JAVAAGENT.get()) {
      // can't be used with standalone monitors
      return;
    }

    newCollectors.addAll(findOrCreateNewByType(currentCollectors, paramsMap, "", collectorTypes));
    if (BaseUnloggerStatsCollector.Config.isDynamicOff(paramsMap)) {
      BaseUnloggerStatsCollector.dynamicOffAllPointcuts();
    } else {
      BaseUnloggerStatsCollector.dynamicOnAllPointcuts();
    }
  }
}
