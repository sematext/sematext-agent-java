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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SqlStatementParserTest {
  @Test
  public void testParseSelect() throws Exception {
    assertEquals(SqlStatementParser
                     .parse("select * from User"), new SqlStatement(SqlStatement.Operation.SELECT, "user"));
    assertEquals(SqlStatementParser
                     .parse("select u1.name, u2.name from User u where id = 2"), new SqlStatement(SqlStatement.Operation.SELECT, "user"));
    assertEquals(SqlStatementParser
                     .parse("select * from User u where u.id in (select userId from System where name = 'system-2')"),
                 new SqlStatement(SqlStatement.Operation.SELECT, null));
    assertEquals(SqlStatementParser
                     .parse("select u3.name, u3.name from User u, System s where u.id = s.userId where s.id < 100"),
                 new SqlStatement(SqlStatement.Operation.SELECT, null));
    assertEquals(SqlStatementParser.parse("select u.name as userName, s.name as systemName from User u join System s"),
                 new SqlStatement(SqlStatement.Operation.SELECT, null));
  }

  @Test
  public void testParseDelete() throws Exception {
    assertEquals(SqlStatementParser
                     .parse("delete from User where id = 10"), new SqlStatement(SqlStatement.Operation.DELETE, "user"));
    assertEquals(SqlStatementParser
                     .parse("delete from `User` where id = 10"), new SqlStatement(SqlStatement.Operation.DELETE, "user"));
    assertEquals(SqlStatementParser
                     .parse("delete from `U``ser` where id = 10"), new SqlStatement(SqlStatement.Operation.DELETE, "u`ser"));
    assertEquals(SqlStatementParser
                     .parse("delete from `some``database```.`U``ser` where id = 10"), new SqlStatement(SqlStatement.Operation.DELETE, "u`ser"));
    assertEquals(SqlStatementParser
                     .parse("delete from User where id in (select userId from System where name = 'system-1')"), new SqlStatement(SqlStatement.Operation.DELETE, "user"));
    assertEquals(SqlStatementParser
                     .parse("delete from db1.User where id in (select userId from System where name = 'system-1')"), new SqlStatement(SqlStatement.Operation.DELETE, "user"));
  }

  @Test
  public void testParseInsert() throws Exception {
    assertEquals(SqlStatementParser
                     .parse("insert into User(name, age) values('user-1', 42)"), new SqlStatement(SqlStatement.Operation.INSERT, "user"));
    assertEquals(SqlStatementParser
                     .parse("insert into db1.User(name, age) values('user-1', 42)"), new SqlStatement(SqlStatement.Operation.INSERT, "user"));
    assertEquals(SqlStatementParser
                     .parse("insert into db1.User values('user-1', 42)"), new SqlStatement(SqlStatement.Operation.INSERT, "user"));
    assertEquals(SqlStatementParser
                     .parse("insert into User values('user-1', 42)"), new SqlStatement(SqlStatement.Operation.INSERT, "user"));
  }

  @Test
  public void testParseUpdate() throws Exception {
    assertEquals(SqlStatementParser
                     .parse("update User set age = 42 where name = 'user-1"), new SqlStatement(SqlStatement.Operation.UPDATE, "user"));
    assertEquals(SqlStatementParser
                     .parse("update db1.User set age = 42 where name = 'user-1"), new SqlStatement(SqlStatement.Operation.UPDATE, "user"));
  }

  @Test
  public void testParseOther() throws Exception {
    assertEquals(SqlStatementParser
                     .parse("alter table User add column city varchar(1024)"), new SqlStatement(SqlStatement.Operation.OTHER, null));
  }

  @Test
  public void testParseComments() throws Exception {
    String sql1 = "/* remove me */ update User set /* and me */ age = 42 where name = 'user-1";
    assertEquals(SqlStatementParser.parse(sql1), new SqlStatement(SqlStatement.Operation.UPDATE, "user"));

    String sql2 = "/* multiline \n comment with lot of tabs \t\t\t \n*/ delete from User";
    assertEquals(SqlStatementParser.parse(sql2), new SqlStatement(SqlStatement.Operation.DELETE, "user"));

    String sql3 = "-- One line comment\t with tabs\ndelete from User\n-- All things are removed";
    assertEquals(SqlStatementParser.parse(sql3), new SqlStatement(SqlStatement.Operation.DELETE, "user"));

    String sql4 = "--One line comment\t with tabs\ndelete from User\n-- All things are removed";
    assertEquals(SqlStatementParser.parse(sql4), new SqlStatement(SqlStatement.Operation.DELETE, "user"));
  }

  @Test
  public void testParseMultiline() throws Exception {
    String sql1 = "delete \nfrom User \n\twhere id in (select userId from System where name = 'system-1')";
    assertEquals(SqlStatementParser.parse(sql1), new SqlStatement(SqlStatement.Operation.DELETE, "user"));
  }
}
