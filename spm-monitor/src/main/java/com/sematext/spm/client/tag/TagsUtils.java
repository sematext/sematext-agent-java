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

package com.sematext.spm.client.tag;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sematext.spm.client.StatsCollectorBadConfigurationException;
import com.sematext.spm.client.util.StringUtils;

public final class TagsUtils {
  private static final String TAG_KEY_VALUE_SEPARATOR = ":";
  private static final String KEY_VALUE_PATTERN = "[a-zA-Z0-9_\\-=\\+\\.]*";

  private TagsUtils() {
  }

  public static Set<String> parseTags(String tagsString) throws StatsCollectorBadConfigurationException {
    if (StringUtils.isEmpty(tagsString)) {
      return new HashSet<String>();
    }

    String[] tagsArray = tagsString.split(",");

    Pattern keyValuePattern = Pattern.compile(KEY_VALUE_PATTERN);

    Set<String> tags = new HashSet<String>();

    for (String tagString : tagsArray) {
      String trimmedTag = StringUtils.trim(tagString);
      if (!StringUtils.isEmpty(trimmedTag)) {

        if (!trimmedTag.contains(TAG_KEY_VALUE_SEPARATOR)) {
          throw new StatsCollectorBadConfigurationException(
              "Tag MUST contain separator ':' between key and value, like 'env:foo' current tags: " + trimmedTag);
        }

        if (org.apache.commons.lang.StringUtils.countMatches(trimmedTag, TAG_KEY_VALUE_SEPARATOR) > 1) {
          throw new StatsCollectorBadConfigurationException(
              "Tag MUST contain 1 separator ':' between key and value, like 'env:foo' current tags: " + trimmedTag);
        }

        String[] keyValue = trimmedTag.split(TAG_KEY_VALUE_SEPARATOR);

        if (keyValue.length != 2) {
          throw new StatsCollectorBadConfigurationException(
              "Tag MUST contain 1 separator ':' between key and value, key and value can't be empty, current tags: "
                  + trimmedTag);
        }

        Matcher keyMatcher = keyValuePattern.matcher(keyValue[0]);
        if (!keyMatcher.matches()) {
          throw new StatsCollectorBadConfigurationException(
              "Tag's key can't be empty, can contain alphanumeric characters and _-+=. : " + keyValue[0]);
        }

        Matcher valueMatcher = keyValuePattern.matcher(keyValue[1]);
        if (!valueMatcher.matches()) {
          throw new StatsCollectorBadConfigurationException(
              "Tag's value can't be empty, can contain alphanumeric characters and _-+=. : " + keyValue[1]);
        }

        tags.add(trimmedTag);
      }
    }
    return tags;
  }
}
