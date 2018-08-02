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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Broker<A> extends PubSub<A, Set<A>> {
  public Broker(long consumptionIntervalMillis, Clock clock) {
    super(consumptionIntervalMillis, clock, Collections.newSetFromMap(new ConcurrentHashMap<A, Boolean>()));
  }

  public Broker() {
    super(Collections.newSetFromMap(new ConcurrentHashMap<A, Boolean>()));
  }

  @Override
  public void publish(A a) {
    container().add(a);
  }

  @Override
  protected Set<A> newContainer() {
    return Collections.newSetFromMap(new ConcurrentHashMap<A, Boolean>());
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Set<A> emptyContainer() {
    return Collections.EMPTY_SET;
  }
}
