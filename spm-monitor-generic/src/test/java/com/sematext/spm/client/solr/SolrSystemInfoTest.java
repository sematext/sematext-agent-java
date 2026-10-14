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

public class SolrSystemInfoTest {
  private static Map<String, Object> systemInfo(String mode, String version) {
    Map<String, Object> lucene = new HashMap<String, Object>();
    lucene.put("solr-spec-version", version);
    lucene.put("lucene-spec-version", "10.3.2");
    Map<String, Object> info = new HashMap<String, Object>();
    info.put("mode", mode);
    info.put("lucene", lucene);
    return info;
  }

  @Test
  public void testReadsVersionAndMode() {
    Map<String, Object> info = systemInfo("solrcloud", "10.0.0");

    Assert.assertEquals("10.0.0", SolrSystemInfo.version(info));
    Assert.assertEquals("solrcloud", SolrSystemInfo.mode(info));
  }

  @Test
  public void testMissingFieldsGiveNull() {
    Assert.assertNull(SolrSystemInfo.version(null));
    Assert.assertNull(SolrSystemInfo.mode(null));
    Assert.assertNull(SolrSystemInfo.version(new HashMap<String, Object>()));
  }
}
