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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sematext.spm.client.util.Preconditions;

public final class SqlStatementParser {

  private static final Pattern TABLE_NAME_RE = Pattern
      .compile("(?:(?:(?:\"(?:(?:\"\")|[^\"])*\")|(?:`(?:(?:``)|[^`])*`)|(?:[a-zA-Z][a-zA-Z0-9]*))\\.)?((?:\"(?:(?:\"\")|[^\"])*\")|(?:`(?:(?:``)|[^`])*`)|(?:[a-zA-Z][a-zA-Z0-9]*)).*", Pattern.DOTALL);

  private static final Pattern ID_BRACKETS = Pattern.compile("^[\"`](.*)[`\"]$");

  private static final Pattern ID_BRACKETS_ESCAPE = Pattern.compile("(`){2}|(\"){2}");

  private static final Pattern MULTILINE_COMMENT = Pattern
      .compile("\\s*/\\*.*?\\*/\\s*", Pattern.MULTILINE | Pattern.DOTALL);

  private static final Pattern ONE_LINE_COMMENT = Pattern.compile("--[^\r\n]*");

  private static interface StatementParser {
    SqlStatement.Operation getOperation();

    Pattern operationPattern();

    String getTableName(String tail);
  }

  private static abstract class BaseStatementParser implements StatementParser {
    private final Pattern operationPattern;
    private final SqlStatement.Operation operation;

    BaseStatementParser(String operationRE, SqlStatement.Operation operation) {
      this.operationPattern = Pattern
          .compile(operationRE, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
      this.operation = operation;
    }

    @Override
    public SqlStatement.Operation getOperation() {
      return operation;
    }

    @Override
    public Pattern operationPattern() {
      return operationPattern;
    }
  }

  private static class SelectStatementParser extends BaseStatementParser {

    private static final Pattern INNER_SELECT = Pattern
        .compile(".*\\(\\s*select\\s+.*?from.*", Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private static final Pattern JOIN = Pattern
        .compile("\\s*((?:\"(?:(?:\"\")|[^\"])*\")|(?:`(?:(?:``)|[^`])*`)|(?:[a-zA-Z][a-zA-Z0-9]*)).*?(?:,|(?:join)).*",
                 Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public SelectStatementParser() {
      super("^\\s*(select)\\s+.*?from\\s+(.*)", SqlStatement.Operation.SELECT);
    }

    @Override
    public String getTableName(String tail) {
      Matcher innerSelectMatcher = INNER_SELECT.matcher(tail);
      if (innerSelectMatcher.matches()) { // don't extract table name in case when inner select is used
        return null;
      }
      Matcher tableNameMatcher = TABLE_NAME_RE.matcher(tail);
      if (tableNameMatcher.matches() && tableNameMatcher.groupCount() == 1) {
        String fromExpr = tail.substring(tableNameMatcher.end(1));
        if (JOIN.matcher(fromExpr).matches()) { // don't extract table name in case when join is used
          return null;
        }
        return tableNameMatcher.group(1);
      }
      return null;
    }
  }

  private static class DeleteStatementParser extends BaseStatementParser {
    public DeleteStatementParser() {
      super("^\\s*(delete)\\s+from\\s+(.*)", SqlStatement.Operation.DELETE);
    }

    @Override
    public String getTableName(String tail) {
      Matcher matcher = TABLE_NAME_RE.matcher(tail);
      if (matcher.matches() && matcher.groupCount() == 1) {
        return matcher.group(1);
      }
      return null;
    }
  }

  private static class InsertStatementParser extends BaseStatementParser {
    public InsertStatementParser() {
      super("^\\s*(insert)\\s+into\\s+(.*)", SqlStatement.Operation.INSERT);
    }

    @Override
    public String getTableName(String tail) {
      Matcher matcher = TABLE_NAME_RE.matcher(tail);
      if (matcher.matches() && matcher.groupCount() == 1) {
        return matcher.group(1);
      }
      return null;
    }
  }

  private static class UpdateStatementParser extends BaseStatementParser {
    public UpdateStatementParser() {
      super("^\\s*(update)\\s+(.*)", SqlStatement.Operation.UPDATE);
    }

    @Override
    public String getTableName(String tail) {
      Matcher matcher = TABLE_NAME_RE.matcher(tail);
      if (matcher.matches() && matcher.groupCount() == 1) {
        return matcher.group(1);
      }
      return null;
    }
  }

  private static enum Parser {
    SELECT(new SelectStatementParser()),
    DELETE(new DeleteStatementParser()),
    INSERT(new InsertStatementParser()),
    UPDATE(new UpdateStatementParser()),
    OTHER(new StatementParser() {
      @Override
      public SqlStatement.Operation getOperation() {
        return SqlStatement.Operation.OTHER;
      }

      @Override
      public Pattern operationPattern() {
        return null;
      }

      @Override
      public String getTableName(String tail) {
        return null;
      }
    });

    final StatementParser parser;

    Parser(StatementParser parser) {
      this.parser = parser;
    }
  }

  private static String normalizeTableName(String tableName) {
    Preconditions.checkNotNull(tableName, "tableName should be defined");

    tableName = ID_BRACKETS.matcher(tableName).replaceAll("$1");
    tableName = ID_BRACKETS_ESCAPE.matcher(tableName).replaceAll("$1");
    return tableName.toLowerCase();
  }

  public static SqlStatement parse(String sql) {
    Preconditions.checkNotNull(sql);

    sql = MULTILINE_COMMENT.matcher(sql).replaceAll("");
    sql = ONE_LINE_COMMENT.matcher(sql).replaceAll("");

    for (Parser p : Parser.values()) {
      final StatementParser stmtParser = p.parser;

      if (stmtParser.operationPattern() != null) {
        final Matcher matcher = stmtParser.operationPattern().matcher(sql);
        if (matcher.matches()) {
          if (matcher.groupCount() == 2) {
            String tableName = stmtParser.getTableName(matcher.group(2));
            if (tableName != null) {
              tableName = normalizeTableName(tableName);
            }
            return new SqlStatement(stmtParser.getOperation(), tableName);
          }
        }
      } else {
        break;
      }
    }
    return new SqlStatement(SqlStatement.Operation.OTHER, null);
  }
}
