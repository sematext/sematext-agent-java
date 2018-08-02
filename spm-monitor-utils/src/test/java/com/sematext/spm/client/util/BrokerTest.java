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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Collections;

public class BrokerTest {
  @Test
  public void testConsume() {
    Clocks.Mock clock = Clocks.mock();
    Broker<String> broker = new Broker<String>(3, clock);
    broker.publish("1");
    assertEquals(Collections.singleton("1"), broker.consume());
    assertEquals(Collections.emptySet(), broker.consume());
    broker.publish("2");
    broker.publish("3");
    broker.publish("4");
    assertEquals(Collections.emptySet(), broker.consume());
    clock.increment(3);
    assertEquals(3, broker.consume().size());
    assertEquals(0, broker.consume().size());

    clock.increment(3);
    assertEquals(0, broker.consume().size());

    broker.publish("5");
    clock.increment(3);
    assertEquals(Collections.singleton("5"), broker.consume());
  }
}
