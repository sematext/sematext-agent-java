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

import com.fasterxml.jackson.core.JsonFactory;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.http.ServerInfo;

/**
 * Parses the Prometheus text exposition format into the tree that json collectors walk. Every sample becomes a map
 * of its labels plus a "value" entry, listed under the sample name:
 * <pre>
 *   m_total{core="c1",result="hit"} 2.0   ->   {"m_total": [{"core": "c1", "result": "hit", "value": 2}]}
 * </pre>
 * Integral values are Longs so long_gauge and counter metrics can read them.
 */
public class PrometheusTextJsonHandler implements CustomJsonHandler<Map<String, Object>> {
  private static final Log LOG = LogFactory.getLog(PrometheusTextJsonHandler.class);

  public static final String VALUE_FIELD = "value";

  private static final double MAX_EXACT_LONG = 9007199254740992d;

  private ServerInfo jsonServerInfo;

  @Override
  public Map<String, Object> parse(InputStream inputStream, JsonFactory jsonFactory) throws IOException {
    Map<String, Object> samples = new UnifiedMap<String, Object>();
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
    String line;
    while ((line = reader.readLine()) != null) {
      line = line.trim();
      if (line.length() == 0 || line.charAt(0) == '#') {
        continue;
      }
      if (!addSample(samples, line)) {
        LOG.debug("Skipping unparsable Prometheus line: " + line);
      }
    }
    return samples;
  }

  private static boolean addSample(Map<String, Object> samples, String line) {
    Map<String, Object> sample = new UnifiedMap<String, Object>();
    int pos = 0;
    while (pos < line.length() && line.charAt(pos) != '{' && !Character.isWhitespace(line.charAt(pos))) {
      pos++;
    }
    String name = line.substring(0, pos);
    if (pos < line.length() && line.charAt(pos) == '{') {
      pos = parseLabels(line, pos + 1, sample);
      if (pos < 0) {
        return false;
      }
    }

    Object value = parseValue(line.substring(pos).trim());
    if (name.length() == 0 || value == null) {
      return false;
    }
    sample.put(VALUE_FIELD, value);

    List<Map<String, Object>> sameName = (List<Map<String, Object>>) samples.get(name);
    if (sameName == null) {
      sameName = new FastList<Map<String, Object>>();
      samples.put(name, sameName);
    }
    sameName.add(sample);
    return true;
  }

  /**
   * Reads label pairs starting right after '{'. Returns the position after the closing '}', or -1 if malformed.
   */
  private static int parseLabels(String line, int pos, Map<String, Object> sample) {
    while (pos < line.length()) {
      char c = line.charAt(pos);
      if (c == '}') {
        return pos + 1;
      }
      if (c == ',' || Character.isWhitespace(c)) {
        pos++;
        continue;
      }

      int eq = line.indexOf('=', pos);
      if (eq < 0 || eq + 1 >= line.length() || line.charAt(eq + 1) != '"') {
        return -1;
      }
      String labelName = line.substring(pos, eq).trim();

      StringBuilder labelValue = new StringBuilder();
      pos = eq + 2;
      boolean closed = false;
      while (pos < line.length()) {
        c = line.charAt(pos++);
        if (c == '"') {
          closed = true;
          break;
        }
        if (c == '\\' && pos < line.length()) {
          char escaped = line.charAt(pos++);
          labelValue.append(escaped == 'n' ? '\n' : escaped);
        } else {
          labelValue.append(c);
        }
      }
      if (!closed) {
        return -1;
      }
      sample.put(labelName, labelValue.toString());
    }
    return -1;
  }

  private static Object parseValue(String valueAndTimestamp) {
    if (valueAndTimestamp.length() == 0) {
      return null;
    }
    String value = valueAndTimestamp.split("\\s+")[0];
    double d;
    if ("NaN".equals(value)) {
      return Double.NaN;
    } else if ("+Inf".equals(value) || "Inf".equals(value)) {
      return Double.POSITIVE_INFINITY;
    } else if ("-Inf".equals(value)) {
      return Double.NEGATIVE_INFINITY;
    }
    try {
      d = Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return null;
    }
    if (d == Math.rint(d) && Math.abs(d) < MAX_EXACT_LONG) {
      return (long) d;
    }
    return d;
  }

  @Override
  public void setJsonServerInfo(ServerInfo jsonServerInfo) {
    this.jsonServerInfo = jsonServerInfo;
  }
}
