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
package com.sematext.spm.client.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectorFileConfig {
  public enum ConfigType {
    JMX,
    JSON,
    DB
  }

  private static final Map<String, ConfigType> CONFIG_TYPES = new HashMap<String, ConfigType>(
      ConfigType.values().length);

  private static final String ALLOWED_CONFIG_TYPES;

  static {
    for (ConfigType mt : ConfigType.values()) {
      CONFIG_TYPES.put(mt.name().toLowerCase(), mt);
    }

    String allowedConfigTypes = "(";
    int counter = 0;
    for (String allowed : CONFIG_TYPES.keySet()) {
      allowedConfigTypes += ((counter > 0) ? ", " : "") + allowed;
      counter++;
    }
    allowedConfigTypes += ")";
    ALLOWED_CONFIG_TYPES = allowedConfigTypes;
  }

  private ConfigType type;
  private DataConfig data;
  private List<RequireDefinitionConfig> require = Collections.EMPTY_LIST;
  private List<ObservationDefinitionConfig> observation = Collections.EMPTY_LIST;

  public ConfigType getType() {
    return type;
  }

  public void setType(String type) {
    String tmp = type.trim().toLowerCase();
    this.type = CONFIG_TYPES.get(tmp);
    if (this.type == null) {
      throw new IllegalArgumentException(
          "Unrecognized config type: " + type + ", allowed types are: " + ALLOWED_CONFIG_TYPES);
    }
  }

  public List<RequireDefinitionConfig> getRequire() {
    return require;
  }

  public void setRequire(List<RequireDefinitionConfig> require) {
    this.require = require;
  }

  public List<ObservationDefinitionConfig> getObservation() {
    return observation;
  }

  public void setObservation(List<ObservationDefinitionConfig> observation) {
    this.observation = observation;
  }

  public DataConfig getData() {
    return data;
  }

  public void setData(DataConfig data) {
    this.data = data;
  }
}
