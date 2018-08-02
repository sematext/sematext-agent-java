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

import com.sematext.spm.client.tracing.agent.sql.SqlStatement;
import com.sematext.spm.client.tracing.agent.sql.SqlStatementParser;

public final class SQLAnnotation {
  private String type;
  private String sql;
  private String url;
  private int resultCount;
  private String table;
  private SqlStatement.Operation operation;

  private SQLAnnotation() {
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getSql() {
    return sql;
  }

  public void setSql(String sql) {
    this.sql = sql;
  }

  public int getCount() {
    return resultCount;
  }

  public void setResultCount(int resultCount) {
    this.resultCount = resultCount;
  }

  public String getTable() {
    return table;
  }

  public void setTable(String table) {
    this.table = table;
  }

  public SqlStatement.Operation getOperation() {
    return operation;
  }

  public void setOperation(SqlStatement.Operation operation) {
    this.operation = operation;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public static SQLAnnotation make(String query, String url) {
    final SQLAnnotation annotation = new SQLAnnotation();
    annotation.setType("query");
    annotation.setSql(query);
    annotation.setUrl(url);

    final SqlStatement statement = SqlStatementParser.parse(query);
    annotation.setTable(statement.getTable());
    annotation.setOperation(statement.getOperation());
    return annotation;
  }
}
