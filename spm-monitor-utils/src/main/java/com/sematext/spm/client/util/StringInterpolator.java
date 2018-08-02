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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringInterpolator {

  private final Pattern pattern;
  private final Map<String, String> params = new HashMap<String, String>();

  private StringInterpolator(String leftDelim, String rightDelim) {
    pattern = Pattern.compile(Pattern.quote(leftDelim) + "(\\w+?)" + Pattern.quote(rightDelim));
  }

  public static StringInterpolator interpolator(String delim) {
    return interpolator(delim, delim);
  }

  public static StringInterpolator interpolator(String leftDelim, String rightDelim) {
    return new StringInterpolator(leftDelim, rightDelim);
  }

  public StringInterpolator addParam(String paramName, Object paramValue) {
    params.put(paramName, paramValue.toString());
    return this;
  }

  public StringInterpolator addParams(Map<String, ?> params) {
    for (Map.Entry<String, ?> entry : params.entrySet()) {
      addParam(entry.getKey(), entry.getValue());
    }
    return this;
  }

  public String interpolate(String interpolateString) {
    StringBuffer res = new StringBuffer();
    Matcher matcher = pattern.matcher(interpolateString);
    while (matcher.find()) {
      String paramName = matcher.group(1);
      String paramValue = params.containsKey(paramName) ? params.get(paramName) : "";
      matcher.appendReplacement(res, Matcher.quoteReplacement(paramValue));
    }

    matcher.appendTail(res);

    return res.toString();
  }
}
