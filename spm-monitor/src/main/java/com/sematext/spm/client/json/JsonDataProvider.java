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
package com.sematext.spm.client.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;

import com.sematext.spm.client.http.HttpDataProvider;
import com.sematext.spm.client.http.HttpDataSourceAuthentication;

public class JsonDataProvider extends HttpDataProvider<Object> {
  private static final ObjectMapper JSON_MAPPER_STANDARD = new ObjectMapper(new JsonFactory());
  private static final ObjectMapper JSON_MAPPER_SMILE = new ObjectMapper(new SmileFactory());

  private boolean useSmile = false;

  private CustomJsonHandler<Object> customJsonHandler;

  public JsonDataProvider(boolean https, String host, String port, String dataRequestUrl,
                          HttpDataSourceAuthentication auth, boolean useSmile) {
    super(https, host, port, dataRequestUrl, auth);
    this.useSmile = useSmile;
  }

  public JsonDataProvider(HttpDataSourceAuthentication auth, boolean useSmile) {
    super(auth);
    this.useSmile = useSmile;
  }

  public JsonDataProvider(String dataRequestUrl, HttpDataSourceAuthentication auth, boolean useSmile) {
    super(dataRequestUrl, auth);
    this.useSmile = useSmile;
  }

  /**
   * Handles any error handling if necessary, returns InputStream for response content
   *
   * @param response
   * @return
   * @throws IOException
   */
  @Override
  protected Object handleResponse(HttpResponse response) throws IOException {
    HttpEntity entity = response.getEntity();

    if (entity != null) {
      InputStream is = null;

      try {
        is = entity.getContent();
        TypeReference<Object> typeRef = new TypeReference<Object>() {
        };

        if (useSmile) {
          if (customJsonHandler == null) {
            return JSON_MAPPER_SMILE.readValue(is, typeRef);
          } else {
            return customJsonHandler.parse(is, JSON_MAPPER_SMILE.getFactory());
          }
        } else {
          if (customJsonHandler == null) {
            return JSON_MAPPER_STANDARD.readValue(is, typeRef);
          } else {
            return customJsonHandler.parse(is, JSON_MAPPER_STANDARD.getFactory());
          }
        }

      } finally {
        if (is != null) {
          is.close();
        }

        EntityUtils.consume(entity);
      }
    }

    return null;
  }

  @Override
  protected void setHeaders(HttpRequestBase request) {
    if (useSmile) {
      request.setHeader(HTTP.CONTENT_TYPE, "application/smile");
    }
  }

  public CustomJsonHandler<Object> getCustomJsonHandler() {
    return customJsonHandler;
  }

  public void setCustomJsonHandler(CustomJsonHandler<Object> customJsonHandler) {
    this.customJsonHandler = customJsonHandler;
  }

  @Override
  public String toString() {
    return this.getClass().getName() + "[" + super.toString() + "]. smile=" + useSmile;
  }

}
