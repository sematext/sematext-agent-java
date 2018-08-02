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
package com.sematext.spm.client.tracing.agent.impl;

import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Collections;

import com.sematext.spm.client.tracing.agent.SinkEvent;
import com.sematext.spm.client.tracing.agent.errors.TracingError;
import com.sematext.spm.client.util.test.TmpFS;

public class DisruptorThriftEventSinkTest {
  @Test
  public void testSink() throws Exception {
    final TmpFS fs = TmpFS.fs();
    try {
      final DisruptorThriftEventSink sink = DisruptorThriftEventSink
          .create(fs.createDirectory().getPath(), "tracing", ".bin", 10000, 5);
      TracingError error1 = new TracingError("token-1", -1L, -1L, -1L, System
          .currentTimeMillis(), true, Collections.<String, String>emptyMap());
      TracingError error2 = new TracingError("token-1", -1L, -1L, -1L, System
          .currentTimeMillis(), true, Collections.<String, String>emptyMap());
      sink.sink(new SinkEvent<TracingError>(error1));
      sink.sink(new SinkEvent<TracingError>(error2));

      try {
        sink.sink(new SinkEvent(new Object()));
        fail("Sink shouldn't accept unknown events");
      } catch (IllegalArgumentException e) {
      }

    } finally {
      fs.cleanup();
    }
  }
}
