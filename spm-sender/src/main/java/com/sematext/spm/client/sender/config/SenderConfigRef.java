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
package com.sematext.spm.client.sender.config;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

public final class SenderConfigRef {
  private static final Log LOG = LogFactory.getLog(SenderConfigRef.class);

  private final SenderConfigFactory factory;
  private final String token;
  private ChangeWatcher.Watch watch;
  private final boolean shouldCheckStale;

  public SenderConfigRef(String token, SenderConfigFactory factory, ChangeWatcher.Watch watch,
                         boolean shouldCheckStale) {
    this.factory = factory;
    this.token = token;
    this.watch = watch;
    this.shouldCheckStale = shouldCheckStale;
  }

  public boolean updated() {
    return watch.isChanged();
  }

  public void updateSenderConfigRef(SenderConfigRef newRef) {
    this.watch = newRef.getChangeWatcher();
  }

  public ChangeWatcher.Watch getChangeWatcher() {
    return watch;
  }

  public boolean isShouldCheckStale() {
    return shouldCheckStale;
  }

  public SenderConfig getConfig() throws Exception {
    return factory.getConfig();
  }

  public String getToken() {
    return token;
  }

  /*CHECKSTYLE:OFF*/
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SenderConfigRef that = (SenderConfigRef) o;

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
