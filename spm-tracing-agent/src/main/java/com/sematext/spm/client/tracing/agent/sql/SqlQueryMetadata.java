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

public final class SqlQueryMetadata {
  private SqlQueryMetadata() {
  }

  public static enum Operation {
    SELECT, INSERT, DELETE, UNKNOWN;

    public static Operation fromString(String op) {
      for (Operation value : values()) {
        if (value.name().equalsIgnoreCase(op)) {
          return value;
        }
      }

      return UNKNOWN;
    }
  }

  public static final class Metadata {
    private final String table;
    private final Operation operation;

    public Metadata(Operation operation, String table) {
      this.operation = operation;
      this.table = table;
    }

    public String getTable() {
      return table;
    }

    public Operation getOperation() {
      return operation;
    }
  }

  public static Metadata extract(String query) {
    if (query == null) {
      throw new NullPointerException();
    }
    final String expr = query.trim();
    int i = expr.indexOf(" ");
    if (i >= 0) {
      Operation op = Operation.fromString(expr.substring(0, i));
      if (Operation.DELETE == op) {
        return parseDelete(expr.substring(i).trim());
      } else if (Operation.INSERT == op) {
        return parseInsert(expr.substring(i).trim());
      } else if (Operation.SELECT == op) {
        return parseSelect(expr.substring(i).trim());
      }
    }

    return new Metadata(Operation.UNKNOWN, null);
  }

  private static Metadata parseSelect(String expr) {
    return null;
  }

  private static Metadata parseInsert(String expr) {
    String[] tokens = expr.split("[ \t\\(\\),]");
    if (tokens.length < 2) {
      return null;
    }
    if (!tokens[0].equalsIgnoreCase("into")) {
      return null;
    }
    return new Metadata(Operation.INSERT, tokens[1].toLowerCase());
  }

  private static Metadata parseDelete(String expr) {
    String[] tokens = expr.split("[ \t\\(\\),]");
    if (tokens.length < 2) {
      return null;
    }
    if (!tokens[0].equalsIgnoreCase("from")) {
      return null;
    }
    return new Metadata(Operation.DELETE, tokens[1].toLowerCase());
  }
}
