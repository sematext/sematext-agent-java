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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.aggregation.AgentAggregationFunction;
import com.sematext.spm.client.attributes.MetricType;

/**
 * Holds stats values
 */
public class StatValues {
  private static final Log LOG = LogFactory.getLog(StatValues.class);
  // support for both the new logic (with metrics and tags) and the old logic with just stats values
  private Map<String, Object> metrics;
  private Map<String, AgentAggregationFunction> agentAggregationFunctions;
  private Map<String, MetricType> metricTypes;
  private Map<String, String> tags;
  private long timestamp;
  private String appToken;
  private String metricNamespace;

  private List<Object> vals;

  public StatValues add(Object value) {
    if (vals == null) {
      vals = new ArrayList<Object>();
    }
    vals.add(value);
    return this;
  }

  public StatValues addNulls(int count) {
    for (int i = 0; i < count; i++) {
      add(null);
    }
    return this;
  }

  public int getSize() {
    if (metrics != null) {
      return metrics.size();
    } else {
      return vals.size();
    }
  }

  public void setMetrics(Map<String, Object> metrics) {
    this.metrics = metrics;
  }

  public void setTags(Map<String, String> tags) {
    this.tags = tags;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public void setAppToken(String appToken) {
    this.appToken = appToken;
  }

  /**
   * A StatValues row is considered zero row when all its Number metrics have value 0. The logic is a bit dirty, consider
   * refactoring (for instance, we assume the first Number will always be a timestamp).
   * <p>
   * NOTE:
   * The difference between null-row and zero-row: zero-rows are regular rows where all metrics are recorded as
   * 0 (for instance there was 0 requests, with 0 latency...). Null-rows are rows where our internal optimization
   * converted numeric values (under specific conditions) into null values. Example of such optimization is
   * optimization for counter values which are now recorded once every 60 sec (remaining measurements are
   * recorded as null values).
   *
   * @return
   */
  public boolean isZeroRow() {
    boolean foundNumber = false;
    boolean skippedFirstNumber = false;

    if (metrics != null) {
      for (String k : metrics.keySet()) {
        Object v = metrics.get(k);
        if (v != null) {
          if (v instanceof Number) {
            // first Number should be ignored since it is a timestamp
            if (!skippedFirstNumber) {
              skippedFirstNumber = true;
              continue;
            }

            foundNumber = true;

            // only Integer and Long supported for now; the rest are treated as non-0
            if (v instanceof Integer) {
              if (((Integer) v).intValue() != 0) {
                return false;
              }
            } else if (v instanceof Long) {
              if (((Long) v).longValue() != 0) {
                return false;
              }
            } else {
              return false;
            }
          }
        }
      }
    } else {
      for (Object v : vals) {
        if (v != null) {
          if (v instanceof Number) {
            // first Number should be ignored since it is a timestamp
            if (!skippedFirstNumber) {
              skippedFirstNumber = true;
              continue;
            }

            foundNumber = true;

            // only Integer and Long supported for now; the rest are treated as non-0
            if (v instanceof Integer) {
              if (((Integer) v).intValue() != 0) {
                return false;
              }
            } else if (v instanceof Long) {
              if (((Long) v).longValue() != 0) {
                return false;
              }
            } else {
              return false;
            }
          }
        }
      }
    }

    // if we found some numbers but didn't return (false) till now, meaning all found numbers were 0, return true; otherwise return false
    return foundNumber;
  }

  /**
   * A StatValues row is considered null row when all its Number metrics have value null. The logic is a bit dirty, consider
   * refactoring (for instance, we assume the first Number will always be a timestamp).
   * <p>
   * NOTE:
   * The difference between null-row and zero-row: zero-rows are regular rows where all metrics are recorded as
   * 0 (for instance there was 0 requests, with 0 latency...). Null-rows are rows where our internal optimization
   * converted numeric values (under specific conditions) into null values. Example of such optimization is
   * optimization for counter values which are now recorded once every 60 sec (remaining measurements are
   * recorded as null values).
   *
   * @return
   */
  public boolean isNullRow() {
    if (metrics != null) {
      for (String k : metrics.keySet()) {
        Object v = metrics.get(k);
        if (v != null) {
          return false;
        }
      }
      return true;
    } else if (vals != null) {
      throw new IllegalStateException("Unsupported situation encountered - old metrics format processed by new agent");
      
      /*
      for (Object v : vals) {
        if (v != null) {
          if (v instanceof Number) {
            // first Number should be ignored since it is a timestamp
            if (!skippedFirstNumber) {
              skippedFirstNumber = true;
              continue;
            }
            
            return false;
          }
        } else {
          foundNull = true;
        }
      }
      */
    }

    return true;
  }

  public <T> T getAsSerialized(StatValuesSerializer<T> serializer) {
    if (serializer == Serializer.INFLUX) {
      // if (metrics != null) {
      return serializer.serialize(metricNamespace, appToken, metrics, tags, timestamp);
    } else {
      return serializer.serialize(vals);
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("StatValues");
    sb.append("{").append(metrics != null ?
                              Serializer.INFLUX.serialize(metricNamespace, appToken, metrics, tags, timestamp) :
                              Serializer.TAB.serialize(vals));
    sb.append('}');
    return sb.toString();
  }

  public Map<String, Object> getMetrics() {
    return metrics;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  public String getAppToken() {
    return appToken;
  }

  public Map<String, AgentAggregationFunction> getAgentAggregationFunctions() {
    return agentAggregationFunctions;
  }

  public void setAgentAggregationFunctions(Map<String, AgentAggregationFunction> agentAggregationFunctions) {
    this.agentAggregationFunctions = agentAggregationFunctions;
  }

  public String getMetricNamespace() {
    return metricNamespace;
  }

  public void setMetricNamespace(String metricNamespace) {
    this.metricNamespace = metricNamespace;
  }

  public Map<String, MetricType> getMetricTypes() {
    return metricTypes;
  }

  public void setMetricTypes(Map<String, MetricType> metricTypes) {
    this.metricTypes = metricTypes;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
