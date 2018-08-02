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
import java.util.LinkedHashMap;
import java.util.Map;

public class MethodPointcutTest {

  @Test
  public void testCanonicalOneParam() {
    MethodPointcut pointCut = make("void org.apache.solr.handler.component.SearchComponent#prepare(org.apache.solr.handler.component.ResponseBuilder responseBuilder)");
    assertEquals("void", pointCut.getReturnType());
    assertEquals("org.apache.solr.handler.component.SearchComponent", pointCut.getTypeName());
    assertEquals("prepare", pointCut.getMethodName());
    assertEquals(Collections.singletonMap("responseBuilder", "org.apache.solr.handler.component.ResponseBuilder"),
                 pointCut.getParams());
  }

  @Test
  public void testCanonicalOneParamWithPhysical() {
    MethodPointcut pointCut = make("com.some.texxa.Void$In org.apache.solr.handler.component.SearchComponent$In#prepare(org.apache.solr.handler.component.ResponseBuilder$In responseBuilder)");
    assertEquals("com.some.texxa.Void$In", pointCut.getReturnType());
    assertEquals("org.apache.solr.handler.component.SearchComponent$In", pointCut.getTypeName());
    assertEquals("prepare", pointCut.getMethodName());
    assertEquals(Collections.singletonMap("responseBuilder", "org.apache.solr.handler.component.ResponseBuilder$In"),
                 pointCut.getParams());
  }

  @Test
  public void testCanonicalTwoParam() {
    MethodPointcut pointCut = make("void org.apache.solr.handler.component.SearchComponent#prepare(org.apache.solr.handler.component.ResponseBuilder responseBuilderX, org.apache.solr.handler.component.ResponseBuilder responseBuilderY)");
    assertEquals("void", pointCut.getReturnType());
    assertEquals("org.apache.solr.handler.component.SearchComponent", pointCut.getTypeName());
    assertEquals("prepare", pointCut.getMethodName());
    Map<String, String> params = new LinkedHashMap<String, String>();
    params.put("responseBuilderX", "org.apache.solr.handler.component.ResponseBuilder");
    params.put("responseBuilderY", "org.apache.solr.handler.component.ResponseBuilder");
    assertEquals(params, pointCut.getParams());
  }

  @Test
  public void testCanonicalNoParams() {
    MethodPointcut pointCut = make("void org.apache.solr.handler.component.SearchComponent#prepare()");
    assertEquals("void", pointCut.getReturnType());
    assertEquals("org.apache.solr.handler.component.SearchComponent", pointCut.getTypeName());
    assertEquals("prepare", pointCut.getMethodName());
    assertEquals(Collections.emptyMap(), pointCut.getParams());
  }

  @Test
  public void testCanonicalTwoParamWithSpaces() {
    MethodPointcut pointCut = make("  void    org.apache.solr.handler.component.SearchComponent#prepare( org.apache.solr.handler.component.ResponseBuilder  responseBuilderX ,  org.apache.solr.handler.component.ResponseBuilder  responseBuilderY ) ");
    assertEquals("void", pointCut.getReturnType());
    assertEquals("org.apache.solr.handler.component.SearchComponent", pointCut.getTypeName());
    assertEquals("prepare", pointCut.getMethodName());
    Map<String, String> params = new LinkedHashMap<String, String>();
    params.put("responseBuilderX", "org.apache.solr.handler.component.ResponseBuilder");
    params.put("responseBuilderY", "org.apache.solr.handler.component.ResponseBuilder");
    assertEquals(params, pointCut.getParams());
  }

  @Test
  public void testArrayParams() {
    MethodPointcut pointCut = make(" void org.apache.solr.handler.component.SearchComponent#prepare(org.apache.solr.handler.component.ResponseBuilder[] responseBuilders) ");
    assertEquals("void", pointCut.getReturnType());
    assertEquals("org.apache.solr.handler.component.SearchComponent", pointCut.getTypeName());
    assertEquals("prepare", pointCut.getMethodName());
    Map<String, String> params = new LinkedHashMap<String, String>();
    params.put("responseBuilders", "org.apache.solr.handler.component.ResponseBuilder[]");
    assertEquals(params, pointCut.getParams());
  }

  @Test
  public void testArraySecondParams() {
    MethodPointcut pointCut = make(" void org.apache.solr.handler.component.SearchComponent#prepare(org.apache.solr.handler.component.ResponseBuilder  responseBuilder , org.apache.solr.handler.component.ResponseBuilder[] responseBuilders) ");
    assertEquals("void", pointCut.getReturnType());
    assertEquals("org.apache.solr.handler.component.SearchComponent", pointCut.getTypeName());
    assertEquals("prepare", pointCut.getMethodName());
    Map<String, String> params = new LinkedHashMap<String, String>();
    params.put("responseBuilder", "org.apache.solr.handler.component.ResponseBuilder");
    params.put("responseBuilders", "org.apache.solr.handler.component.ResponseBuilder[]");
    assertEquals(params, pointCut.getParams());
  }

  @Test(expected = Exception.class)
  public void testCanonicalTwoParamWithExtraCommaInParameters() {
    MethodPointcut pointCut = make("void org.apache.solr.handler.component.SearchComponent#prepare(org.apache.solr.handler.component.ResponseBuilder  responseBuilderX ,  org.apache.solr.handler.component.ResponseBuilder  responseBuilderY ,)");
  }

  private static MethodPointcut make(String serializedForm) {
    return MethodPointcut.FACTORY.make(serializedForm);
  }

}
