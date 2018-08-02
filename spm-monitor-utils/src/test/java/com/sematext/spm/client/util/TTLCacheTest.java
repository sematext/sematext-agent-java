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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class TTLCacheTest {
  private static class TestClock implements TTLCache.Clock {
    private long time = 0;

    void tick() {
      time++;
    }

    void set(long time) {
      this.time = time;
    }

    @Override
    public long now() {
      return time;
    }
  }

  private static Set<String> set(String... values) {
    Set<String> s = new HashSet<String>();
    for (String value : values) s.add(value);
    return s;
  }

  @Test
  public void testShouldInsertNewEntry() {
    final TestClock clock = new TestClock();
    final TTLCache<String, String> c = new TTLCache<String, String>(1, clock);
    assertNull(c.putIfAbsent("42", "42"));
    assertTrue(c.contains("42"));
    assertEquals("42", c.get("42"));
    assertEquals("42", c.putIfAbsent("42", "24"));
    clock.tick();
    clock.tick();
    assertFalse(c.contains("42"));
    assertNull(c.get("42"));
  }

  @Test
  public void testShouldNotReturnExpiredEntries() {
    final TestClock clock = new TestClock();
    final TTLCache<String, String> c = new TTLCache<String, String>(1, clock);
    c.putIfAbsent("1", "1");
    c.putIfAbsent("2", "2");
    c.putIfAbsent("3", "3");

    assertEquals(set("1", "2", "3"), c.values());
    clock.tick();
    clock.tick();
    assertEquals(set(), c.values());

    c.putIfAbsent("1", "1");
    c.putIfAbsent("2", "2");
    clock.tick();
    c.putIfAbsent("3", "3");
    clock.tick();

    assertEquals(set("3"), c.values());
  }
}
