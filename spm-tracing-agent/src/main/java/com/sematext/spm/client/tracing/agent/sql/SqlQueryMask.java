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

/**
 * Simple SQL Query Parameters 'Masker'.
 */
public final class SqlQueryMask {

  private static final char SQL_SINGLE_QUOTE = '\'';
  private static final char SQL_DOUBLE_QUOTE = '\"';
  private static final char SQL_BACKSLASH = '\\';
  private static final char MASK_CHAR = '?';
  private static final char UNDERSCORE = '_';

  private SqlQueryMask() {
  }

  public static String mask(String sql) {
    final StringBuilder masked = new StringBuilder();
    char[] query = sql.toCharArray();
    int i = 0;
    while (i < query.length) {
      char c = query[i];
      if (Character.isAlphabetic(c) || c == UNDERSCORE) { //identifier/keyword: [A-Z]+[A-Z0-9\_]*
        do {
          masked.append(query[i++]);
        } while (i < query.length && (Character.isDigit(query[i]) || Character.isAlphabetic(query[i])
            || query[i] == '_'));
      } else if (Character.isDigit(c)) {
        do {
          i++;
        } while (i < query.length && Character.isDigit(query[i]));
        masked.append(MASK_CHAR);
      } else if (c == SQL_SINGLE_QUOTE || c == SQL_DOUBLE_QUOTE) {
        char quote = c;
        do {
          i++;
          if (i < query.length - 1 && query[i] == quote && query[i + 1] == quote) { //escape ''
            i += 2;
          } else if (i < query.length - 1 && query[i] == SQL_BACKSLASH && query[i + 1] == quote) { //escape \'
            i += 2;
          }
        } while (i < query.length && query[i] != quote);
        masked.append("?");
        i++;
      } else {
        masked.append(c);
        i++;
      }
    }
    return masked.toString();
  }
}
