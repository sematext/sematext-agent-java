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

public final class ESAction {

  public static enum OperationType {
    INDEX, DELETE, SEARCH, GET, UPDATE
  }

  private final OperationType operationType;
  private final String index;
  private final String type;
  private final String query;
  private final int count;

  public ESAction(OperationType operationType, String index, String type, String query) {
    this(operationType, index, type, query, 1);
  }

  public ESAction(OperationType operationType, String index, String type, String query, int count) {
    this.operationType = operationType;
    this.index = index;
    this.type = type;
    this.query = query;
    this.count = count;
  }

  public OperationType getOperationType() {
    return operationType;
  }

  public String getIndex() {
    return index;
  }

  public String getType() {
    return type;
  }

  public String getQuery() {
    return query;
  }

  public int getCount() {
    return count;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ESAction esAction = (ESAction) o;

    if (count != esAction.count) return false;
    if (index != null ? !index.equals(esAction.index) : esAction.index != null) return false;
    if (operationType != esAction.operationType) return false;
    if (query != null ? !query.equals(esAction.query) : esAction.query != null) return false;
    if (type != null ? !type.equals(esAction.type) : esAction.type != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = operationType != null ? operationType.hashCode() : 0;
    result = 31 * result + (index != null ? index.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (query != null ? query.hashCode() : 0);
    result = 31 * result + count;
    return result;
  }
}
