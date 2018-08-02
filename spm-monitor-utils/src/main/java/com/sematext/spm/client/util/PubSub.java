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

import java.util.concurrent.TimeUnit;

public abstract class PubSub<ELEMENT, CONTAINER> extends PubSubSupport {
  private volatile CONTAINER values;

  public PubSub(long consumptionIntervalMillis, Clock clock, CONTAINER init) {
    super(consumptionIntervalMillis, clock);
    this.values = init;
  }

  public PubSub(CONTAINER init) {
    this(TimeUnit.MINUTES.toMillis(1), Clocks.WALL, init);
  }

  protected CONTAINER container() {
    return values;
  }

  public abstract void publish(ELEMENT element);

  public CONTAINER peek() {
    return container();
  }

  protected abstract CONTAINER newContainer();

  protected abstract CONTAINER emptyContainer();

  @SuppressWarnings("unchecked")
  public CONTAINER consume() {
    if (canConsume()) {
      final CONTAINER old = values;
      values = newContainer();
      return old;
    }
    return emptyContainer();
  }
}
