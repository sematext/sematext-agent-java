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

public class CalculateSegmentsCountTest {
  private CalculateSegmentsCount c = new CalculateSegmentsCount();

  @Test
  public void testSolr3() {
    Map<String, Object> metrics = new HashMap<String, Object>();
    metrics.put("Value", "something something segments=23");
    
    Assert.assertEquals(23, c.calculateAttribute(metrics, "Value").intValue());
  }

  @Test
  public void testSolr4Plus() {
    Map<String, Object> metrics = new HashMap<String, Object>();
    metrics.put("Value",
        "StandardDirectoryReader(segments_6:11 _0(4.0):C2 _1(4.0):C11 _2(4.0):C2 _3(4.0):C4 _4(4.0):C5)");
    Assert.assertEquals(5, c.calculateAttribute(metrics, "Value").intValue());
    
    metrics.put("Value", "StandardDirectoryReader(segments_4:9 _1(4.0.0.2):C1 _2(4.0.0.2):C1)");
    Assert.assertEquals(2, c.calculateAttribute(metrics, "Value").intValue());
    
    // solr 7
    metrics.put("Value", "ExitableDirectoryReader(UninvertingDirectoryReader(Uninverting(_7(7.1.0):C23/14:delGen=1) " +
        "Uninverting(_8(7.1.0):C14)))");
    Assert.assertEquals(2, c.calculateAttribute(metrics, "Value").intValue());
  }
  
  @Test
  public void testSolr85() {
    Map<String, Object> metrics = new HashMap<String, Object>();
    metrics.put("Value",
        "ExitableDirectoryReader(UninvertingDirectoryReader(Uninverting(_0(8.5.2):C3:[diagnostics={java.vendor=Oracle" +
        " Corporation, os=Linux, java.version=1.8.0_151, java.vm.version=25.151-b12, lucene.version=8.5.2, " +
        "os.arch=amd64, java.runtime.version=1.8.0_151-b12, source=flush, os.version=4.4.0-31-generic, " +
        "timestamp=1603261473913}]:[attributes={Lucene50StoredFieldsFormat.mode=BEST_SPEED}]) " +
        "Uninverting(_2(8.5.2):C4:[diagnostics={java.vendor=Oracle Corporation, os=Linux, java.version=1.8.0_151, " +
        "java.vm.version=25.151-b12, lucene.version=8.5.2, os.arch=amd64, java.runtime.version=1.8.0_151-b12, " +
        "source=flush, os.version=4.4.0-31-generic, " +
        "timestamp=1603261550321}]:[attributes={Lucene50StoredFieldsFormat.mode=BEST_SPEED}])))");
    
    Assert.assertEquals(2, c.calculateAttribute(metrics, "Value").intValue());
  }
}
