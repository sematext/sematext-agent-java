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
package com.sematext.spm.client;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SerializerTest {
  @Test
  public void testInfluxFormat() {
    Map<String, Object> metrics = new HashMap<String, Object>();
    metrics.put("metric1", 123);
    Map<String, String> tags = new HashMap<String, String>();
    tags.put("host", "abc");
    tags.put("network", "def");
    Assert.assertEquals("solr,token=aaa,host=abc,network=def metric1=123i 100000000", Serializer.INFLUX
        .serialize("solr", "aaa", metrics, tags, 100));
  }
}
