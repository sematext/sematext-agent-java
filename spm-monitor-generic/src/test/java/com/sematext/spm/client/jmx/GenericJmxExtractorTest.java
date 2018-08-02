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
package com.sematext.spm.client.jmx;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.DataFormat;
import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.Serializer;
import com.sematext.spm.client.StatsCollectionFailedException;
import com.sematext.spm.client.StatsCollector;
import com.sematext.spm.client.StatsCollectorBadConfigurationException;
import com.sematext.spm.client.yaml.YamlConfigLoader;

public class GenericJmxExtractorTest {

  @Before
  public void createSpmHomeDir() {
    File spmHome = new File("spm");
    if (!spmHome.exists()) {
      spmHome.mkdir();
    }
    System.setProperty("spm.home", spmHome.getAbsolutePath());
  }

  @After
  public void deleteSpmHomeDir() {
    File spmHome = new File("spm");
    spmHome.delete();
  }

  @Test
  public void test_create_resolve1()
      throws ConfigurationFailedException, StatsCollectorBadConfigurationException, StatsCollectionFailedException {

    MonitorConfig monitorConfig = new MonitorConfig("token", new File("src/test/resources/com/sematext/spm/client/jmx/spm-monitor-config-token-default.properties"), DataFormat.PLAIN_TEXT, 0);
    String configName = "jmxbean_simple.yml";
    JmxStatsExtractorConfig config = new JmxStatsExtractorConfig(YamlConfigLoader.load(getClass()
                                                                                           .getResourceAsStream(configName), configName), monitorConfig);
    String beanPath = "solr:dom1=core,dom2=collection1,dom3=shard3,dom4=replica5,category=QUERY,scope=/select,name=requests";

    for (MBeanObservation obs : config.getObservations()) {
      Map<String, String> objectNameTags = GenericJmxExtractor
          .getBeanPathTags(beanPath, obs.getObjectNamePattern().toString(),
                           obs.getObjectNamePatternGroupNames());

      if (GenericJmxExtractor.canBeMonitored(obs.getName(), config, objectNameTags)) {
        StatsCollector<?> collector = new GenericJmxCollector("solr", Serializer.INFLUX, monitorConfig,
                                                              configName, beanPath,
                                                              obs.getName(), obs
                                                                  .getOriginalObjectNamePattern(), config, objectNameTags);

        Assert.assertEquals(4, obs.getObjectNamePatternGroupNames().size());

        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("requestsCount", "123"); // set value for metric alias
        Map<String, String> resolvedTags = ((GenericJmxCollector) collector).getGenericExtractor().resolveTags(stats);
        Assert.assertEquals(5, resolvedTags.size());
        Assert.assertEquals("/select", resolvedTags.get("searchHandler"));
        Assert.assertEquals("replica5", resolvedTags.get("replica"));
        Assert.assertEquals("shard3", resolvedTags.get("shard"));
        Assert.assertEquals("collection1", resolvedTags.get("collection"));
        Assert.assertEquals("123", resolvedTags.get("exampleTag"));
      }
    }
  }

  @Test
  public void test_create_resolve2()
      throws ConfigurationFailedException, StatsCollectorBadConfigurationException, StatsCollectionFailedException {
    MonitorConfig monitorConfig = new MonitorConfig("token", new File("src/test/resources/com/sematext/spm/client/jmx/spm-monitor-config-token-default.properties"), DataFormat.PLAIN_TEXT, 0);
    String configName = "jmx_tomcat_webmodule.yml";
    JmxStatsExtractorConfig config = new JmxStatsExtractorConfig(YamlConfigLoader.load(getClass()
                                                                                           .getResourceAsStream(configName), configName), monitorConfig);
    String beanPath = "Catalina:j2eeType=WebModule,name=//localhost/,J2EEApplication=none,J2EEServer=none";

    for (MBeanObservation obs : config.getObservations()) {
      Map<String, String> objectNameTags = GenericJmxExtractor
          .getBeanPathTags(beanPath, obs.getObjectNamePattern().toString(),
                           obs.getObjectNamePatternGroupNames());

      if (GenericJmxExtractor.canBeMonitored(obs.getName(), config, objectNameTags)) {
        StatsCollector<?> collector = new GenericJmxCollector("tomcat", Serializer.INFLUX, monitorConfig,
                                                              configName, beanPath,
                                                              obs.getName(), obs
                                                                  .getOriginalObjectNamePattern(), config, objectNameTags);

        Assert.assertEquals(2, obs.getObjectNamePatternGroupNames().size());

        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("requestsCount", "123");
        Map<String, String> resolvedTags = ((GenericJmxCollector) collector).getGenericExtractor().resolveTags(stats);
        Assert.assertEquals(2, resolvedTags.size());
        Assert.assertEquals("localhost", resolvedTags.get("host"));
        Assert.assertEquals("", resolvedTags.get("web.app"));
      }
    }
  }

  @Test
  public void test_create_resolve3()
      throws ConfigurationFailedException, StatsCollectorBadConfigurationException, StatsCollectionFailedException {
    MonitorConfig monitorConfig = new MonitorConfig("token", new File("src/test/resources/com/sematext/spm/client/jmx/spm-monitor-config-token-default.properties"), DataFormat.PLAIN_TEXT, 0);
    String configName = "jmx_tomcat_datasource.yml";
    JmxStatsExtractorConfig config = new JmxStatsExtractorConfig(YamlConfigLoader.load(getClass()
                                                                                           .getResourceAsStream(configName), configName), monitorConfig);
    String beanPath = "Catalina:type=DataSource,host=localhost,context=/host-manager,class=javax.sql.DataSource,name=\"jdbc/TestDB\"";

    for (MBeanObservation obs : config.getObservations()) {
      Map<String, String> objectNameTags = GenericJmxExtractor
          .getBeanPathTags(beanPath, obs.getObjectNamePattern().toString(),
                           obs.getObjectNamePatternGroupNames());

      if (GenericJmxExtractor.canBeMonitored(obs.getName(), config, objectNameTags)) {
        StatsCollector<?> collector = new GenericJmxCollector("tomcat", Serializer.INFLUX, monitorConfig,
                                                              configName, beanPath,
                                                              obs.getName(), obs
                                                                  .getOriginalObjectNamePattern(), config, objectNameTags);

        Assert.assertEquals(2, obs.getObjectNamePatternGroupNames().size());

        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("requestsCount", "123");
        Map<String, String> resolvedTags = ((GenericJmxCollector) collector).getGenericExtractor().resolveTags(stats);
        Assert.assertEquals(2, resolvedTags.size());
        Assert.assertEquals("javax.sql.DataSource", resolvedTags.get("dsc"));
        Assert.assertEquals("jdbc/TestDB", resolvedTags.get("dsn"));
      }
    }
  }

  @Test
  public void testGetBeanPathTags() throws StatsCollectorBadConfigurationException {
    Map<String, String> tags = GenericJmxExtractor
        .getBeanPathTags("solr:dom1=core,dom2=gettingstarted,dom3=shard1,dom4=replica_n1,category=CACHE,scope=core,name=fieldCache",
                         "solr:dom1=core,dom2=*,dom3=*,dom4=*,category=CACHE,scope=core,name=fieldCache", Arrays
                             .asList("collection", "shard", "replica"));
    Assert.assertEquals("gettingstarted", tags.get("collection"));
    Assert.assertEquals("shard1", tags.get("shard"));
    Assert.assertEquals("replica_n1", tags.get("replica"));

    tags = GenericJmxExtractor
        .getBeanPathTags("solr:dom1=core,scope=core,dom3=shard1,dom2=gettingstarted,dom4=replica_n1,category=CACHE,name=fieldCache",
                         "solr:dom1=core,dom2=*,dom3=*,dom4=*,category=CACHE,scope=core,name=fieldCache", Arrays
                             .asList("collection", "shard", "replica"));
    Assert.assertEquals("gettingstarted", tags.get("collection"));
    Assert.assertEquals("shard1", tags.get("shard"));
    Assert.assertEquals("replica_n1", tags.get("replica"));

    tags = GenericJmxExtractor
        .getBeanPathTags("solr:dom1 = core,scope =core,dom3=shard1, dom2 =gettingstarted, dom4=replica_n1 , category =CACHE,name= fieldCache",
                         "solr:dom1 = core, dom2 =*,dom3=*, dom4=*, category = CACHE,scope =core,name= fieldCache ", Arrays
                             .asList("collection", "shard", "replica"));
    Assert.assertEquals("gettingstarted", tags.get("collection"));
    Assert.assertEquals("shard1", tags.get("shard"));
    Assert.assertEquals("replica_n1 ", tags.get("replica"));

    try {
      tags = GenericJmxExtractor
          .getBeanPathTags("solr:dom1 = core,scope =core,dom3=shard1,dom2 =gettingstarted, dom4=replica_n1 , category =CACHE,name= fieldCache",
                           "solr:dom1 = core, dom2 =*,dom3=*, dom4=*, category = CACHE,scope =core,name= fieldCache ", Arrays
                               .asList("collection", "shard", "replica"));
      Assert.fail();
    } catch (StatsCollectorBadConfigurationException e) {
      Assert.assertTrue(e.getMessage().contains("dom2"));
      // expected
    }
  }

  @Test
  public void testGetBeanPathTags2() throws StatsCollectorBadConfigurationException {
    Map<String, String> tags = GenericJmxExtractor
        .getBeanPathTags("Catalina:type=GlobalRequestProcessor,name=\"http-nio-8080\"",
                         "Catalina:type=GlobalRequestProcessor,name=\"http-*\"", Arrays.asList("port"));
    Assert.assertEquals("nio-8080", tags.get("port"));

    tags = GenericJmxExtractor.getBeanPathTags("Catalina:type=GlobalRequestProcessor,name=\"http-nio-8080\"",
                                               "Catalina:type=GlobalRequestProcessor,name=*", Arrays.asList("port"));
    Assert.assertEquals("http-nio-8080", tags.get("port"));

    tags = GenericJmxExtractor
        .getBeanPathTags("Catalina:j2eeType=WebModule,name=//localhost/,J2EEApplication=none,J2EEServer=none",
                         "Catalina:j2eeType=WebModule,name=//*/*,J2EEApplication=none,J2EEServer=none", Arrays
                             .asList("host_name", "webapp_name"));
    Assert.assertEquals("localhost", tags.get("host_name"));
    Assert.assertEquals("", tags.get("webapp_name"));

    tags = GenericJmxExtractor.getBeanPathTags("Catalina:type=GlobalRequestProcessor,name=\"http-nio-8080\"",
                                               "Catalina:type=GlobalRequestProcessor,name=*,*", Arrays.asList("port"));
    Assert.assertEquals("http-nio-8080", tags.get("port"));

    tags = GenericJmxExtractor.getBeanPathTags("Catalina:type=GlobalRequestProcessor,name=\"http-nio-8080\"",
                                               "Catalina:type=GlobalRequestProcessor,name=*,something*", Arrays
                                                   .asList("port", MBeanObservation.UNNAMED_GROUP_PREFIX + "1"));
    Assert.assertEquals("http-nio-8080", tags.get("port"));

    tags = GenericJmxExtractor
        .getBeanPathTags("Catalina:type=GlobalRequestProcessor,name=\"http-nio-8080\",x=1,otherName=22,*",
                         "Catalina:type=GlobalRequestProcessor,name=*,**,otherName=*", Arrays.asList("port",
                                                                                                     MBeanObservation.UNNAMED_GROUP_PREFIX
                                                                                                         + "1",
                                                                                                     MBeanObservation.UNNAMED_GROUP_PREFIX
                                                                                                         + "2", "otherPort"));
    Assert.assertEquals("http-nio-8080", tags.get("port"));
    Assert.assertEquals("22", tags.get("otherPort"));
  }

  @Test
  public void testGetBeanPathTags3() throws StatsCollectorBadConfigurationException {
    Map<String, String> tags = GenericJmxExtractor
        .getBeanPathTags("solr/myindex_shard1_replica3:type=someCache,id=SomeCache",
                         "solr/*:type=*,id=*Cache", Arrays
                             .asList("core", "cacheName", MBeanObservation.UNNAMED_GROUP_PREFIX + "1"));
    Assert.assertEquals("myindex_shard1_replica3", tags.get("core"));
    Assert.assertEquals("someCache", tags.get("cacheName"));

    tags = GenericJmxExtractor.getBeanPathTags("solr/myindex_shard1_replica3:type=someCache,id=SomeCache",
                                               "solr/*_shard1_*:type=*,id=*Cache", Arrays.asList("core",
                                                                                                 MBeanObservation.UNNAMED_GROUP_PREFIX
                                                                                                     + "1", "cacheName",
                                                                                                 MBeanObservation.UNNAMED_GROUP_PREFIX
                                                                                                     + "2"));
    Assert.assertEquals("myindex", tags.get("core"));
    Assert.assertEquals("someCache", tags.get("cacheName"));

    tags = GenericJmxExtractor.getBeanPathTags("solr/myindex_shard1_replica3:type=someCache,id=SomeCache",
                                               "solr/*_shard1_*:type=*,id=*", Arrays.asList("core",
                                                                                            MBeanObservation.UNNAMED_GROUP_PREFIX
                                                                                                + "1", "cacheName", "cacheId"));
    Assert.assertEquals("myindex", tags.get("core"));
    Assert.assertEquals("someCache", tags.get("cacheName"));
    Assert.assertEquals("SomeCache", tags.get("cacheId"));
  }
}
