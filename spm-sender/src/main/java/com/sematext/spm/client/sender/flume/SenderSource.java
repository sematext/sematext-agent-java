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
package com.sematext.spm.client.sender.flume;

import org.apache.flume.ChannelException;
import org.apache.flume.Event;
import org.apache.flume.agent.embedded.EmbeddedSource;

import java.util.Collection;
import java.util.List;

/**
 * Not used at the moment, but would be useful in case of single sink and N sources.
 */
public class SenderSource extends EmbeddedSource {
  public enum Type {
    MULTIPLE_APPLICATIONS,
    SINGLE_APPLICATION
  }

  private String appTokens;

  private Type type;

  /**
   * application token for which this source is used; can be comma-separated list of tokens
   */
  public SenderSource(String appTokens, Type type) {
    this.appTokens = appTokens;
    this.type = type;
  }

  public void addAppToken(String newAppToken) {
    if (!appTokens.contains(newAppToken)) {
      appTokens = appTokens + "," + newAppToken;
    }
  }

  public void removeAppToken(String tokenToRemove) {
    if (appTokens.contains(tokenToRemove)) {
      String tmp = appTokens.replace(tokenToRemove, "");
      // if this produced two consecutive commas...
      tmp = tmp.replace(",,", ",").trim();

      if (tmp.equals(",")) {
        tmp = "";
      }

      appTokens = tmp;
    }
  }

  public void setAppTokens(Collection<String> tokens) {
    StringBuilder sb = new StringBuilder();

    for (String token : tokens) {
      if (sb.length() != 0) {
        sb.append(";");
      }
      sb.append(token);
    }

    appTokens = sb.toString();
  }

  public String getAppTokens() {
    return appTokens;
  }

  /*CHECKSTYLE:OFF*/
  @Override
  public void put(Event event) throws ChannelException {
    if (appTokens == null || appTokens.trim().equals("")) {
      // ignore events without appTokens
      return;
    }

    event.getHeaders().put("appTokens", appTokens);

    super.put(event);
  }
  /*CHECKSTYLE:ON*/

  /*CHECKSTYLE:OFF*/
  @Override
  public void putAll(List<Event> events) throws ChannelException {
    if (appTokens == null || appTokens.trim().equals("")) {
      // ignore events without appTokens
      return;
    }

    if (events != null) {
      for (Event event : events) {
        event.getHeaders().put("appTokens", appTokens);
      }
    }

    super.putAll(events);
  }
  /*CHECKSTYLE:ON*/

  public Type getType() {
    return type;
  }
}
