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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AgentArgsParser {

  public static final String TOKEN_PARAM = "token";
  public static final String SUB_TYPE_PARAM = "sub-type";
  public static final String JVM_NAME_PARAM = "jvm-name";

  private AgentArgsParser() {
  }

  private static final Pattern STANDALONE_PROPERTIES_FILE_NAME_RE = Pattern
      .compile("^spm-monitor-(.*?)-?config-([\\w-]{36})-(\\w+).properties$");

  public static Map<String, String> parse(String args) {
    final Map<String, String> p = new HashMap<String, String>();
    if (args == null) {
      return p;
    }
    final String[] pairs = args.split(",");
    for (final String pair : pairs) {
      final String[] kv = pair.split("=");
      if (kv.length == 2) {
        p.put(kv[0], kv[1]);
      }
    }
    return p;
  }

  public static Map<String, String> parseColonSeparated(String args) {
    Map<String, String> p = new HashMap<String, String>();
    String[] params = args.split(":");
    if (params.length == 3) {
      p.put(TOKEN_PARAM, params[0]);
      p.put(SUB_TYPE_PARAM, params[1]);
      p.put(JVM_NAME_PARAM, params[2]);
    }
    return p;
  }

  public static Map<String, String> parseConfigFileName(String args) {
    String propertiesFileName = args.substring(args.lastIndexOf(File.separatorChar) + 1);
    Matcher matcher = STANDALONE_PROPERTIES_FILE_NAME_RE.matcher(propertiesFileName);
    Map<String, String> p = new HashMap<String, String>();
    if (matcher.matches()) {
      p.put(SUB_TYPE_PARAM, matcher.group(1));
      p.put(TOKEN_PARAM, matcher.group(2));
      p.put(JVM_NAME_PARAM, matcher.group(3));
    }
    return p;
  }

  public static Map<String, String> parseCommandLineArguments(String args) {
    if (args.endsWith(".properties")) {
      return parseConfigFileName(args);
    }
    return parseColonSeparated(args);
  }

}
