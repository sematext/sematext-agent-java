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

public class SolrVersionCheckTest {
  private static class StubbedCheck extends SolrVersionCheck {
    private final String jmxVersion;
    private final String httpVersion;
    private boolean httpCalled;

    StubbedCheck(String jmxVersion, String httpVersion) {
      this.jmxVersion = jmxVersion;
      this.httpVersion = httpVersion;
    }

    @Override
    protected String readVersionOverJmx() {
      return jmxVersion;
    }

    @Override
    protected String readVersionOverHttp() {
      httpCalled = true;
      return httpVersion;
    }

    boolean matches(String version, String range) {
      return clauseSatisfies(version, range);
    }
  }

  @Test
  public void testJmxVersionSkipsHttp() {
    StubbedCheck check = new StubbedCheck("9.11.0", "10.0.0");

    Assert.assertEquals("9.11.0", check.readVersion());
    Assert.assertFalse(check.httpCalled);
  }

  @Test
  public void testSolr10HasNoJmxVersionSoHttpIsUsed() {
    StubbedCheck check = new StubbedCheck(null, "10.0.0");

    Assert.assertEquals("10.0.0", check.readVersion());
    Assert.assertTrue(check.httpCalled);
  }

  @Test
  public void testFallsBackToOldestWhenNothingAnswers() {
    Assert.assertEquals("3", new StubbedCheck(null, null).readVersion());
  }

  @Test
  public void testSolr10DoesNotMatchJmxRanges() {
    StubbedCheck check = new StubbedCheck(null, "10.0.0");
    String version = check.readVersion();

    Assert.assertTrue(check.matches(version, "10-*"));
    Assert.assertFalse(check.matches(version, "7-9"));
    Assert.assertFalse(check.matches(version, "1-6"));
    Assert.assertTrue(check.matches("9.11.0", "7-9"));
    Assert.assertFalse(check.matches("9.11.0", "10-*"));
  }

  @Test
  public void testCacheRangeForSolr7Plus() {
    StubbedCheck check = new StubbedCheck(null, null);

    Assert.assertTrue(check.matches("9.11.0", "7.4.1-9"));
    Assert.assertFalse(check.matches("10.0.0", "7.4.1-9"));
    Assert.assertTrue(check.matches("10.0.0-SNAPSHOT", "10-*"));
  }
}
