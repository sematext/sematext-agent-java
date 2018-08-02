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

import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringOuterpolator {

  private final Pattern pattern;
  private final List<String> params;

  private StringOuterpolator(Pattern pattern, List<String> params) {
    this.pattern = pattern;
    this.params = params;
  }

  public static StringOuterpolator outerpolator(String pattern, String delim) {
    return outerpolator(pattern, delim, delim);
  }

  public static StringOuterpolator outerpolator(String pattern, String leftDelim, String rightDelim) {
    Matcher matcher = compile(quote(leftDelim) + "(\\w+?)" + quote(rightDelim)).matcher(pattern);
    StringBuilder regexpifiedPatterm = new StringBuilder();
    List<String> params = new ArrayList<String>();
    while (matcher.find()) {
      params.add(matcher.group(1));
      regexpifiedPatterm.append(quote(nextSlice(matcher))).append("(.*?)");
    }
    regexpifiedPatterm.append(quote(finalSlice(matcher)));
    return new StringOuterpolator(compile(regexpifiedPatterm.toString()), params);
  }

  private static String nextSlice(Matcher matcher) {
    StringBuffer slice = new StringBuffer();
    matcher.appendReplacement(slice, "");
    return slice.toString();
  }

  private static String finalSlice(Matcher matcher) {
    StringBuffer slice = new StringBuffer();
    matcher.appendTail(slice);
    return slice.toString();
  }

  public Map<String, String> outerpolate(String string) {
    Matcher matcher = pattern.matcher(string);
    if (!matcher.matches()) {
      return Collections.emptyMap();
    }
    Map<String, String> res = new HashMap<String, String>(params.size());
    int pos = 1;
    for (String param : params) {
      res.put(param, matcher.group(pos++));
    }
    return res;
  }
}
