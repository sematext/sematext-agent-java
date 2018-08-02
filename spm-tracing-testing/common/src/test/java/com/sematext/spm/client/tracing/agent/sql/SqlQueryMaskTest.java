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

import static com.sematext.spm.client.tracing.agent.sql.SqlQueryMask.mask;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SqlQueryMaskTest {

  @Test
  public void testMask() throws Exception {
    assertEquals(" ? ", mask(" 1234 "));
    assertEquals("select * from user where login = ? and password=?", mask("select * from user where login = 'user-12' and password='qwerty'"));
    assertEquals("select * from user where name = ?", mask("select * from user where name = ''"));
    assertEquals("select * from user where country = ? and age between(?, ?)", mask("select * from user where country = 'USA' and age between(30, 50)"));
    assertEquals("select * from post where title = ?", mask("select * from post where title = 'Kafka, ''The Process'''"));
    assertEquals("select * from post where title = ?", mask("select * from post where title = 'Kafka, \\'The Process\\''"));

    assertEquals("select * from post where title = ?", mask("select * from post where title = \"Kafka, \"\"The Process\"\"\""));

    assertEquals("select owner1 from pets where pet_name = ?", mask("select owner1 from pets where pet_name = 10"));

    System.out.println(mask("select visits0_.pet_id as pet_id4_1_0_, visits0_.id as id1_6_0_, visits0_.id as" +
                                " id1_6_1_, visits0_.visit_date as visit_da2_6_1_, visits0_.description as descript3_6_1_, \n"
                                +
                                "visits0_.pet_id as pet_id4_6_1_ from visits visits0_ where visits0_.pet_id=?"));
  }
}
