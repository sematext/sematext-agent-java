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

import java.util.Map;

public final class WellknownParams {

  private WellknownParams() {
    // It's utiltiy class.
  }

  public static String jvmName(Map<String, String> paramsMap) {
    return paramsMap.get("jvmName");
  }

  public static String[] configFiles(Map<String, String> paramsMap) {
    if (!paramsMap.containsKey("configFile")) {
      return new String[0];
    }

    String[] configFiles = paramsMap.get("configFile").split(",");

    String[] trimmedConfigFiles = new String[configFiles.length];

    for (int i = 0; i < configFiles.length; i++) {
      trimmedConfigFiles[i] = configFiles[i].trim();
    }

    return trimmedConfigFiles;
  }

  public static String nodeType(Map<String, String> paramsMap) {
    return paramsMap.get("nodeType");
  }
}
