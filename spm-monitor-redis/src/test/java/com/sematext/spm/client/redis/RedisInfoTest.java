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
package com.sematext.spm.client.redis;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;

public class RedisInfoTest {
  @Test
  public void testParse() {
    final String testRedisInfoOutput = "# Stats\n" +
        "total_connections_received:203\n" +
        "total_commands_processed:3612\n" +
        "instantaneous_ops_per_sec:0\n" +
        "rejected_connections:0\n" +
        "expired_keys:0\n" +
        "evicted_keys:0\n" +
        "keyspace_hits:0\n" +
        "keyspace_misses:0\n" +
        "pubsub_channels:0\n" +
        "pubsub_patterns:0\n" +
        "latest_fork_usec:0\n" +
        "\n" +
        "# Replication\n" +
        "role:master\n" +
        "connected_slaves:0\n" +
        "\n" +
        "# CPU\n" +
        "used_cpu_sys:1.50\n" +
        "used_cpu_user:3.32\n" +
        "used_cpu_sys_children:0.00\n" +
        "used_cpu_user_children:0.00\n" +
        "\n" +
        "# Keyspace\n" +
        "db0:keys=6998,expires=0\n" +
        "db3:keys=1,expires=0\n";

    RedisInfo parsed = RedisInfo.parse(testRedisInfoOutput);

    assertTrue(parsed.getDatabases().contains("db0"));
    assertTrue(parsed.getDatabases().contains("db3"));

    assertEquals(2, parsed.getDatabases().size());

    assertEquals("6998", parsed.get("db0", "keys"));
    assertEquals("0", parsed.get("db0", "expires"));

    assertEquals("1", parsed.get("db3", "keys"));
    assertEquals("0", parsed.get("db3", "expires"));

    assertNull(parsed.get("unexisting"));
    assertNull(parsed.get("db4", "unexisting"));
    assertNull(parsed.get("db3", "unexisting"));
  }

}
