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
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sematext.spm.client.observation.AttributeObservation;
import com.sematext.spm.client.observation.DerivedAttribute;
import com.sematext.spm.client.observation.ObservationBean;
import com.sematext.spm.client.observation.ObservationBeanDump;

public abstract class StatsExtractor<T extends StatsExtractorConfig<U>, U extends ObservationBean<?, ?>> {
  private static final Log LOG = LogFactory.getLog(StatsExtractor.class);

  private T config;

  public StatsExtractor(T config) {
    this.config = config;
  }

  /**
   * Extracts fresh metric stats.
   *
   * @return
   */
  public Map<String, Map<String, Object>> extractStats(Map<String, Object> outerMetrics)
      throws StatsCollectionFailedException {
    Map<String, Map<String, Object>> data = new UnifiedMap<String, Map<String, Object>>(1);

    // currently we have one instantiated observation per extractorConfig (when reading the config we
    // will have N observations per config - they act just as a template for creating real objects later;
    // when creating particular observation bean instance that is used for monitoring, we have only a single
    // observation per config). Check on this place will ensure no bugs get introduced here
    if (config.getObservations() != null && config.getObservations().size() > 1) {
      throw new StatsCollectionFailedException(
          "More than 1 observation found for config " + config + ", observations: " +
              config.getObservations());
    }

    for (U observation : config.getObservations()) {
      Set<ObservationBeanDump> stats = collectObservationStats(observation);
      for (ObservationBeanDump mBeanDump : stats) {
        data.put(mBeanDump.getName(), mBeanDump.getAttributes());

        if (observation.getDerivedAttributes() != null) {
          for (DerivedAttribute attr : observation.getDerivedAttributes()) {
            mBeanDump.getAttributes().put(attr.getName(), attr.apply(mBeanDump.getAttributes(), outerMetrics));
          }
        }
      }
    }
    return data;
  }

  public void removeOmitStats(Map<String, Map<String, Object>> dump) throws StatsCollectionFailedException {
    if (config.getObservations() == null || config.getObservations().size() == 0) {
      return;
    } else if (config.getObservations().size() > 1) {
      throw new StatsCollectionFailedException(
          "More than 1 observation found for config " + config + ", observations: " +
              config.getObservations());
    }

    // there is just one observation bean per stats extract config
    U observation = config.getObservations().get(0);

    if (observation.getOmitAttributes() != null && !observation.getOmitAttributes().isEmpty()) {
      for (Map<String, Object> stats : dump.values()) {
        for (String omit : observation.getOmitAttributes()) {
          stats.remove(omit);
        }
      }
    }
  }

  public Map<String, Map<String, Object>> extractStats(String observationPattern)
      throws StatsCollectionFailedException {
    Map<String, Map<String, Object>> data = new UnifiedMap<String, Map<String, Object>>();
    for (U observation : config.getObservations()) {
      if (!checkBeanShouldBeCollected(observationPattern, observation)) {
        continue;
      }
      Set<ObservationBeanDump> stats = collectObservationStats(observation);
      for (ObservationBeanDump mBeanDump : stats) {
        data.put(mBeanDump.getName(), mBeanDump.getAttributes());
      }
    }
    return data;
  }

  public List<String> getAttributeNames(String observationName) {
    List<String> attributeNames = null;
    for (U obsv : config.getObservations()) {
      if (checkBeanShouldBeCollected(observationName, obsv)) {
        attributeNames = new FastList<String>();
        for (AttributeObservation<?> attributeObservation : obsv.getAttributeObservations()) {
          attributeNames.add(attributeObservation.getAttributeName());
        }
      }
    }
    return attributeNames;
  }

  /**
   * Collects fresh metric stats for particular Observation.
   *
   * @param observation
   * @return
   */
  protected abstract Set<ObservationBeanDump> collectObservationStats(U observation)
      throws StatsCollectionFailedException;

  protected abstract boolean checkBeanShouldBeCollected(String observationName, U obsv);

  /**
   * Used to free resources used by particular extractor
   */
  public abstract void close();

  public T getConfig() {
    return config;
  }

  public void setConfig(T config) {
    this.config = config;
  }
}
