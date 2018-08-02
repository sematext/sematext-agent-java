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

package com.sematext.spm.client.tracing.agent.httpclient4;

import static com.sematext.spm.client.tracing.agent.httpclient4.HttpClientURL.makeUrl;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HttpClientURLTest {
  private static final class Host implements HttpHostAccess {
    private final String hostname;
    private final int port;

    private Host(String hostname, int port) {
      this.hostname = hostname;
      this.port = port;
    }

    @Override
    public String _$spm_tracing$_getHostName() {
      return hostname;
    }

    @Override
    public int _$spm_tracing$_getPort() {
      return port;
    }

    @Override
    public String _$spm_tracing$_getSchemeName() {
      return "http";
    }
  }

  private static final class Line implements RequestLineAccess {
    private final String uri;

    public Line(String uri) {
      this.uri = uri;
    }

    @Override
    public String _$spm_tracing$_getMethod() {
      return null;
    }

    @Override
    public String _$spm_tracing$_getUri() {
      return uri;
    }
  }

  @Test
  public void testMakeUrl() throws Exception {
    assertEquals("http://localhost:2345/add-user", makeUrl(new Host("localhost", 2345), new Line("/add-user")));
    assertEquals(null, makeUrl(new Host("localhost", 2345), new Line("add-user")));
    assertEquals("http://localhost:2345/add-user", makeUrl(new Host("localhost", 2344), new Line("http://localhost:2345/add-user")));
    assertEquals("http://localhost:2345/add-user", makeUrl(null, new Line("http://localhost:2345/add-user")));
    assertEquals("http://localhost:2345/add-user?name=jack&age=53", makeUrl(new Host("localhost", 2345), new Line("/add-user?name=jack&age=53")));
  }
}
