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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.config.CollectorFileConfig;
import com.sematext.spm.client.config.RequireDefinitionConfig;
import com.sematext.spm.client.observation.AttributeObservation;
import com.sematext.spm.client.observation.BeanConditionCheck;
import com.sematext.spm.client.observation.ObservationBean;
import com.sematext.spm.client.util.ReflectionUtils;

public abstract class StatsExtractorConfig<T extends ObservationBean> {
  private static final Log LOG = LogFactory.getLog(StatsExtractorConfig.class);

  private List<T> observations;
  private List<BeanConditionCheck> conditions;
  private CollectorFileConfig config;

  public StatsExtractorConfig(StatsExtractorConfig<T> origConfig, boolean createObservationDuplicates) {
    if (!createObservationDuplicates) {
      observations = new ArrayList<T>(origConfig.getObservations());
    } else {
      observations = new ArrayList<T>(origConfig.getObservations().size());
      for (T obs : origConfig.getObservations()) {
        try {
          observations.add((T) obs.getCopy());
        } catch (ConfigurationFailedException cfe) {
          LOG.error("Can't copy observation " + obs, cfe);
        }
      }
    }

    conditions = origConfig.conditions;
    config = origConfig.getConfig();
    copyFrom(origConfig);
  }

  public StatsExtractorConfig(CollectorFileConfig config, MonitorConfig monitorConfig)
      throws ConfigurationFailedException {
    readFields(config);
    this.config = config;
    updateConditionsAfterConstruction(monitorConfig);
    updateStateAfterConstruction(monitorConfig);
  }

  protected void updateStateAfterConstruction(MonitorConfig monitorConfig) {
    // default does nothing, but subclasses can change this
  }

  protected abstract void readFields(CollectorFileConfig config) throws ConfigurationFailedException;

  public StatsExtractorConfig(InputStream is, MonitorConfig monitorConfig) {
    updateConditionsAfterConstruction(monitorConfig);
  }

  private void updateConditionsAfterConstruction(MonitorConfig monitorConfig) {
    for (String key : CONDITIONS_MAP.keySet()) {
      CONDITIONS_MAP.get(key).setMonitorConfig(monitorConfig);
    }
  }

  public List<T> getObservations() {
    return observations;
  }

  public void setObservations(List<T> observations) {
    this.observations = observations;
  }

  public static <T extends StatsExtractorConfig<?>> T make(Class<T> type, InputStream config) {
    return ReflectionUtils.instance(type, InputStream.class, config);
  }

  protected abstract void copyFrom(StatsExtractorConfig<T> origConfig);

  public boolean conditionsSatisfied() {
    if (conditions != null) {
      for (BeanConditionCheck check : conditions) {
        if (!check.satisfies()) {
          return false;
        }
      }
    }
    return true;
  }

  private static final Map<String, BeanConditionCheck> CONDITIONS_MAP = new UnifiedMap<String, BeanConditionCheck>();

  protected void readConditions(CollectorFileConfig yamlConfig) throws ConfigurationFailedException {
    if (!yamlConfig.getRequire().isEmpty()) {
      this.conditions = new FastList<BeanConditionCheck>();

      for (RequireDefinitionConfig requireDef : yamlConfig.getRequire()) {
        String className = requireDef.getClassName();
        String requiredValues = requireDef.getValues();
        String countNeededMatches = requireDef.getCountNeededMatches();

        if (className.equals("") || requiredValues.equals("")) {
          throw new ConfigurationFailedException("Condition must be defined with 'className' (was: " + className +
                                                     ") and 'values' attribute");
        }

        synchronized (CONDITIONS_MAP) {
          // extractorConfig is also part of the key since each condition has extractorConfig property so we can't have them mix
          // --> one day we'll support monitoring of N apps with single agent process, at that point not having extractorConfig
          // as part of they key could cause issues
          String extractorConfigKey =
              getClass().getName() + "@" + Integer.toHexString(hashCode()); // based on Object.toString();
          String conditionKey = className + ":" + extractorConfigKey + ":" + requiredValues + ":" + countNeededMatches;
          BeanConditionCheck condition = CONDITIONS_MAP.get(conditionKey);

          if (condition == null) {
            try {
              Class c = Class.forName(className);
              condition = (BeanConditionCheck) c.newInstance();
              condition.setRequiredValuesString(requiredValues);
              if (countNeededMatches != null && !"".equals(countNeededMatches.trim())) {
                condition.setCountNeededMatches(Integer.valueOf(countNeededMatches.trim()));
              }
              condition.setExtractorConfig(this);

              // can't, monitorConfig is still not set at this point
              // condition.setMonitorConfig(monitorConfig);
              CONDITIONS_MAP.put(conditionKey, condition);
            } catch (Throwable thr) {
              LOG.error("Error while loading BeanConditionCheck : " + className, thr);
            }
          }

          if (condition != null) {
            conditions.add(condition);
          }
        }
      }
    }
  }

  public void replaceAttributeName(String observationName, String origAttribName, Object newAttr) {
    for (T obs : getObservations()) {
      if (obs.getName().equals(observationName)) {
        int position = 0;
        for (Object attr : obs.getAttributeObservations()) {
          // compare by reference is ok here
          if (((AttributeObservation) attr).getAttributeName().equals(origAttribName)) {
            break;
          }
          position++;
        }

        if (position >= obs.getAttributeObservations().size()) {
          throw new IllegalArgumentException(
              "Observation " + observationName + " doesn't contain attribute " + origAttribName +
                  ", so it can't be replaced!");
        }
        obs.getAttributeObservations().set(position, newAttr);
      }
    }
  }

  // final since we depend on it for key calculation above
  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  public CollectorFileConfig getConfig() {
    return config;
  }
}
