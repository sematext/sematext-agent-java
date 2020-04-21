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

import java.util.List;
import java.util.Properties;

public class MonitorUtilTest {
  @Test
  public void testGetPropertyVariants() {
    List<String> variants = MonitorUtil.getPropertyVariants("SPM_SOME_PROP");
    Assert.assertEquals(3, variants.size());
    Assert.assertEquals("ST_SOME_PROP", variants.get(0));
    Assert.assertEquals("SPM_SOME_PROP", variants.get(1));
    Assert.assertEquals("SOME_PROP", variants.get(2));

    variants = MonitorUtil.getPropertyVariants("ST_SOME_PROP");
    Assert.assertEquals(3, variants.size());
    Assert.assertEquals("ST_SOME_PROP", variants.get(0));
    Assert.assertEquals("SPM_SOME_PROP", variants.get(1));
    Assert.assertEquals("SOME_PROP", variants.get(2));

    variants = MonitorUtil.getPropertyVariants("SOME_PROP");
    Assert.assertEquals(3, variants.size());
    Assert.assertEquals("ST_SOME_PROP", variants.get(0));
    Assert.assertEquals("SPM_SOME_PROP", variants.get(1));
    Assert.assertEquals("SOME_PROP", variants.get(2));
    
    variants = MonitorUtil.getPropertyVariants("ST_TEST_PROP");
    Assert.assertEquals(3, variants.size());
    Assert.assertEquals("ST_TEST_PROP", variants.get(0));
    Assert.assertEquals("SPM_TEST_PROP", variants.get(1));
    Assert.assertEquals("TEST_PROP", variants.get(2));
  }
  
  @Test
  public void testGetPropertyFromAnyNameVariant() {
    Properties props = new Properties();
    props.put("PROP1", "prop1");
    props.put("SPM_PROP2", "prop2");
    props.put("ST_PROP3", "prop3");
    
    Assert.assertEquals("prop1", MonitorUtil.getPropertyFromAnyNameVariant(props, "PROP1", null));
    Assert.assertEquals("prop1", MonitorUtil.getPropertyFromAnyNameVariant(props, "ST_PROP1", null));
    Assert.assertEquals("prop1", MonitorUtil.getPropertyFromAnyNameVariant(props, "SPM_PROP1", null));
    Assert.assertEquals(null, MonitorUtil.getPropertyFromAnyNameVariant(props, "PPPROP1", null));

    Assert.assertEquals("prop2", MonitorUtil.getPropertyFromAnyNameVariant(props, "PROP2", null));
    Assert.assertEquals("prop2", MonitorUtil.getPropertyFromAnyNameVariant(props, "ST_PROP2", null));
    Assert.assertEquals("prop2", MonitorUtil.getPropertyFromAnyNameVariant(props, "SPM_PROP2", null));
    Assert.assertEquals(null, MonitorUtil.getPropertyFromAnyNameVariant(props, "PPPROP2", null));

    Assert.assertEquals("prop3", MonitorUtil.getPropertyFromAnyNameVariant(props, "PROP3", null));
    Assert.assertEquals("prop3", MonitorUtil.getPropertyFromAnyNameVariant(props, "ST_PROP3", null));
    Assert.assertEquals("prop3", MonitorUtil.getPropertyFromAnyNameVariant(props, "SPM_PROP3", null));
    Assert.assertEquals(null, MonitorUtil.getPropertyFromAnyNameVariant(props, "PPPROP1", null));
  }
}
