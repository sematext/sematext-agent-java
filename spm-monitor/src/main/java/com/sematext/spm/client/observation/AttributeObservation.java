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
package com.sematext.spm.client.observation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.StatsCollectionFailedException;
import com.sematext.spm.client.aggregation.AgentAggregationFunction;
import com.sematext.spm.client.attributes.MetricType;
import com.sematext.spm.client.attributes.MetricValueHolder;
import com.sematext.spm.client.config.FunctionInvokerConfig;
import com.sematext.spm.client.config.MetricConfig;

public abstract class AttributeObservation<T> {
  private String attributeName;
  private String metricName;

  /**
   * name that should be used in output (based on values of "name" and "source" attributes)
   */
  private String finalName;

  // when for some monitored attribute there are multiple values for same datapoint, specified aggregation function will
  // be used by the agent to produce a single value for output
  private AgentAggregationFunction agentAggregationFunction;

  // describes metric's nature and how it should be treated internally (aggregation function doesn't have to be linked to this type)
  private MetricType metricType;

  // TODO: there is a potential problem with this map - it will grow indefinitely, even when some observation bean
  // is gone - we should ensure that destroying an observation bean also clears this map
  private Map<ObservationBean<?, ?>, MetricValueHolder<?>> valueHolders;

  public AttributeObservation() {
  }

  public AttributeObservation(AttributeObservation<T> original, String newAttributeName) {
    this.attributeName = newAttributeName;
    this.metricName = original.metricName;
    this.finalName = original.finalName;
    this.agentAggregationFunction = original.agentAggregationFunction;
    this.metricType = original.metricType;
  }

  /**
   * Performs initialization based on YAML configuration
   *
   * @throws ConfigurationFailedException when configured improperly
   */
  public void initFromConfig(MetricConfig metric) throws ConfigurationFailedException {
    if (metric instanceof FunctionInvokerConfig) {
      attributeName = metric.getName();
    } else {
      String source = metric.getSource().trim();
      if (source == null || "".equals(source.toString().trim())) {
        throw new ConfigurationFailedException("Missing required 'source' attribute for 'metric' definition");
      }
      attributeName = source;
    }

    metricName = metric.getName();
    if (metricName == null || "".equals(metricName.trim())) {
      finalName = attributeName;
    } else {
      finalName = metricName;
    }

    metricType = getMetricType();

    agentAggregationFunction = getAgentAggregationFunction(metricType, metric.getAgentAggregation());
  }

  public static AgentAggregationFunction getAgentAggregationFunction(MetricType metricType,
                                                                     AgentAggregationFunction agentAggregationFunction) {
    if (agentAggregationFunction == null) {
      if (metricType == MetricType.COUNTER) {
        return AgentAggregationFunction.SUM;
      } else if (metricType == MetricType.GAUGE) {
        return AgentAggregationFunction.AVG;
      } else if (metricType == MetricType.PERCENTILE) {
        throw new UnsupportedOperationException("Aggregation of pctl value not supported!");
      } else {
        return AgentAggregationFunction.DISCARD;
      }
    } else {
      return agentAggregationFunction;
    }
  }

  public boolean isOptional() {
    return false;
  }

  public String getAttributeName() {
    return attributeName;
  }

  protected String getDescriptionAttributeId() {
    return "description";
  }

  protected String getOptionalAttributeId() {
    return "optional";
  }

  // TODO this needs more refactoring, variable argument list is used for now to quickly unify json and jmx observation attributes

  /**
   * Collects value of attribute
   *
   * @param data
   * @param context          of params
   * @param additionalParams
   * @return
   * @throws StatsCollectionFailedException
   */
  public abstract Object getValue(ObservationBean<?, ?> parentObservation, T data, Map<String, ?> context,
                                  Object... additionalParams) throws StatsCollectionFailedException;

  protected abstract MetricValueHolder<?> createHolder();

  public abstract MetricType getMetricType();

  protected final MetricValueHolder<?> getValueHolder(ObservationBean<?, ?> parentObservation) {
    if (valueHolders == null) {
      valueHolders = new ConcurrentHashMap<ObservationBean<?, ?>, MetricValueHolder<?>>();
    }

    MetricValueHolder<?> holder = valueHolders.get(parentObservation);
    if (holder == null) {
      holder = createHolder();
      valueHolders.put(parentObservation, holder);
    }
    return holder;
  }

  // attributeObservation measures current value ('measurement'), but real metric value depends on previous measurement
  // of the same attribute, so this method can be used to "calculate" that real value
  protected final Object getMetricValue(ObservationBean<?, ?> parentObservation, Object measurement) {
    if (measurement == null) {
      return null;
    }
    if (measurement instanceof Number) {
      // in case of numeric metrics, filter-out anything that is not really a number
      String measurementStr = String.valueOf(measurement);
      if ("null".equalsIgnoreCase(measurementStr) || measurementStr.contains("Infinity") || measurementStr
          .equals("NaN")) {
        return null;
      }
    }

    return getValueHolder(parentObservation).getValue(measurement);
  }

  public String getFinalName() {
    return finalName;
  }

  public AgentAggregationFunction getAgentAggregationFunction() {
    return agentAggregationFunction;
  }
}
