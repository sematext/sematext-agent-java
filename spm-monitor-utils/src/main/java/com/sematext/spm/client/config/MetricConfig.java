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

import java.util.HashMap;
import java.util.Map;

import com.sematext.spm.client.MetricMetainfo;
import com.sematext.spm.client.aggregation.AgentAggregationFunction;

public class MetricConfig implements MetricMetainfo {
  public enum ConfigMetricType {
    LONG_GAUGE,
    GAUGE,
    DOUBLE_GAUGE,
    COUNTER,
    DOUBLE_COUNTER,
    TEXT
  }

  private static final Map<String, ConfigMetricType> METRIC_TYPES = new HashMap<String, MetricConfig.ConfigMetricType>(
      ConfigMetricType.values().length);
  private static final Map<String, AgentAggregationFunction> AGENT_AGG_FUNCTIONS = new HashMap<String, AgentAggregationFunction>(
      AgentAggregationFunction.values().length);

  private static final String ALLOWED_TYPES;
  private static final String ALLOWED_AGENT_AGG_FUNCTIONS;

  static {
    for (ConfigMetricType mt : ConfigMetricType.values()) {
      METRIC_TYPES.put(mt.name().toLowerCase(), mt);
    }
    for (AgentAggregationFunction aaf : AgentAggregationFunction.values()) {
      AGENT_AGG_FUNCTIONS.put(aaf.name().toLowerCase(), aaf);
    }

    String allowedMetricTypes = "(";
    int counter = 0;
    for (String allowed : METRIC_TYPES.keySet()) {
      allowedMetricTypes += ((counter > 0) ? ", " : "") + allowed;
      counter++;
    }
    allowedMetricTypes += ")";
    ALLOWED_TYPES = allowedMetricTypes;

    String allowedFunctions = "(";
    counter = 0;
    for (String allowed : AGENT_AGG_FUNCTIONS.keySet()) {
      allowedFunctions += ((counter > 0) ? ", " : "") + allowed;
      counter++;
    }
    allowedFunctions += ")";
    ALLOWED_AGENT_AGG_FUNCTIONS = allowedFunctions;
  }

  private String namespace;
  private String name;
  private String source;
  private boolean send = true;
  private String pctls;
  protected String type;
  private boolean stateful = false;
  private AgentAggregationFunction agentAggregation;
  private String label;
  private String description;
  private String unit;

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name.trim();
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public boolean isSend() {
    return send;
  }

  public void setSend(String send) {
    if ("true".equalsIgnoreCase(send)) {
      this.send = true;
    } else if ("false".equalsIgnoreCase(send)) {
      this.send = false;
    } else {
      throw new IllegalArgumentException("Unrecognized boolean value: " + send + " for field 'send'");
    }
  }

  public String getPctls() {
    return pctls;
  }

  public void setPctls(String pctls) {
    this.pctls = pctls;
  }

  @Override
  public String getType() {
    return type;
  }

  public void setType(String type) {
    String tmp = type.trim().toLowerCase();
    ConfigMetricType tmpType = METRIC_TYPES.get(tmp);
    if (tmpType == null) {
      throw new IllegalArgumentException("Unrecognized metric type: " + type + ", allowed types are: " + ALLOWED_TYPES);
    } else {
      this.type = tmpType.name().toLowerCase();
    }
  }

  public boolean isStateful() {
    return stateful;
  }

  public void setStateful(String stateful) {
    if ("true".equalsIgnoreCase(stateful)) {
      this.stateful = true;
    } else if ("false".equalsIgnoreCase(stateful)) {
      this.stateful = false;
    } else {
      throw new IllegalArgumentException("Unrecognized boolean value: " + stateful + " for field 'stateful'");
    }
  }

  public AgentAggregationFunction getAgentAggregation() {
    return agentAggregation;
  }

  public void setAgentAggregation(String agentAggregation) {
    String tmp = agentAggregation.trim().toLowerCase();
    this.agentAggregation = AGENT_AGG_FUNCTIONS.get(tmp);
    if (this.agentAggregation == null) {
      throw new IllegalArgumentException("Unrecognized metric agentAggregationFunction: " + agentAggregation +
                                             ", allowed functions are: " + ALLOWED_AGENT_AGG_FUNCTIONS);
    }
  }

  @Override
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  @Override
  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  private boolean compare(String thisStr, String that) {
    return (thisStr == null && that == null) ||
        (thisStr != null && thisStr.equals(that));
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    MetricConfig that = (MetricConfig) o;

    boolean equals = compare(namespace, that.namespace) &&
        compare(unit, that.unit) &&
        compare(label, that.label) &&
        compare(name, that.name) &&
        compare(description, that.description) &&
        compare(type, that.type);
    return equals;
  }
}
