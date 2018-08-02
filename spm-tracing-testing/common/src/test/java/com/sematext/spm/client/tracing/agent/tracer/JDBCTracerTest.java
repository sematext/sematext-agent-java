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
package com.sematext.spm.client.tracing.agent.tracer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.annotation.SQLAnnotation;
import com.sematext.spm.client.tracing.agent.sql.SqlStatement;
import com.sematext.spm.client.tracing.utils.MockTransactionSink;
import com.sematext.spm.client.tracing.utils.TracingContext;
import com.sematext.spm.client.tracing.utils.TracingJUnit4ClassRunner;
import com.sematext.spm.client.tracing.utils.TracingTesting;
import com.sematext.spm.client.util.IOUtils;

@RunWith(TracingJUnit4ClassRunner.class)
@TracingContext(tracers = Tracers.JDBCTracer.class)
public class JDBCTracerTest {

  public void testStatement(final Connection connection) throws Exception {
    final Statement statement = connection.createStatement();
    final ResultSet rs = statement.executeQuery("select * from users");

    while (rs.next()) {
    }

    statement.execute("select * from users");
  }

  public void testPreparedStatement(final Connection connection) throws Exception {
    final PreparedStatement statement = connection.prepareStatement("select * from users where id = ?");
    statement.setInt(1, 1);
    statement.executeQuery().next();

    statement.setInt(1, 2);
    statement.executeQuery().next();

    statement.setInt(1, 3);
    statement.executeQuery().next();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void test() throws Exception {
    Tracing.newTrace("jdbc-trace", Call.TransactionType.BACKGROUND);

    Class.forName("org.h2.Driver");

    final Connection connection = DriverManager.getConnection("jdbc:h2:mem:test");
    for (final String line : (List<String>) IOUtils.readLines(getClass().getResourceAsStream("/users.sql"))) {
      if (line.trim().isEmpty()) {
        continue;
      }
      connection.createStatement().executeUpdate(line);
    }

    final MockTransactionSink mockTransactionSink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(mockTransactionSink);

    testStatement(connection);
    assertEquals(2, mockTransactionSink.getTransactions().size());

    List<Call> calls = TracingTesting.findMatching(".*execute\\(\\)", mockTransactionSink.getTransactions());
    assertEquals(1, calls.size());

    Call jdbcExecuteCall = calls.get(0);
    assertNotNull(jdbcExecuteCall.getAnnotation());
    SQLAnnotation sqlAnnotation = (SQLAnnotation) jdbcExecuteCall.getAnnotation();
    assertEquals(SqlStatement.Operation.SELECT, sqlAnnotation.getOperation());
    assertEquals("users", sqlAnnotation.getTable());

    mockTransactionSink.clean();

    testPreparedStatement(connection);
    assertEquals(3, mockTransactionSink.getTransactions().size());

    calls = TracingTesting.findMatching(".*executeQuery\\(\\)", mockTransactionSink.getTransactions());

    jdbcExecuteCall = calls.get(0);
    assertNotNull(jdbcExecuteCall.getAnnotation());
    sqlAnnotation = (SQLAnnotation) jdbcExecuteCall.getAnnotation();
    assertEquals(SqlStatement.Operation.SELECT, sqlAnnotation.getOperation());
    assertEquals("users", sqlAnnotation.getTable());
  }

  @Test
  public void testIgnoreProxyClasses() throws Exception {
    Tracing.newTrace("jdbc-proxy-trace", Call.TransactionType.BACKGROUND);

    final ComboPooledDataSource cpds = new ComboPooledDataSource();
    cpds.setDriverClass("org.h2.Driver");
    cpds.setJdbcUrl("jdbc:h2:mem:test-proxy");

    final Connection connection = cpds.getConnection();
    for (final String line : (List<String>) IOUtils.readLines(getClass().getResourceAsStream("/users.sql"))) {
      if (line.trim().isEmpty()) {
        continue;
      }
      connection.createStatement().executeUpdate(line);
    }

    final MockTransactionSink mockCallSink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(mockCallSink);

    testPreparedStatement(connection);

    assertEquals(3, mockCallSink.clean());
  }
}
