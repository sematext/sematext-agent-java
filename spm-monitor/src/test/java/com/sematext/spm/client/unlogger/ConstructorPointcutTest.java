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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConstructorPointcutTest {

  @Test
  public void testSimpleConstructor() {
    ConstructorPointcut pointCut = make("org.apache.lucene.index.CorruptIndexException()");
    assertEquals("org.apache.lucene.index.CorruptIndexException", pointCut.getTypeName());
    assertEquals(Collections.emptyMap(), pointCut.getParams());
  }

  @Test
  public void testConstructorWithParam() {
    ConstructorPointcut pointCut = make("org.apache.lucene.index.CorruptIndexException(java.lang.String message)");
    assertEquals("org.apache.lucene.index.CorruptIndexException", pointCut.getTypeName());
    assertEquals(Collections.singletonMap("message", "java.lang.String"), pointCut.getParams());
  }

  @Test
  public void testConstructorWithDigitsInParams() {
    ConstructorPointcut pointCut = make("org.apache.solr.common.SolrException(org.apache.solr.common.SolrException$ErrorCode param0,java.lang.String param1,boolean param2)");
    assertEquals("org.apache.solr.common.SolrException", pointCut.getTypeName());
    Map<String, String> params = new HashMap<String, String>();
    params.put("param0", "org.apache.solr.common.SolrException$ErrorCode");
    params.put("param1", "java.lang.String");
    params.put("param2", "boolean");
    assertEquals(params, pointCut.getParams());
  }

  private static ConstructorPointcut make(String serializedForm) {
    return ConstructorPointcut.FACTORY.make(serializedForm);
  }

}
