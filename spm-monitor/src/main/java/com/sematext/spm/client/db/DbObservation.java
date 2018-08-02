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
package com.sematext.spm.client.db;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.StatsCollectionFailedException;
import com.sematext.spm.client.config.ObservationDefinitionConfig;
import com.sematext.spm.client.observation.ObservationBean;
import com.sematext.spm.client.observation.ObservationBeanDump;

public class DbObservation extends ObservationBean<DbAttributeObservation, Map<String, Object>> {
  private static final Log LOG = LogFactory.getLog(DbAttributeObservation.class);

  private String beanName;

  // used when instantiating a "real" DbObservation object for some particular db bean (resulting object is not just a 
  // config holder anymore)
  public DbObservation(DbObservation orig, String beanName) {
    super(orig, Collections.EMPTY_MAP);
    this.beanName = beanName;
  }

  public DbObservation(ObservationDefinitionConfig observationDefinition) throws ConfigurationFailedException {
    super(observationDefinition);
  }

  public ObservationBean getCopy() throws ConfigurationFailedException {
    return new DbObservation(this.observationDefinition);
  }

  @Override
  public Set<ObservationBeanDump> collectStats(Map<String, Object> data) {
    ObservationBeanDump stats = new ObservationBeanDump(beanName);
    for (DbAttributeObservation attributeObservation : getAttributeObservations()) {
      Object value = null;
      try {
        Map<String, ?> context = Collections.emptyMap();
        value = attributeObservation.getValue(this, data, context);
      } catch (StatsCollectionFailedException e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Failed to extract stats value, attribute name: " + attributeObservation.getAttributeName() +
                        ", message: " + e.getMessage());
        }
      } catch (RuntimeException re) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Failed to extract stats value, attribute name: " + attributeObservation.getAttributeName(), re);
        }
      }
      stats.setAttribute(attributeObservation.getFinalName(), value);
    }

    Set<ObservationBeanDump> res = new HashSet<ObservationBeanDump>();
    res.add(stats);

    return res;
  }

  @Override
  public void read(ObservationDefinitionConfig observationDefinition) throws ConfigurationFailedException {
    beanName = observationDefinition.getName();
    if (beanName == null || "".equals(beanName.toString().trim())) {
      throw new ConfigurationFailedException("Observation missing required attribute 'name'");
    }

    readAttributeObservations(observationDefinition);
    readTagDefinitions(observationDefinition);
    // readAttributeNameMappings(observationDefinition);
    readIgnoreElements(observationDefinition);
    readAcceptElements(observationDefinition);
  }

  @Override
  public String getName() {
    return beanName;
  }

  @Override
  protected DbAttributeObservation createAttributeObservation(String type) {
    if ("counter".equals(type)) {
      return new DbRealCounterAttribute();
    } else if ("text".equals(type)) {
      return new DbTextAttribute();
    } else if ("long_gauge".equals(type)) {
      return new DbLongGaugeAttribute();
    } else if ("double_gauge".equals(type)) {
      return new DbDoubleGaugeAttribute();
    } else if ("change".equals(type)) {
      return new DbValueChangeAttribute();
    } else {
      LOG.error("Unknown attribute observation type: " + type);
      return null;
    }
  }
}
