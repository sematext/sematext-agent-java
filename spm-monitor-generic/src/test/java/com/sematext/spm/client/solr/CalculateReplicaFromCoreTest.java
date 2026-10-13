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
package com.sematext.spm.client.solr;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CalculateReplicaFromCoreTest {
  private CalculateReplicaFromCore c = new CalculateReplicaFromCore();

  private static Map<String, String> tags(String collection, String shard, String core) {
    Map<String, String> tags = new HashMap<String, String>();
    tags.put("collection", collection);
    tags.put("shard", shard);
    tags.put("core", core);
    return tags;
  }

  @Test
  public void testStripsCollectionAndShard() {
    Assert.assertEquals("replica_n2",
                        c.calculateTag(tags("col1", "shard1", "col1_shard1_replica_n2"), "collection", "shard", "core"));
  }

  @Test
  public void testCollectionWithUnderscores() {
    Assert.assertEquals("replica_t5",
                        c.calculateTag(tags("my_col", "shard10", "my_col_shard10_replica_t5"), "collection", "shard",
                                       "core"));
  }

  @Test
  public void testUnexpectedCoreNameIsKept() {
    Assert.assertEquals("renamed_core",
                        c.calculateTag(tags("col1", "shard1", "renamed_core"), "collection", "shard", "core"));
  }
}
