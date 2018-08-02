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

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 */
@Ignore
public class MonitorConfigTest {
  private File spmHome;

  @Before
  public void createSpmHomeDir() {
    spmHome = new File("spm");
    if (!spmHome.exists()) {
      spmHome.mkdir();
    }
    System.setProperty("spm.home", spmHome.getAbsolutePath());
  }

  @After
  public void deleteSpmHomeDir() throws IOException {
    FileUtils.cleanDirectory(spmHome);
  }

  @Test
  public void testMinimal() throws ConfigurationFailedException {
    MonitorConfig config = new MonitorConfig("token", new File(
        "src/test/resources/com/sematext/spm/client/jmx/spm-monitor-config-token-default.properties"),
                                             DataFormat.PLAIN_TEXT, 0);
    Assert.assertEquals(
        spmHome.getAbsolutePath() + "/spm-monitor/logs/applications/token/default/", config.getLogBasedir());
    Assert.assertEquals(10 * 1024 * 1024, config.getLogMaxFileSize());
    Assert.assertEquals(10, config.getLogMaxBackups());
  }

  @Test
  public void testConfig() throws ConfigurationFailedException {
    MonitorConfig config = new MonitorConfig("token", new File(
        "src/test/resources/com/sematext/spm/client/jmx/spm-monitor-config-token-default2.properties"),
                                             DataFormat.PLAIN_TEXT, 0);

    Assert.assertEquals(
        spmHome.getAbsolutePath() + "/spm-monitor/logs/applications/token/default2/", config.getLogBasedir());
    Assert.assertEquals("DEBUG", config.getLogLevel());
    Assert.assertEquals(10002, config.getMonitorCollectInterval());
  }

}
