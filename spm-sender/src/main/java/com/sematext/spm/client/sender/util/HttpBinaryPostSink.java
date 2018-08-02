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
package com.sematext.spm.client.sender.util;

import org.apache.flume.Event;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

public class HttpBinaryPostSink extends HttpPostSink {
  private static final Log LOG = LogFactory.getLog(HttpBinaryPostSink.class);

  @Override
  protected void post(List<Event> events) throws IOException {
    final HttpURLConnection c = (HttpURLConnection) new URL(getFullUrlParamsString()).openConnection();
    c.setRequestProperty("Content-Type", getContentType());
    c.setDoInput(true);
    c.setDoOutput(true);

    final OutputStream os = c.getOutputStream();
    final DataOutputStream daos = new DataOutputStream(os);
    for (final Event event : events) {
      daos.writeInt(event.getBody().length);
      daos.write(event.getBody());
    }

    int rc = c.getResponseCode() - 200;
    if (rc >= 0 && rc < 100) {
      LOG.info(events.size() + " are posted to url " + getUrl() + " successfully, response code: " + c.getResponseCode()
                   + ".");
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug(
            events.size() + " can't be posted to url " + getUrl() + ", response code: " + c.getResponseCode() + ".");
      }

      throw new IOException("Non success http response: " + c.getResponseCode() + ".");
    }
  }
}
