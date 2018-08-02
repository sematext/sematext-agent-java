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
package com.sematext.spm.client.tracing.agent.util;

public final class JdbcURL {
  private JdbcURL() {
  }

  public static String getHostname(String url) {
    int i = 0;
    char[] urlChars = url.toCharArray();
    while (i < urlChars.length && urlChars[i] != '/') {
      i++;
    }
    if (i < urlChars.length - 1 && urlChars[i] == '/' && urlChars[i + 1] == '/') {
      i += 2;
    } else {
      return null;
    }
    final StringBuilder hostname = new StringBuilder();
    for (; i < urlChars.length && urlChars[i] != ':' && urlChars[i] != '/'; i++) {
      hostname.append(urlChars[i]);
    }
    return hostname.toString();
  }
}
