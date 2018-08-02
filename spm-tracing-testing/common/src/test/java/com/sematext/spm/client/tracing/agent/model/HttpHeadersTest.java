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

package com.sematext.spm.client.tracing.agent.model;

import static com.sematext.spm.client.tracing.agent.model.HttpHeaders.decodeCrossAppCallHeader;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.sematext.spm.client.tracing.agent.model.HttpHeaders.CrossAppCallHeader;

public class HttpHeadersTest {
  @Test
  public void shouldEncodeCrossAppHeader() {
    String encoded = HttpHeaders
        .encodeCrossAppCallHeader(1L, 2L, 3L, 10L, "token", "request", new Endpoint("127.0.0.1", "localhost"), false);
    assertEquals("token;1;2;3;10;request;127.0.0.1;localhost;false", encoded);
  }

  @Test
  public void shouldDecodeValidCrossAppHeader() {
    String encoded = "token;1;2;3;10;request;127.0.0.1;localhost;false";
    CrossAppCallHeader header = decodeCrossAppCallHeader(encoded);
    assertNotNull("valid header should be decoded", header);
    assertEquals(1L, header.getCallId());
    assertEquals(2L, header.getParentCallId());
    assertEquals(3L, header.getTraceId());
    assertEquals(10L, header.getDuration());
    assertEquals("token", header.getToken());
    assertEquals("request", header.getRequest());
    assertEquals("127.0.0.1", header.getEndpoint().getAddress());
    assertEquals("localhost", header.getEndpoint().getHostname());
    assertFalse(header.isSampled());
  }

  @Test
  public void shouldReturnNullIfHeaderIsNotValid() {
    assertNull(decodeCrossAppCallHeader("token;1;2;3;10"));
    assertNull(decodeCrossAppCallHeader("token;1;2;3;10;token;request"));
    assertNull(decodeCrossAppCallHeader("token;1;2;3;10;request;127.0.0.1"));
  }
}
