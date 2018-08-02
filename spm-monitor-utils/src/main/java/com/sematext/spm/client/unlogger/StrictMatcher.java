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
package com.sematext.spm.client.unlogger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple wrapper around java matcher, allow to compoud submatchers.
 */
public final class StrictMatcher {

  private final Pattern pattern;
  private final Map<Integer, Reparse> reparseMap;

  private StrictMatcher(Pattern pattern, Map<Integer, Reparse> reparseMap) {
    super();
    this.pattern = pattern;
    this.reparseMap = reparseMap;
  }

  public static final class Builder {

    private final Pattern rootPattern;
    private final Map<Integer, Reparse> reparseMap = new HashMap<Integer, Reparse>();

    private Builder(Pattern rootPattern) {
      this.rootPattern = rootPattern;
    }

    public Builder reparseGroupAsMap(int groupId, String pattern, int keyIndex, int valueIndex) {
      return reparseGroupAsMap(groupId, pattern, keyIndex, valueIndex, true);
    }

    public Builder reparseGroupAsMap(int groupId, String pattern, int keyIndex, int valueIndex, boolean checkUniques) {
      reparseMap.put(groupId, new Reparse(Pattern.compile(pattern), keyIndex, valueIndex, checkUniques));
      return this;
    }

    public StrictMatcher make() {
      return new StrictMatcher(rootPattern, reparseMap);
    }

  }

  private static final class Reparse {
    private final Pattern pattern;
    private final int keyIndex;
    private final int valueIndex;
    private final boolean duplicationCheck;

    private Reparse(Pattern pattern, int keyIndex, int valueIndex, boolean duplicationCheck) {
      this.pattern = pattern;
      this.keyIndex = keyIndex;
      this.valueIndex = valueIndex;
      this.duplicationCheck = duplicationCheck;
    }
  }

  public static Builder rootPattern(String pattern) {
    return new Builder(Pattern.compile(pattern));
  }

  public final Result match(String string) {
    Matcher matcher = pattern.matcher(string);
    if (!matcher.matches()) {
      return null;
    }
    return new Result(matcher);
  }

  public final class Result {
    private final Matcher matcher;

    private Result(Matcher matcher) {
      this.matcher = matcher;
    }

    public String get(int id) {
      return matcher.group(id);
    }

    public <T> T get(int id, Class<T> type) {
      if (!reparseMap.containsKey(id)) {
        return null;
      }
      if (LinkedHashMap.class != type) {
        // We will add another types if them needed.
        return null;
      }

      Reparse reparse = reparseMap.get(id);
      return (T) computeParams(reparse, matcher.group(id));
    }
  }

  private static LinkedHashMap<String, String> computeParams(Reparse reparse, String stringRepresentation) {
    stringRepresentation = stringRepresentation.trim();
    if (stringRepresentation.isEmpty()) {
      return new LinkedHashMap<String, String>();
    }

    LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();

    Matcher paramsMatcher = reparse.pattern.matcher(stringRepresentation);
    int parsedSymbols = 0;
    while (paramsMatcher.find()) {
      String key = paramsMatcher.group(reparse.keyIndex);
      String value = paramsMatcher.group(reparse.valueIndex);
      parsedSymbols = paramsMatcher.end();

      if (reparse.duplicationCheck && params.containsKey(key)) {
        throw new IllegalStateException("Duplicated param: " + key);
      }

      params.put(key, value);
    }

    if (parsedSymbols < stringRepresentation.length()) {
      throw new IllegalStateException("Something wrong in params: " + stringRepresentation);
    }

    return params;
  }

}
