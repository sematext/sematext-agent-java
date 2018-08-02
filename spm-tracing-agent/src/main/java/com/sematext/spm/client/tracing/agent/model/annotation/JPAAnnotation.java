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
package com.sematext.spm.client.tracing.agent.model.annotation;

public final class JPAAnnotation {
  private String type;
  private String query;
  private String object;
  private int count;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getObject() {
    return object;
  }

  public void setObject(String object) {
    this.object = object;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public static JPAAnnotation find(String object) {
    final JPAAnnotation annotation = new JPAAnnotation();
    annotation.setType("find");
    annotation.setObject(object);
    return annotation;
  }

  public static JPAAnnotation query(String query, String object) {
    final JPAAnnotation annotation = new JPAAnnotation();
    annotation.setType("query");
    annotation.setObject(object);
    annotation.setQuery(query);
    return annotation;
  }
}
