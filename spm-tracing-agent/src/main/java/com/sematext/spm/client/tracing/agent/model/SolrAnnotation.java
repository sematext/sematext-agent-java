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

import java.util.Map;

public final class SolrAnnotation {

  public static enum RequestType {
    SCHEMA, UPDATE, COLLECTION_ADMIN, CORE_ADMIN, ANALYSIS, OTHER, QUERY
  }

  private String collection;
  private boolean succeed;
  private String url;
  private Map<String, String> params;
  private RequestType requestType;
  private int responseStatus;

  public String getCollection() {
    return collection;
  }

  public void setCollection(String collection) {
    this.collection = collection;
  }

  public boolean isSucceed() {
    return succeed;
  }

  public void setSucceed(boolean succeed) {
    this.succeed = succeed;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Map<String, String> getParams() {
    return params;
  }

  public void setParams(Map<String, String> params) {
    this.params = params;
  }

  public RequestType getRequestType() {
    return requestType;
  }

  public void setRequestType(RequestType requestType) {
    this.requestType = requestType;
  }

  public int getResponseStatus() {
    return responseStatus;
  }

  public void setResponseStatus(int responseStatus) {
    this.responseStatus = responseStatus;
  }

  @Override
  public String toString() {
    return "SolrAnnotation{" +
        "collection='" + collection + '\'' +
        ", succeed=" + succeed +
        ", baseURL='" + url + '\'' +
        ", params=" + params +
        ", requestType=" + requestType +
        ", responseStatus=" + responseStatus +
        '}';
  }
}
