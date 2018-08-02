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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public enum Serializer implements StatValuesSerializer<String> {
  INFLUX("i", ",", "") {
    @Override public String serializeMetainfo(String appToken, MetricMetainfo metricConfig) {
      StringBuilder builder = new StringBuilder(200);
      builder.append(metricConfig.getNamespace()).append(",").append("token=").append(appToken);
      appendMetaInfo(builder, "type", convertType(metricConfig.getType()));
      String numericType = getNumericType(metricConfig.getType());
      if (numericType != null) {
        appendMetaInfo(builder, "numericType", numericType);
      }
      appendMetaInfo(builder, "unit", metricConfig.getUnit());
      appendMetaInfo(builder, "label", metricConfig.getLabel());
      appendMetaInfo(builder, "description", metricConfig.getDescription());
      if (!isNullOrEmpty(metricConfig.getName())) {
        builder.append(" ").append(formatKeysAndTagValues(metricConfig.getName()));
      }
      return builder.toString();
    }

    private void appendMetaInfo(StringBuilder builder, String key, String value) {
      if (!isNullOrEmpty(value)) {
        builder.append(",").append(key).append("=");
        builder.append(formatKeysAndTagValues(value));
      }
    }

    private String convertType(String type) {
      String convertedType = type;
      if ("long_gauge".equals(type) || "double_gauge".equals(type)) {
        convertedType = "gauge";
      } else if ("long_counter".equals(type) || "double_counter".equals(type)) {
        convertedType = "counter";
      }

      return convertedType;
    }

    private String getNumericType(String type) {
      String numericType = null;
      if ("gauge".equals(type) ||
          "long_gauge".equals(type) ||
          "counter".equals(type)) {
        numericType = "long";
      } else if ("double_gauge".equals(type) ||
          "double_counter".equals(type)) {
        numericType = "double";
      }
      return numericType;
    }

    private boolean isNullOrEmpty(String value) {
      return value == null || value.trim().isEmpty();
    }

    @Override
    public boolean isNull(Object value) {
      if (value == null) {
        return true;
      } else if (value instanceof Number) {
        String valueStr = String.valueOf(value);
        return "null".equalsIgnoreCase(valueStr) || valueStr.contains("Infinity") || valueStr.equals("NaN");
      } else {
        return false;
      }
    }

    @Override
    protected void serialize(StringBuilder sb, String metricNamespace, String appToken, Map<String, Object> metrics,
                             Map<String, String> tags, long timestamp) {
      if (metrics == null || metrics.isEmpty()) {
        return;
      }

      sb.append(metricNamespace).append(",").append("token=").append(appToken);

      if (tags != null) {
        // TODO - As per Influx protocol, for performance reasons tags should be sorted
        // https://docs.influxdata.com/influxdb/v1.4/write_protocols/line_protocol_tutorial/
        for (String tag : tags.keySet()) {
          String val = tags.get(tag);
          if (!isNull(val)) {
            append(sb, formatKeysAndTagValues(tag), formatKeysAndTagValues(val), false);
          }
        }
      }

      sb.append(" ");

      if (metrics != null) {
        boolean isFirst = true;
        for (String metric : metrics.keySet()) {
          Object val = metrics.get(metric);
          if (!isNull(val)) {
            append(sb, formatKeysAndTagValues(metric), formatMetricValue(val), isFirst);
            isFirst = false;
          }
        }
      }

      if (timestamp != -1) {
        // influx protocol requires timestamp in ns
        sb.append(" ").append(timestamp).append("000000");
      }
    }

    @Override
    protected void append(StringBuilder sb, String name, Object value, boolean isFirstElem) {
      if (!isFirstElem) {
        sb.append(getDelimiter());
      }
      sb.append(serialize(name, value));
    }

    @Override
    protected Object serialize(String name, Object val) {
      return val == null ? "" : name + "=" + val;
    }

    @Override
    protected void serialize(StringBuilder stringBuilder, List<Object> statValues) {
      throw new UnsupportedOperationException(
          "Not supported for this type of data " + stringBuilder + ", " + statValues);
    }

    private String formatKeysAndTagValues(String value) {
      String esc = INFLUX_ALREADY_ESCAPED.get(value);
      if (esc != null) {
        return esc;
      }

      String tmp = value;
      for (String ch : INFLUX_CHARS_TO_ESCAPE) {
        tmp = tmp.replaceAll(ch, "\\\\" + ch);
      }
      INFLUX_ALREADY_ESCAPED.put(value, tmp);
      return tmp;
    }

    private String formatMetricValue(Object value) {
      if (value instanceof Integer || value instanceof Long || value instanceof BigInteger) {
        return value.toString() + "i";
      } else if (value instanceof String) {
        // TODO - Can be improved by adding a cache of already escaped values to avoid repeated concatenations and replaceAll
        return "\"" + ((String) value).replaceAll("\"", "\\\"") + "\"";
      } else if (value instanceof Boolean) {
        return String.valueOf(value);
      } else if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) {
        return String.valueOf(value);
      } else if (value instanceof SerializableMetricValue) {
        return ((SerializableMetricValue) value).serializeToInflux();
      } else {
        throw new UnsupportedOperationException(
            "Unsupported type " + value.getClass().getName() + " - can't serialize it!");
      }
    }

    @Override
    protected boolean isShouldGeneratePrefix() {
      return false;
    }
  },
  TAB("t", "\t", "!") {
    @Override public String serializeMetainfo(String appToken, MetricMetainfo metricConfig) {
      throw new UnsupportedOperationException("Not supported for this type of data");
    }

    @Override
    public boolean isNull(Object value) {
      return value == null || value.toString().equals(getNullValue());
    }

    @Override
    protected void serialize(StringBuilder sb, List<Object> statValues) {
      for (Object value : statValues) {
        append(sb, null, value, true);
      }
    }

    @Override
    protected void append(StringBuilder sb, String name, Object value, boolean isFirstElem) {
      sb.append(serialize(value)).append(getDelimiter());
    }

    @Override
    protected Object serialize(String name, Object val) {
      throw new UnsupportedOperationException("Not supported for this type of data");
    }

    @Override
    protected void serialize(StringBuilder stringBuilder, String metricNamespace, String appToken,
                             Map<String, Object> metrics,
                             Map<String, String> tags, long timestamp) {
      throw new UnsupportedOperationException("Not supported for this type of data");
    }

    @Override
    protected boolean isShouldGeneratePrefix() {
      return true;
    }
  },
  COLLECTD("c", ",", "!null!") {
    @Override public String serializeMetainfo(String appToken, MetricMetainfo metricConfig) {
      throw new UnsupportedOperationException("Not supported for this type of data");
    }

    @Override
    public boolean isNull(Object value) {
      return value == null || value.toString().equals(getNullValue());
    }

    @Override
    protected void serialize(StringBuilder sb, List<Object> statValues) {
      Iterator<Object> it = statValues.iterator();

      if (!envelop(it, sb)) {
        return;
      }

      boolean isFirst = true;
      while (it.hasNext()) {
        Object value = it.next();
        append(sb, null, value, isFirst);
        isFirst = false;
      }
    }

    // we add standard header to the collectd string
    private boolean envelop(Iterator<Object> it, StringBuilder sb) {
      if (!it.hasNext()) {
        return false;
      }
      TAB.append(sb, null, it.next(), true);

      if (!it.hasNext()) {
        return false;
      }
      TAB.append(sb, null, it.next(), false);
      return true;
    }

    @Override
    protected void append(StringBuilder sb, String name, Object value, boolean isFirstElem) {
      if (!isFirstElem) {
        sb.append(getDelimiter());
      }
      sb.append(serialize(value));
    }

    @Override
    protected Object serialize(String name, Object val) {
      throw new UnsupportedOperationException("Not supported for this type of data");
    }

    @Override
    protected void serialize(StringBuilder stringBuilder, String metricNamespace, String appToken,
                             Map<String, Object> metrics,
                             Map<String, String> tags, long timestamp) {
      throw new UnsupportedOperationException("Not supported for this type of data");
    }

    @Override
    protected boolean isShouldGeneratePrefix() {
      return false;
    }
  };

  public abstract boolean isNull(Object value);

  private static final Map<String, Serializer> KEY_SERIALIZER = new HashMap<String, Serializer>();

  private static final List<String> INFLUX_CHARS_TO_ESCAPE = Arrays.asList(",", "=", " ");
  private static final Map<String, String> INFLUX_ALREADY_ESCAPED = new HashMap<String, String>();

  public static Serializer get(String key) {
    return KEY_SERIALIZER.get(key);
  }

  static {
    for (Serializer serializer : Serializer.values()) {
      KEY_SERIALIZER.put(serializer.key, serializer);
    }
  }

  private final String key;
  private final String delimiter;
  private final String nullValue;

  private Serializer(String key, String delimeter, String nullValue) {
    this.key = key;
    this.delimiter = delimeter;
    this.nullValue = nullValue;
  }

  protected String getNullValue() {
    return nullValue;
  }

  public final String getDelimiter() {
    return delimiter;
  }

  protected abstract Object serialize(String name, Object val);

  protected Object serialize(Object val) {
    return val == null ? nullValue : val;
  }

  protected abstract void append(StringBuilder sb, String name, Object value, boolean isFirstElem);

  protected abstract void serialize(StringBuilder stringBuilder, String metricNamespace, String appToken,
                                    Map<String, Object> metrics, Map<String, String> tags, long timestamp);

  protected abstract void serialize(StringBuilder stringBuilder, List<Object> statValues);

  protected abstract boolean isShouldGeneratePrefix();

  @Override
  public final String serialize(List<Object> statValues) {
    StringBuilder sb = new StringBuilder(50);
    serialize(sb, statValues);
    return sb.toString();
  }

  @Override
  public final String serialize(String metricNamespace, String appToken, Map<String, Object> metrics,
                                Map<String, String> tags, long timestamp) {
    // starting with a bit bigger initial size, our grouped metric lines can easily be few kB big
    StringBuilder sb = new StringBuilder(1000);
    serialize(sb, metricNamespace, appToken, metrics, tags, timestamp);
    return sb.toString();
  }

  @Override
  public boolean shouldGeneratePrefix() {
    return isShouldGeneratePrefix();
  }
}
