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
package com.sematext.spm.client.aggregation;

public final class MetricAggregator {
  private MetricAggregator() {
  }

  // TODO for performance reasons we have holder only for AVG type. That means in theory somebody could have the same
  // metric defined with agentAggregationFunctions SUM, MAX and MIN and would not get any exceptions or error messages, but instead
  // some strange resulting value (after a combination of those sum, min, max functions are applied). Some day we should maybe
  // introduce some checks/warnings for that, but for now we can (again, for performance reasons) keep it simple
  public static Object aggregate(Object storedMetric, Object newMetric, String metricName,
                                 AgentAggregationFunction aggFunction) {
    if (newMetric == null) {
      return storedMetric;
    }

    Class<?> typeStored = storedMetric instanceof AvgAggregationHolder ?
        ((AvgAggregationHolder<?>) storedMetric).getType() :
        storedMetric.getClass();
    Class<?> typeNew = newMetric.getClass();

    // first check metric types
    if (!typeStored.equals(typeNew)) {
      throw new IllegalArgumentException("Can't aggregate metric " + metricName + " when multiple types are used: " +
                                             typeStored + ", " + typeNew);
    }

    if (!(newMetric instanceof Long || newMetric instanceof Double || newMetric instanceof Integer
        || newMetric instanceof Float)) {
      throw new IllegalArgumentException("Only metrics of type Long or Double can be aggregated. Metric " +
                                             metricName + " is of type " + newMetric.getClass());
    }

    if (aggFunction == AgentAggregationFunction.DISCARD) {
      return storedMetric;
    } else {
      if (storedMetric instanceof AvgAggregationHolder) {
        if (aggFunction != AgentAggregationFunction.AVG) {
          throw new IllegalArgumentException(
              "Metric " + metricName + " uses multiple agentAggregationFunctions functions: AVG, " + aggFunction);
        } else {
          ((AvgAggregationHolder) storedMetric).add((Number) newMetric);
          return storedMetric;
        }
      } else {
        if (aggFunction == AgentAggregationFunction.AVG) {
          if (storedMetric instanceof Long) {
            LongAvgAggregationHolder holder = new LongAvgAggregationHolder((Long) storedMetric);
            holder.add(newMetric instanceof Long ? (Long) newMetric : ((Number) newMetric).longValue());
            return holder;
          } else if (storedMetric instanceof Integer) {
            LongAvgAggregationHolder holder = new LongAvgAggregationHolder(((Integer) storedMetric).longValue());
            holder.add(newMetric instanceof Long ? (Long) newMetric : ((Number) newMetric).longValue());
            return holder;
          } else if (storedMetric instanceof Double) {
            DoubleAvgAggregationHolder holder = new DoubleAvgAggregationHolder((Double) storedMetric);
            holder.add(newMetric instanceof Double ? (Double) newMetric : ((Number) newMetric).doubleValue());
            return holder;
          } else if (storedMetric instanceof Float) {
            DoubleAvgAggregationHolder holder = new DoubleAvgAggregationHolder(((Float) storedMetric).doubleValue());
            holder.add(newMetric instanceof Double ? (Double) newMetric : ((Number) newMetric).doubleValue());
            return holder;
          } else {
            throw new UnsupportedOperationException(
                "Currently only Long/Integer and Double/Float metric types can be aggregated as AVG. Metric " +
                    metricName + " uses type " + storedMetric.getClass());
          }
        } else if (aggFunction == AgentAggregationFunction.MAX) {
          Object res;
          if (storedMetric instanceof Long) {
            res = ((((Long) storedMetric).longValue() > ((Long) newMetric).longValue()) ? storedMetric : newMetric);
          } else {
            res = ((((Double) storedMetric).doubleValue() > ((Double) newMetric).doubleValue()) ?
                storedMetric :
                newMetric);
          }
          return res;
        } else if (aggFunction == AgentAggregationFunction.MIN) {
          Object res;
          if (storedMetric instanceof Long) {
            res = ((((Long) storedMetric).longValue() < ((Long) newMetric).longValue()) ? storedMetric : newMetric);
          } else {
            res = ((((Double) storedMetric).doubleValue() < ((Double) newMetric).doubleValue()) ?
                storedMetric :
                newMetric);
          }
          return res;
        } else if (aggFunction == AgentAggregationFunction.SUM) {
          Object res;
          if (storedMetric instanceof Long) {
            res = getLongSum((Long) storedMetric, (Long) newMetric);
          } else {
            res = getDoubleSum((Double) storedMetric, (Double) newMetric);
          }
          return res;
        } else {
          throw new IllegalArgumentException(
              "Can't do the aggregation for metric " + metricName + " using function : " + aggFunction);
        }
      }
    }
  }

  static Double getDoubleSum(Double storedMetric, Double newMetric) {
    if (storedMetric.doubleValue() == 0) {
      return newMetric;
    } else if (newMetric.doubleValue() == 0) {
      return storedMetric;
    } else {
      return storedMetric + newMetric;
    }
  }

  static Long getLongSum(Long storedMetric, Long newMetric) {
    if (storedMetric.longValue() == 0) {
      return newMetric;
    } else if (newMetric.longValue() == 0) {
      return storedMetric;
    } else {
      return storedMetric.longValue() + newMetric.longValue();
    }
  }
}

