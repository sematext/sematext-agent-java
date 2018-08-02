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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.sematext.spm.client.util.CollectionUtils.FunctionT;

/**
 * Stats collectors factory
 */
public abstract class StatsCollectorsFactory<K extends StatsCollector> {
  private static final Log LOG = LogFactory.getLog(StatsCollectorsFactory.class);

  Collection<? extends K> createCollectors(Properties monitorProperties,
                                           List<? extends K> currentCollectors, MonitorConfig monitorConfig)
      throws StatsCollectorBadConfigurationException {
    boolean printInitiallyCreatedCollectors = (currentCollectors == null || currentCollectors.size() == 0);

    Collection<? extends K> newCollectors = create(monitorProperties, currentCollectors, monitorConfig);

    if (newCollectors == null) {
      newCollectors = Collections.emptyList();
    }

    if (printInitiallyCreatedCollectors) {
      if (newCollectors.size() < 1000) {
        LOG.info("Initially created collectors: " + newCollectors);
      }
    }

    return newCollectors;
  }

  /**
   * Creates {@link StatsCollector}s
   *
   * @return collection of {@link StatsCollector}s
   * @throws StatsCollectorBadConfigurationException if configuration is bad
   */
  public abstract Collection<? extends K> create(Properties monitorProperties, List<? extends K> currentCollectors,
                                                 MonitorConfig monitorConfig)
      throws StatsCollectorBadConfigurationException;

  /**
   * This method can return null.
   */
  protected static <T extends StatsCollector> T findExistingCollector(
      Collection<? extends StatsCollector> currentCollectors, Class<T> class1, String id) {

    if (currentCollectors == null) {
      return null;
    }

    String calculatedId = StatsCollector.calculateIdForCollector(class1, id);

    for (StatsCollector c : currentCollectors) {
      if (class1.equals(c.getClass()) && calculatedId.equals(c.getId())) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("For calculated ID " + calculatedId + " FOUND existing collector " + c);
        }
        return (T) c;
      }
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("For calculated ID " + calculatedId + " found NO existing collectors");
    }
    return null;
  }

  public static <T extends StatsCollector<?>, E extends Exception> void updateCollector(
      Collection<? extends StatsCollector<?>> currentCollectors, Collection<? super StatsCollector<?>> newCollectors,
      Class<T> claxx, String id, FunctionT<String, ? extends T, E> factory) throws E {
    T existing = findExistingCollector(currentCollectors, claxx, id);

    if (existing != null) {
      newCollectors.add(existing);
    } else {
      StatsCollector<?> tmp = factory.apply(id);

      if (tmp != null) {
        newCollectors.add(tmp);
      }
    }
  }

  public static <T extends StatsCollector<?>, E extends Exception> void updateCollectors(
      Collection<? extends StatsCollector<?>> currentCollectors, Collection<? super StatsCollector<?>> newCollectors,
      Class<T> claxx, Collection<String> ids, FunctionT<String, ? extends T, E> factory) throws E {
    for (String id : ids) {
      updateCollector(currentCollectors, newCollectors, claxx, id, factory);
    }
  }

}
