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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class AsyncContextTest {

  private static class MyThread {
    private int hashCodeCalled;
    private int equalsCalled;
    private final String name;

    public MyThread(String name) {
      this.name = name;
    }

    @Override
    public boolean equals(Object o) {
      equalsCalled++;
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      MyThread myThread = (MyThread) o;

      if (name != null ? !name.equals(myThread.name) : myThread.name != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      hashCodeCalled++;
      return name != null ? name.hashCode() : 0;
    }
  }

  @Test
  public void testShouldStoreAsyncContextByObjectIdentity() {
    final MyThread t1 = new MyThread("t1");
    final MyThread t2 = new MyThread("t2");

    AsyncContext.create(t1, 1L, 2L, false);

    assertNull(AsyncContext.get(t2));
    assertEquals(new Long(1L), AsyncContext.get(t1).getTraceId());
    assertEquals(new Long(2L), AsyncContext.get(t1).getParentCallId());

    AsyncContext.create(t2, 1L, 3L, false);

    assertEquals(new Long(2L), AsyncContext.get(t1).getParentCallId());
    assertEquals(new Long(3L), AsyncContext.get(t2).getParentCallId());

    assertEquals(0, t1.equalsCalled);
    assertEquals(0, t1.hashCodeCalled);

    assertEquals(0, t2.equalsCalled);
    assertEquals(0, t2.hashCodeCalled);
  }

}
