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
package com.sematext.spm.client.util;

import java.util.Iterator;
import java.util.Map;

public final class StringUtils {

  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private StringUtils() {
    // It's utility class
  }

  public static final class Chain {

    private final StringOuterpolator outerpolator;
    private final String leftDelim;
    private final String rightDelim;
    private final String outTemplate;

    private Chain(StringOuterpolator outerpolator, String outTemplate, String leftDelim, String rightDelim) {
      this.outerpolator = outerpolator;
      this.leftDelim = leftDelim;
      this.rightDelim = rightDelim;
      this.outTemplate = outTemplate;
    }

    public static Chain make(String inTemplate, String outTemplate, String leftDelim, String rightDelim) {
      return new Chain(StringOuterpolator.outerpolator(inTemplate, leftDelim, rightDelim), outTemplate, leftDelim,
                       rightDelim);
    }

    public String process(String string) {
      return StringInterpolator.interpolator(leftDelim, rightDelim).addParams(extractParams(string))
          .interpolate(outTemplate);
    }

    public Map<String, String> extractParams(String string) {
      return outerpolator.outerpolate(string);
    }
  }

  public static String trim(String from) {
    return from == null ? "" : from.trim();
  }

  public static boolean isEmpty(String from) {
    return "".equals(trim(from));
  }

  public static String prefixed(String from, String prefix) {
    return !trim(from).startsWith(prefix) ? null : trim(from).substring(prefix.length());
  }

  public static String join(Iterable<?> iterable, String delim) {
    final Iterator<?> iter = iterable.iterator();
    final StringBuilder joined = new StringBuilder();
    while (iter.hasNext()) {
      joined.append(iter.next());
      if (iter.hasNext()) {
        joined.append(delim);
      }
    }
    return joined.toString();
  }

  public static String join(Object[] objects, String delim) {
    final StringBuilder joined = new StringBuilder();
    for (int i = 0; i < objects.length; i++) {
      joined.append(objects[i]);
      if (i < objects.length - 1) {
        joined.append(",");
      }
    }
    return joined.toString();
  }

  public static String removeQuotes(String property) {
    property = property.trim();
    if (property.startsWith("\"") && property.endsWith("\"")) {
      return property.substring(1, property.length() - 1);
    } else {
      return property;
    }
  }
}
