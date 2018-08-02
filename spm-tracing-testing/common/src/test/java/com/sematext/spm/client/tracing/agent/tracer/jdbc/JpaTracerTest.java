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
package com.sematext.spm.client.tracing.agent.tracer.jdbc;

import static com.sematext.spm.client.tracing.utils.CallDebug.printCallTree;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import com.sematext.spm.client.tracing.Trace;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.tracer.Tracers;
import com.sematext.spm.client.tracing.utils.MockTransactionSink;
import com.sematext.spm.client.tracing.utils.TracingContext;
import com.sematext.spm.client.tracing.utils.TracingJUnit4ClassRunner;
import com.sematext.spm.client.util.IOUtils;

@RunWith(TracingJUnit4ClassRunner.class)
@TracingContext(tracers = { Tracers.TracedMethodsTracer.class, Tracers.JDBCTracer.class, Tracers.JpaTracer.class })
public class JpaTracerTest {

  private void populateDB(String... scripts) throws Exception {
    Class.forName("org.hsqldb.jdbcDriver");
    final Connection connection = DriverManager.getConnection("jdbc:hsqldb:mem:users");

    for (String script : scripts) {
      executeScript(connection, script);
    }
  }

  private List<String> extractStatements(List<String> lines) {
    final List<String> statements = new ArrayList<String>();
    String statement = "";
    for (String line : lines) {
      statement += line;
      if (line.trim().endsWith(";")) {
        statements.add(statement);
        statement = "";
      }
    }
    return statements;
  }

  private void executeScript(Connection c, String classPath) throws Exception {
    final Statement script = c.createStatement();
    final List<String> lines = (List<String>) IOUtils.readLines(getClass().getResourceAsStream(classPath));
    for (final String sql : extractStatements(lines)) {
      if (sql.trim().isEmpty()) continue;
      try {
        script.addBatch(sql);
      } catch (Exception e) {
        System.err.println("Invalid statement: [" + sql + "]");
        throw e;
      }
    }
    try {
      script.executeBatch();
    } catch (Exception e) {
    }
  }

  public static class JpaService {
    final EntityManager em;

    public JpaService() {
      Thread.currentThread().setContextClassLoader(JpaService.class.getClassLoader());

      this.em = Persistence.createEntityManagerFactory("users").createEntityManager();
    }

    @Trace(force = true)
    public int queryUsers() {
      TypedQuery<User> query = em.createQuery("select u from User u", User.class);
      int i = 0;
      for (User user : query.getResultList()) {
        i++;
      }

      return i;
    }

    @Trace(force = true)
    @SuppressWarnings("unchecked")
    public void findUser() {
      em.find(User.class, 1L);
      em.find(User.class, 1L, LockModeType.NONE);
      em.find(User.class, 1L, Collections.EMPTY_MAP);
      em.find(User.class, 1L, LockModeType.NONE, Collections.EMPTY_MAP);
    }

    @Trace
    @SuppressWarnings("unchecked")
    public void insertVets() {
      Vet vet1 = new Vet();
      vet1.setFirstName("Vet");
      vet1.setLastName("Vet");

      em.persist(vet1);

      vet1.setFirstName("NotVet");

      em.merge(vet1);

      em.remove(vet1);
    }

    @Trace
    public List<Owner> findOwners() {
      Query query = this.em
          .createQuery("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets WHERE owner.lastName LIKE :lastName");
      query.setParameter("lastName", "%");
      return query.getResultList();
    }
  }

  @Before
  public void before() throws Exception {
    populateDB("/users.sql");
    populateDB("/initDB.sql", "/populateDB.sql");
  }

  @Test
  public void testQueryUsers() throws Exception {
    final MockTransactionSink mockCallSink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(mockCallSink);

    JpaService jpaService = new JpaService();

    assertEquals(4, jpaService.queryUsers());

    System.out.println(printCallTree(mockCallSink.getTransactions()));

    mockCallSink.clean();

    assertEquals(4, jpaService.queryUsers());

    System.out.println(printCallTree(mockCallSink.getTransactions()));
  }

  @Test
  public void testFindUsers() throws Exception {
    final MockTransactionSink sink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(sink);

    JpaService loader = new JpaService();

    loader.findUser();

    System.out.println(printCallTree(sink.getTransactions()));
  }

  @Test
  public void testInsertVets() throws Exception {
    final MockTransactionSink sink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(sink);

    JpaService service = new JpaService();

    service.insertVets();

    System.out.println(printCallTree(sink.getTransactions()));
  }

  @Test
  public void testListOwners() throws Exception {

    final MockTransactionSink mockCallSink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(mockCallSink);

    JpaService jpaService = new JpaService();

    for (int i = 0; i < 1; i++) {
      jpaService.findOwners();
    }

    System.out.println(printCallTree(mockCallSink.getTransactions()));

  }

}
