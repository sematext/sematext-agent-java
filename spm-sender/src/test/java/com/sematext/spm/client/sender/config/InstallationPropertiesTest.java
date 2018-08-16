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

package com.sematext.spm.client.sender.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Maps;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Map;
import java.util.Properties;

import com.sematext.spm.client.util.FileUtil;
import com.sematext.spm.client.util.test.TmpFS;

public class InstallationPropertiesTest {

  private static String serialize(Map<String, String> props) {
    try {
      final Properties p = new Properties();
      for (final String key : props.keySet()) {
        p.setProperty(key, props.get(key));
      }

      final ByteArrayOutputStream os = new ByteArrayOutputStream();
      p.store(os, "");

      return new String(os.toByteArray());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testLoadSpmSenderInstallationProperties() throws Exception {
    final TmpFS fs = TmpFS.fs();
    try {
      final Map<String, String> props = Maps.newHashMap();
      props.put("proxy_host", "");
      props.put("server_base_url", "https://spm-receiver.sematext.com/receiver/v1");

      final File spmHome = fs.createDirectory();
      final File propertiesDir = fs.createDirectory(spmHome, "properties");
      final File p1 = fs.createFile(propertiesDir, "spm-sender.properties", serialize(props));
      props.put("server_base_url", "https://spm-receiver.sematext.com/receiver/v2");
      final File p2 = fs.createFile(propertiesDir, "spm-setup-1.properties", serialize(props));
      props.put("server_base_url", "https://spm-receiver.sematext.com/receiver/v3");
      final File p3 = fs.createFile(propertiesDir, "spm-setup-2.properties", serialize(props));

      final Configuration config = Configuration.configuration(spmHome.getAbsolutePath());

      InstallationProperties properties = InstallationProperties.loadSpmSenderInstallationProperties(config);
      assertNotNull(properties);

      assertEquals("https://spm-receiver.sematext.com/receiver/v1", properties.getProperties()
          .get("server_base_url"));

      assertTrue(p1.delete());

      long now = System.currentTimeMillis();
      assertTrue(p3.setLastModified(now));
      assertTrue(p2.setLastModified(now - 24 * 60 * 60 * 1000));

      properties = InstallationProperties.loadSpmSenderInstallationProperties(config);

      assertEquals("https://spm-receiver.sematext.com/receiver/v3", properties.getProperties()
          .get("server_base_url"));

      props.remove("server_base_url");
      FileUtil.write(serialize(props), p3);

      properties = InstallationProperties.loadSpmSenderInstallationProperties(config);

      assertEquals(config.getDefaultReceiverUrl(), properties.getProperties().get("server_base_url"));
      assertEquals(config.getDefaultMetricsEndpoint(), properties.getProperties().get("metrics_endpoint"));
    } finally {
      fs.cleanup();
    }
  }

  @Test
  public void testInstallationPropertiesShouldReloadPropertyFilesWhenChanged() throws Exception {
    final TmpFS fs = TmpFS.fs();
    try {
      final Map<String, String> props = Maps.newHashMap();
      props.put("proxy_host", "");
      props.put("server_base_url", "https://spm-receiver.sematext.com/receiver/v1");

      final File spmHome = fs.createDirectory();
      final File propertiesDir = fs.createDirectory(spmHome, "properties");
      final File spmSenderConfig = fs.createFile(propertiesDir, "spm-sender.properties", serialize(props));

      assertTrue(spmSenderConfig.setLastModified(System.currentTimeMillis() - 24 * 60 * 60 * 1000));

      final Configuration config = Configuration.configuration(spmHome.getAbsolutePath());
      final InstallationProperties properties = InstallationProperties.loadSpmSenderInstallationProperties(config);

      assertEquals("https://spm-receiver.sematext.com/receiver/v1", properties.getProperties()
          .get("server_base_url"));

      props.put("server_base_url", "https://spm-receiver.sematext.com/receiver/v3");

      FileUtil.write(serialize(props), spmSenderConfig);

      assertTrue(spmSenderConfig.setLastModified(System.currentTimeMillis()));

      assertEquals("https://spm-receiver.sematext.com/receiver/v3", properties.getProperties()
          .get("server_base_url"));
    } finally {
      fs.cleanup();
    }
  }

  @Test
  public void testFallbackProperties() throws Exception {
    final Map<String, String> props1 = Maps.newHashMap();
    props1.put("name", "jack");
    props1.put("city", "istambul");
    props1.put("country", "turkey");

    final Map<String, String> props2 = Maps.newHashMap();
    props2.put("name", "mick");
    props2.put("city", "sydney");

    final InstallationProperties p =
        InstallationProperties.staticProperties(props2).fallbackTo(InstallationProperties.staticProperties(props1));

    assertEquals("mick", p.getProperties().get("name"));
    assertEquals("sydney", p.getProperties().get("city"));
    assertEquals("turkey", p.getProperties().get("country"));
  }

  @Test
  public void testFallbackPropertiesFromFile() throws Exception {
    final TmpFS fs = TmpFS.fs();
    try {
      final File f1 = fs.createFile("name=jack\ncity=istambul\ncountry=turkey");
      final File f2 = fs.createFile("name=mick\ncity=sydney");

      final InstallationProperties p =
          InstallationProperties.fromFile(f2).fallbackTo(InstallationProperties.fromFile(f1));

      assertEquals("mick", p.getProperties().get("name"));
      assertEquals("sydney", p.getProperties().get("city"));
      assertEquals("turkey", p.getProperties().get("country"));

      assertTrue(f2.delete());

      final InstallationProperties p1 =
          InstallationProperties.fromFile(f2).fallbackTo(InstallationProperties.fromFile(f1));

      assertEquals("jack", p1.getProperties().get("name"));
      assertEquals("istambul", p1.getProperties().get("city"));
      assertEquals("turkey", p1.getProperties().get("country"));

      final File f3 = fs.createFile("");
      assertTrue(f3.delete());

      final File dir = fs.createDirectory();
      fs.createFile(fs.createDirectory(dir, "properties"), "spm-sender.properties", "name=jack\n" +
          "city=istambul\n" +
          "country=turkey");

      Configuration config = Configuration.configuration(dir.getAbsolutePath());
      final InstallationProperties fallback = InstallationProperties.loadSpmSenderInstallationProperties(config);

      final InstallationProperties p2 =
          InstallationProperties.fromFile(f3).fallbackTo(fallback);

      assertEquals("jack", p2.getProperties().get("name"));
      assertEquals("istambul", p2.getProperties().get("city"));
      assertEquals("turkey", p2.getProperties().get("country"));

      assertEquals("jack", fallback.getProperties().get("name"));
      assertEquals("istambul", fallback.getProperties().get("city"));
      assertEquals("turkey", fallback.getProperties().get("country"));

      final File d1 = fs.createDirectory();
      InstallationProperties p3 = InstallationProperties
          .fromFile(new File(d1.getAbsolutePath() + File.separator + "properties"))
          .fallbackTo(InstallationProperties.fromResource("/default-properties-1.properties"));

      File xmlConfig = new File(d1.getAbsolutePath() + File.separator + "xml");
      xmlConfig.delete();
      xmlConfig.createNewFile();
      xmlConfig.setLastModified(123);

      ChangeWatcher.Watch p3Watch = p3.createWatch(xmlConfig.getAbsolutePath());

      assertEquals("Jack", p3.getProperties().get("name"));

      fs.createFile(d1, "properties", "name=Mick");

      Thread.sleep(2000);

      assertTrue(p3Watch.isChanged());

      assertEquals("Mick", p3.getProperties().get("name"));

    } finally {
      fs.cleanup();
    }
  }
}
