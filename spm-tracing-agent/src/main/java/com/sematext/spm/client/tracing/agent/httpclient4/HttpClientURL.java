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

import java.net.URI;
import java.net.URISyntaxException;

public final class HttpClientURL {
  private HttpClientURL() {
  }

  public static String makeUrl(HttpHostAccess host, RequestLineAccess line) {
    final String requestUri = line._$spm_tracing$_getUri();
    try {
      final URI uri = new URI(requestUri);
      if (uri.isAbsolute()) {
        return requestUri;
      } else {
        final URI absoluteUri = new URI(host._$spm_tracing$_getSchemeName(), null,
                                        host._$spm_tracing$_getHostName(), host._$spm_tracing$_getPort(), uri
                                            .getPath(), uri.getQuery(), uri.getFragment());
        return absoluteUri.toString();
      }
    } catch (URISyntaxException e) {
      return null;
    }
  }
}
