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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.ByteBuffer;

public class ByteBufferAllocatorTest {
  @Test
  public void testAllocate() throws Exception {
    final ByteBufferAllocator allocator = ByteBufferAllocator.allocator();

    ByteBuffer b1 = allocator.allocate(1024);
    assertEquals(1024, b1.limit());
    assertEquals(0, b1.position());
    assertEquals(1024, b1.capacity());

    b1.putInt(42);

    ByteBuffer b2 = allocator.allocate(1024);
    assertEquals(1024, b2.limit());
    assertEquals(0, b2.position());
    assertEquals(1024, b2.capacity());

    assertTrue(b1 == b2);

    ByteBuffer b3 = allocator.allocate(2048);
    assertEquals(2048, b3.limit());
    assertEquals(0, b3.position());
    assertEquals(2048, b3.capacity());

    assertTrue(b3 != b1);

    ByteBuffer b4 = allocator.allocate(1024);
    assertEquals(1024, b4.limit());
    assertEquals(0, b4.position());
    assertEquals(2048, b4.capacity());
  }
}
