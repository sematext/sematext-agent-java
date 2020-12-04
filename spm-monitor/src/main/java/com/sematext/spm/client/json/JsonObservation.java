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
package com.sematext.spm.client.json;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.StatsCollectionFailedException;
import com.sematext.spm.client.config.ObservationDefinitionConfig;
import com.sematext.spm.client.http.ServerInfo;
import com.sematext.spm.client.observation.ObservationBean;
import com.sematext.spm.client.observation.ObservationBeanDump;

public class JsonObservation extends ObservationBean<JsonAttributeObservation, Object> {
  private static final Log LOG = LogFactory.getLog(JsonAttributeObservation.class);

  private String beanName;
  private String jsonDataNodePath;
  private ServerInfo jsonServerInfo;

  // used when instantiating a "real" JsonObservation object for some particular json bean (resulting object is not just a 
  // config holder anymore)
  public JsonObservation(JsonObservation orig, String beanName, String realBeanPath, Map<String, String> beanPathTags) {
    super(orig, beanPathTags);
    this.beanName = beanName;
    this.jsonDataNodePath = realBeanPath;
    this.jsonServerInfo = orig.getJsonServerInfo();
  }

  public JsonObservation(ObservationDefinitionConfig observationDefinition, ServerInfo jsonServerInfo)
      throws ConfigurationFailedException {
    super(observationDefinition);
    this.jsonServerInfo = jsonServerInfo;
  }

  public ObservationBean getCopy() throws ConfigurationFailedException {
    return new JsonObservation(this.observationDefinition, this.jsonServerInfo);
  }

  @Override
  public Set<ObservationBeanDump> collectStats(Object data) {
    ObservationBeanDump stats = new ObservationBeanDump(beanName);
    for (JsonAttributeObservation attributeObservation : getAttributeObservations()) {
      Object value = null;
      try {
        // Json not need the context.
        Map<String, ?> context = Collections.emptyMap();
        value = attributeObservation.getValue(this, data, context, jsonDataNodePath);
      } catch (StatsCollectionFailedException e) {
        LOG.debug("Failed to extract stats value, attribute name: " + attributeObservation.getAttributeName() +
                      ", message: " + e.getMessage());
      } catch (RuntimeException re) {
        LOG.debug("Failed to extract stats value, attribute name: " + attributeObservation.getAttributeName(), re);
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
    jsonDataNodePath = observationDefinition.getPath();
    if (jsonDataNodePath == null || "".equals(jsonDataNodePath.toString().trim())) {
      throw new ConfigurationFailedException("JSON observation missing required attribute 'path'");
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
  protected JsonAttributeObservation createAttributeObservation(String type) {
    if ("counter".equals(type)) {
      return new JsonRealCounterAttribute();
    } else if ("text".equals(type)) {
      return new JsonTextAttribute();
    } else if ("gauge".equals(type)) {
      return new JsonGaugeAttribute();
    } else if ("long_gauge".equals(type)) {
      return new JsonLongGaugeAttribute();
    } else if ("double_gauge".equals(type)) {
      return new JsonDoubleGaugeAttribute();
    } else if ("change".equals(type)) {
      return new JsonValueChangeAttribute();
    } else {
      LOG.error("Unknown attribute observation type: " + type);
      return null;
    }
  }

  public String getJsonDataNodePath() {
    return jsonDataNodePath;
  }

  public ServerInfo getJsonServerInfo() {
    return jsonServerInfo;
  }
}
