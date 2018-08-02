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
package com.sematext.spm.client.es;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.http.ServerInfo;
import com.sematext.spm.client.json.CustomJsonHandler;

public class CustomEsClusterStateJsonHandler implements CustomJsonHandler<Map<String, Object>> {
  private static final Log LOG = LogFactory.getLog(CustomEsClusterStateJsonHandler.class);

  private ServerInfo jsonServerInfo;

  @Override
  public Map<String, Object> parse(InputStream inputStream, JsonFactory jsonFactory) throws IOException {
    JsonParser jsonParser = jsonFactory.createParser(inputStream);
    TypeReference<UnifiedMap<String, Object>> typeRef = new TypeReference<UnifiedMap<String, Object>>() {
    };

    Map<String, Object> res = new UnifiedMap<String, Object>();

    while (true) {
      CustomJsonHandlerUtil.moveToNextObject(jsonParser, true);

      if (jsonParser.getCurrentToken() == null) {
        LOG.error("Found null token where field was expected, exiting...");
        return null;
      }

      String currentFieldName = jsonParser.getCurrentName();
      jsonParser.nextToken();

      if ("cluster_name".equals(currentFieldName)) {
        res.put(currentFieldName, jsonParser.readValueAs(String.class));
      } else if ("master_node".equals(currentFieldName)) {
        res.put(currentFieldName, jsonParser.readValueAs(String.class));
      } else if ("nodes".equals(currentFieldName) || "routing_nodes".equals(currentFieldName)) {
        res.put(currentFieldName, jsonParser.readValueAs(typeRef));
      } else {
        jsonParser.skipChildren();
      }

      jsonParser.nextToken();

      if (jsonParser.getCurrentToken() == JsonToken.END_OBJECT) {
        jsonParser.nextToken();
        break;
      }
    }

    return res;
  }

  @Override
  public void setJsonServerInfo(ServerInfo jsonServerInfo) {
    this.jsonServerInfo = jsonServerInfo;
  }
}
