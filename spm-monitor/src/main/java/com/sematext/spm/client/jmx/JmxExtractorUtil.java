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
package com.sematext.spm.client.jmx;

import java.util.StringTokenizer;

public final class JmxExtractorUtil {
  private JmxExtractorUtil() {
  }

  /**
   * Extracts from a textual JMX field a long value defined with parameter fieldName.
   *
   * @param text
   * @param value
   * @param defaultValue
   * @return
   */
  public static Long extractFromField(String text, String fieldName, Long defaultValue) {
    if (text.indexOf(fieldName) == -1) {
      return defaultValue;
    }

    StringTokenizer str = new StringTokenizer(text, " ,;(){}");

    while (str.hasMoreTokens()) {
      String tmp = str.nextToken();
      if (tmp.indexOf(fieldName) != -1) {
        tmp = tmp.substring(tmp.indexOf(fieldName) + fieldName.length() + 1);
        return Long.parseLong(tmp.trim());
      }
    }

    return -1L;
  }

}
