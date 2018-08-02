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

import static com.sematext.spm.client.tracing.agent.sql.SqlQueryMetadata.extract;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;

import com.sematext.spm.client.tracing.agent.sql.SqlQueryMetadata.Metadata;
import com.sematext.spm.client.tracing.agent.sql.SqlQueryMetadata.Operation;

@Ignore("not implemented")
public class SqlQueryMetadataTest {
  private static void assertMetadataEquals(Operation operation, String table, Metadata metadata) {
    assertNotNull(metadata);
    assertEquals(operation, metadata.getOperation());
    assertEquals(table, metadata.getTable());
  }

  @Test
  public void testExtractOpAndTable() {
    assertMetadataEquals(Operation.INSERT, "users", extract("insert into Users(id, name) values(1, 'pavel');"));
    assertMetadataEquals(Operation.DELETE, "users", extract("delete from Users where id = 1 and name = 'pavel'"));
    assertMetadataEquals(Operation.SELECT, "users", extract("select * from Users u"));
  }

  @Test
  public void testExtractOpWithoutTable() {
    assertMetadataEquals(Operation.SELECT, null, extract("select * from Users u join Account a on a.id = u.accountId"));
  }
}
