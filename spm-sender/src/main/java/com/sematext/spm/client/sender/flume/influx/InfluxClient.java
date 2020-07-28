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

/*CHECKSTYLE:OFF*/
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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
package com.sematext.spm.client.sender.flume.influx;

import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;

import java.util.HashMap;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.status.AgentStatusRecorder;

public abstract class InfluxClient {
  private static final Log logger = LogFactory.getLog(InfluxClient.class);

  public static final String URL_PARAM_VERSION = "v";
  public static final String URL_PARAM_CONTENT_TYPE = "sct";

  protected StringBuilder bulkBuilder;
  protected String urlPath;
  protected String urlParamsString;

  protected Map<String, String> urlParams = new HashMap<String, String>();

  // private InfluxDB influxDB;

  public InfluxClient() {
    // better assume big capacity is needed right away instead of creating tons of objects while appending
    bulkBuilder = new StringBuilder(100000);

    // influxDB = InfluxDBFactory.connect("https://spm-receiver.sematext.com/...",
    //    "username", "password");
  }

  public void addEvent(Event event) throws Exception {
    String body = new String(event.getBody());

    // windows fix
    body = body.replaceAll("\\r", "");

    synchronized (bulkBuilder) {
      bulkBuilder.append(body);
      bulkBuilder.append("\n");
    }
  }

  public void execute() throws Exception {
    String entity;

    synchronized (bulkBuilder) {
      if (bulkBuilder.length() == 0) {
        // nothing to send
        return;
      }

      entity = bulkBuilder.toString();
      // delete so we reuse bulkBuilder
      bulkBuilder.delete(0, bulkBuilder.length());
    }

    boolean somethingSuccessfullySent = sendAndHandleResponse(entity);
    
    if (somethingSuccessfullySent && isMetricsEndpoint()) {
      if (AgentStatusRecorder.GLOBAL_INSTANCE != null) {
        AgentStatusRecorder.GLOBAL_INSTANCE.updateMetricsSent(true);
      }
    }
  }

  protected abstract boolean isMetricsEndpoint();

  protected abstract boolean sendAndHandleResponse(String entity) throws EventDeliveryException;

  protected abstract void initializeUrlVariables();

  public void updateUrlParameter(String paramName, String paramValue) {
    // TODO handle this, params can change dynamically (receiver url, some params in it...)
    this.urlParams.put(paramName, paramValue);
    initializeUrlVariables();
  }

  public abstract void close();
}
/*CHECKSTYLE:ON*/
