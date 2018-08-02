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

import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.util.ReflectionUtils;

public abstract class MonitorConfigurator {

  private static final Log LOG = LogFactory.getLog(MonitorConfigurator.class);

  /**
   * @param paramsMap
   * @param currentCollectors
   * @param newCollectors
   * @throws Exception
   */
  public abstract void configure(Map<String, String> paramsMap, MonitorConfig monitorConfig,
                                 Collection<? extends StatsCollector<?>> currentCollectors,
                                 Collection<? super StatsCollector<?>> newCollectors) throws Exception;

  private static <T extends StatsCollector<?>> T findExistingCollectorOrCreateNew(Class<T> claxx, String id,
                                                                                  Collection<? extends StatsCollector<?>> existedCollectors,
                                                                                  Map<String, String> params) {
    T existed = findExistingCollector(claxx, id, existedCollectors);
    if (existed != null) {
      return existed;
    }

    T newCollector = ReflectionUtils.instance(claxx, Map.class, params);
    if (newCollector != null) {
      return newCollector;
    }
    return ReflectionUtils.instance(claxx);
  }

  protected static Collection<StatsCollector<?>> findOrCreateNewByType(
      Collection<? extends StatsCollector<?>> currentCollectors, Map<String, String> params, String id,
      Class<? extends StatsCollector<?>>... collectorTypes) {
    List<StatsCollector<?>> collectors = new FastList<StatsCollector<?>>();
    for (Class<? extends StatsCollector<?>> collectorType : collectorTypes) {
      StatsCollector<?> collector = findExistingCollectorOrCreateNew(collectorType, id, currentCollectors, params);
      if (collector == null) {
        LOG.error("Can't instantiate collector, of type: " + collector + ", id: " + id + ", params: " + params);
        continue;
      }
      collectors.add(collector);
    }
    LOG.info("New collectors successfully created");
    return collectors;
  }

  protected static <T extends StatsCollector<?>> T findExistingCollector(Class<T> class1, String id,
                                                                         Collection<? extends StatsCollector<?>> currentCollectors) {

    if (currentCollectors == null) {
      return null;
    }

    String calculatedId = StatsCollector.calculateIdForCollector(class1, id);

    for (StatsCollector<?> c : currentCollectors) {
      if (class1.equals(c.getClass()) && calculatedId.equals(c.getId())) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("For calculated ID " + calculatedId + " FOUND existing collector");
        }
        return (T) c;
      }
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("For calculated ID " + calculatedId + " found NO existing collectors");
    }
    return null;
  }

}
