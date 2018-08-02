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

import java.nio.ByteBuffer;

public final class ByteBufferAllocator {
  private final ThreadLocal<ByteBuffer> localBuffer = new ThreadLocal<ByteBuffer>();
  private final int chunkLength;

  private ByteBufferAllocator(int chunkLength) {
    this.chunkLength = chunkLength;
  }

  public ByteBuffer allocate(int length) {
    ByteBuffer buffer = localBuffer.get();
    if (buffer == null) {
      buffer = ByteBuffer.allocate(length <= chunkLength ? chunkLength : length);
      localBuffer.set(buffer);
    } else {
      if (buffer.capacity() < length) {
        buffer = ByteBuffer.allocate(length);
        localBuffer.set(buffer);
      }
    }
    buffer.clear();
    buffer.limit(length);
    return buffer;
  }

  public static ByteBufferAllocator allocator() {
    return new ByteBufferAllocator(1024);
  }
}
