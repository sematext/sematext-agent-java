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
package com.sematext.spm.client.tracing.agent.sql;

public final class SqlStatement {

  public static enum Operation {
    SELECT, DELETE, UPDATE, INSERT, OTHER
  }

  private final Operation operation;
  private final String table;

  public SqlStatement(Operation operation, String table) {
    this.operation = operation;
    this.table = table;
  }

  public Operation getOperation() {
    return operation;
  }

  public String getTable() {
    return table;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SqlStatement sqlQuery = (SqlStatement) o;

    if (operation != sqlQuery.operation) return false;
    if (table != null ? !table.equals(sqlQuery.table) : sqlQuery.table != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = operation != null ? operation.hashCode() : 0;
    result = 31 * result + (table != null ? table.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "SqlStatement{" +
        "operation=" + operation +
        ", table='" + table + '\'' +
        '}';
  }
}
