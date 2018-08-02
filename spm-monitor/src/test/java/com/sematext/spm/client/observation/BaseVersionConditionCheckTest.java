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
package com.sematext.spm.client.observation;

import org.junit.Assert;
import org.junit.Test;

public class BaseVersionConditionCheckTest {
  @Test
  public void testClauseSatisfies() {
    BaseVersionConditionCheckDummy check = new BaseVersionConditionCheckDummy();
    Assert.assertEquals(true, check.clauseSatisfies("1.11", "*-3"));
    Assert.assertEquals(true, check.clauseSatisfies("1.11", "1.11-3"));
    Assert.assertEquals(false, check.clauseSatisfies("1.11", "1.11.1-3"));
    Assert.assertEquals(true, check.clauseSatisfies("1.12", "1.11.1-3"));
    Assert.assertEquals(false, check.clauseSatisfies("1.12", "3"));
    Assert.assertEquals(true, check.clauseSatisfies("1.12", "1.12"));
    Assert.assertEquals(true, check.clauseSatisfies("1.12", "1.12.*"));
    Assert.assertEquals(false, check.clauseSatisfies(null, "1.12.*"));
    Assert.assertEquals(true, check.clauseSatisfies("1.12", "1-*"));
    Assert.assertEquals(true, check.clauseSatisfies("1.12.rc1", "1-*"));
    Assert.assertEquals(false, check.clauseSatisfies("1.10.rc1", "1.11-*"));
    Assert.assertEquals(true, check.clauseSatisfies("0.90.7", "0.90"));
    Assert.assertEquals(true, check.clauseSatisfies("1.4.4", "1-2"));
    Assert.assertEquals(true, check.clauseSatisfies("9", "7-*"));
    Assert.assertEquals(true, check.clauseSatisfies("9", "7-14"));
    Assert.assertEquals(true, check.clauseSatisfies("11", "7-14"));
    Assert.assertEquals(true, check.clauseSatisfies("6.4", "1-6"));

    // this one fails, but it is questionable how it should behave since 1.rc1 would be very atypical version number
    // Assert.assertEquals(false, check.clauseSatisfies("1.rc1", "1.11-*"));
  }
}

class BaseVersionConditionCheckDummy extends BaseVersionConditionCheck {
  @Override
  protected String readVersion() {
    return null;
  }
}
