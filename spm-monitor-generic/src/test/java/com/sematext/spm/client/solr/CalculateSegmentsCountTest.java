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
  
  @Test
  public void testSolr87() {
    Map<String, Object> metrics = new HashMap<String, Object>();
    metrics.put("Value", "ExitableDirectoryReader(&#8203;UninvertingDirectoryReader(&#8203;Uninverting(&#8203;_0(&#8203;8.7.0):C18:[diagnostics={java.vendor=Oracle Corporation, os=Linux, java.version=1.8.0_151, java.vm.version=25.151-b12, lucene.version=8.7.0, os.arch=amd64, java.runtime.version=1.8.0_151-b12, source=flush, os.version=4.4.0-31-generic, timestamp=1615308345608}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=ay6mz9t5crdj71gwkxi2nbkd4) Uninverting(&#8203;_1(&#8203;8.7.0):C5:[diagnostics={java.vendor=Oracle Corporation, os=Linux, java.version=1.8.0_151, java.vm.version=25.151-b12, lucene.version=8.7.0, os.arch=amd64, java.runtime.version=1.8.0_151-b12, source=flush, os.version=4.4.0-31-generic, timestamp=1615308355555}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=ay6mz9t5crdj71gwkxi2nbkdh) Uninverting(&#8203;_2(&#8203;8.7.0):C3:[diagnostics={java.vendor=Oracle Corporation, os=Linux, java.version=1.8.0_151, java.vm.version=25.151-b12, lucene.version=8.7.0, os.arch=amd64, java.runtime.version=1.8.0_151-b12, source=flush, os.version=4.4.0-31-generic, timestamp=1615308358485}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=ay6mz9t5crdj71gwkxi2nbke2)))");
    Assert.assertEquals(3, c.calculateAttribute(metrics, "Value").intValue());
    
    metrics.put("Value", "ExitableDirectoryReader(&#8203;UninvertingDirectoryReader(&#8203;Uninverting(&#8203;_vd(&#8203;8.7.0):C7063701:[diagnostics={os=Linux, java.version=11.0.9.1, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=merge, os.version=4.18.0-240.10.1.el8_3.x86_64, java.vendor=Red Hat, Inc., java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, mergeMaxNumSegments=-1, mergeFactor=10, timestamp=1613821651252}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8tf) Uninverting(&#8203;_tj(&#8203;8.7.0):c37954:[diagnostics={os=Linux, java.version=11.0.9.1, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=merge, os.version=4.18.0-240.10.1.el8_3.x86_64, java.vendor=Red Hat, Inc., java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, mergeMaxNumSegments=-1, mergeFactor=10, timestamp=1613821613671}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo7rq) Uninverting(&#8203;_14l(&#8203;8.7.0):c762152:[diagnostics={os=Linux, java.version=11.0.9.1, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=merge, os.version=4.18.0-240.10.1.el8_3.x86_64, java.vendor=Red Hat, Inc., java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, mergeMaxNumSegments=-1, mergeFactor=10, timestamp=1613821841948}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8td) Uninverting(&#8203;_124(&#8203;8.7.0):c719468:[diagnostics={os=Linux, java.version=11.0.9.1, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=merge, os.version=4.18.0-240.10.1.el8_3.x86_64, java.vendor=Red Hat, Inc., java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, mergeMaxNumSegments=-1, mergeFactor=10, timestamp=1613821788583}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8mn) Uninverting(&#8203;_z2(&#8203;8.7.0):c848557:[diagnostics={os=Linux, java.version=11.0.9.1, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=merge, os.version=4.18.0-240.10.1.el8_3.x86_64, java.vendor=Red Hat, Inc., java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, mergeMaxNumSegments=-1, mergeFactor=10, timestamp=1613821727336}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8et) Uninverting(&#8203;_xz(&#8203;8.7.0):c64686:[diagnostics={os=Linux, java.version=11.0.9.1, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=merge, os.version=4.18.0-240.10.1.el8_3.x86_64, java.vendor=Red Hat, Inc., java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, mergeMaxNumSegments=-1, mergeFactor=10, timestamp=1613821703709}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo852) Uninverting(&#8203;_zc(&#8203;8.7.0):c46375:[diagnostics={os=Linux, java.version=11.0.9.1, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=merge, os.version=4.18.0-240.10.1.el8_3.x86_64, java.vendor=Red Hat, Inc., java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, mergeMaxNumSegments=-1, mergeFactor=10, timestamp=1613821731784}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo894) Uninverting(&#8203;_zq(&#8203;8.7.0):c57034:[diagnostics={os=Linux, java.version=11.0.9.1, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=merge, os.version=4.18.0-240.10.1.el8_3.x86_64, java.vendor=Red Hat, Inc., java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, mergeMaxNumSegments=-1, mergeFactor=10, timestamp=1613821738916}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8a7) Uninverting(&#8203;_12e(&#8203;8.7.0):c60353:[diagnostics={os=Linux, java.version=11.0.9.1, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=merge, os.version=4.18.0-240.10.1.el8_3.x86_64, java.vendor=Red Hat, Inc., java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, mergeMaxNumSegments=-1, mergeFactor=10, timestamp=1613821796822}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8ii) Uninverting(&#8203;_12z(&#8203;8.7.0):c67016:[diagnostics={os=Linux, java.version=11.0.9.1, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=merge, os.version=4.18.0-240.10.1.el8_3.x86_64, java.vendor=Red Hat, Inc., java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, mergeMaxNumSegments=-1, mergeFactor=10, timestamp=1613821807440}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8k3) Uninverting(&#8203;_13c(&#8203;8.7.0):c61281:[diagnostics={os=Linux, java.version=11.0.9.1, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=merge, os.version=4.18.0-240.10.1.el8_3.x86_64, java.vendor=Red Hat, Inc., java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, mergeMaxNumSegments=-1, mergeFactor=10, timestamp=1613821813841}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8l4) Uninverting(&#8203;_15s(&#8203;8.7.0):c76868:[diagnostics={os=Linux, java.version=11.0.9.1, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=merge, os.version=4.18.0-240.10.1.el8_3.x86_64, java.vendor=Red Hat, Inc., java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, mergeMaxNumSegments=-1, mergeFactor=10, timestamp=1613821863932}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8sd) Uninverting(&#8203;_15a(&#8203;8.7.0):c65519:[diagnostics={os=Linux, java.version=11.0.9.1, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=merge, os.version=4.18.0-240.10.1.el8_3.x86_64, java.vendor=Red Hat, Inc., java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, mergeMaxNumSegments=-1, mergeFactor=10, timestamp=1613821853872}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8qv) Uninverting(&#8203;_151(&#8203;8.7.0):c43821:[diagnostics={os=Linux, java.version=11.0.9.1, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=merge, os.version=4.18.0-240.10.1.el8_3.x86_64, java.vendor=Red Hat, Inc., java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, mergeMaxNumSegments=-1, mergeFactor=10, timestamp=1613821849029}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8q4) Uninverting(&#8203;_15i(&#8203;8.7.0):c70282:[diagnostics={os=Linux, java.version=11.0.9.1, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=merge, os.version=4.18.0-240.10.1.el8_3.x86_64, java.vendor=Red Hat, Inc., java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, mergeMaxNumSegments=-1, mergeFactor=10, timestamp=1613821858813}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8rm) Uninverting(&#8203;_161(&#8203;8.7.0):c78334:[diagnostics={os=Linux, java.version=11.0.9.1, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=merge, os.version=4.18.0-240.10.1.el8_3.x86_64, java.vendor=Red Hat, Inc., java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, mergeMaxNumSegments=-1, mergeFactor=10, timestamp=1613821871628}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8t8) Uninverting(&#8203;_157(&#8203;8.7.0):C10000:[diagnostics={java.vendor=Red Hat, Inc., os=Linux, java.version=11.0.9.1, java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=flush, os.version=4.18.0-240.10.1.el8_3.x86_64, timestamp=1613821858118}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8r6) Uninverting(&#8203;_15f(&#8203;8.7.0):C10000:[diagnostics={java.vendor=Red Hat, Inc., os=Linux, java.version=11.0.9.1, java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=flush, os.version=4.18.0-240.10.1.el8_3.x86_64, timestamp=1613821861620}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8ro) Uninverting(&#8203;_15h(&#8203;8.7.0):C10000:[diagnostics={java.vendor=Red Hat, Inc., os=Linux, java.version=11.0.9.1, java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=flush, os.version=4.18.0-240.10.1.el8_3.x86_64, timestamp=1613821861695}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8rq) Uninverting(&#8203;_15g(&#8203;8.7.0):C10000:[diagnostics={java.vendor=Red Hat, Inc., os=Linux, java.version=11.0.9.1, java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=flush, os.version=4.18.0-240.10.1.el8_3.x86_64, timestamp=1613821861895}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8rt) Uninverting(&#8203;_15j(&#8203;8.7.0):C10000:[diagnostics={java.vendor=Red Hat, Inc., os=Linux, java.version=11.0.9.1, java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=flush, os.version=4.18.0-240.10.1.el8_3.x86_64, timestamp=1613821862472}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8rx) Uninverting(&#8203;_15q(&#8203;8.7.0):C10000:[diagnostics={java.vendor=Red Hat, Inc., os=Linux, java.version=11.0.9.1, java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=flush, os.version=4.18.0-240.10.1.el8_3.x86_64, timestamp=1613821866883}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8sg) Uninverting(&#8203;_15z(&#8203;8.7.0):C8145:[diagnostics={java.vendor=Red Hat, Inc., os=Linux, java.version=11.0.9.1, java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=flush, os.version=4.18.0-240.10.1.el8_3.x86_64, timestamp=1613821873651}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8ta) Uninverting(&#8203;_160(&#8203;8.7.0):C8532:[diagnostics={java.vendor=Red Hat, Inc., os=Linux, java.version=11.0.9.1, java.vm.version=11.0.9.1+1-LTS, lucene.version=8.7.0, os.arch=amd64, java.runtime.version=11.0.9.1+1-LTS, source=flush, os.version=4.18.0-240.10.1.el8_3.x86_64, timestamp=1613821873726}]:[attributes={Lucene87StoredFieldsFormat.mode=BEST_SPEED}] :id=c638st8wddoc6jo9oeh3uo8tc)))");
    Assert.assertEquals(24, c.calculateAttribute(metrics, "Value").intValue());
  }
}
