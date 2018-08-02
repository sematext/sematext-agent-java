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
package com.sematext.spm.client.unlogger;

import static com.sematext.spm.client.util.StringOuterpolator.outerpolator;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import com.sematext.spm.client.util.StringOuterpolator;

public class StringOuterpolatorTest {
  @Test
  public void simpleOuterpolateTest() {
    StringOuterpolator stringOuterpolator = outerpolator("some${ix} some${iy}", "${", "}");
    Map<String, String> desiredVals = new HashMap<String, String>();
    desiredVals.put("ix", "IX");
    desiredVals.put("iy", "IY");
    Assert.assertEquals(desiredVals, stringOuterpolator.outerpolate("someIX someIY"));

  }

  @Test
  public void moreMetacharsOuterpolateTest() {
    StringOuterpolator stringOuterpolator = outerpolator(".....${ix}.${iy}", "${", "}");
    Map<String, String> desiredVals = new HashMap<String, String>();
    desiredVals.put("ix", "IX");
    desiredVals.put("iy", "IY");
    Assert.assertEquals(desiredVals, stringOuterpolator.outerpolate(".....IX.IY"));
  }

  @Test
  public void jmxNameWithMetacharsOuterpolateTest() {
    StringOuterpolator stringOuterpolator = outerpolator("com.senseidb:zoie-name=zoie-admin-${node}-${shard}", "${", "}");
    Map<String, String> desiredVals = new HashMap<String, String>();
    desiredVals.put("node", "1");
    desiredVals.put("shard", "15");
    Assert.assertEquals(desiredVals, stringOuterpolator.outerpolate("com.senseidb:zoie-name=zoie-admin-1-15"));

  }

}
