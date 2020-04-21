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
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;

public class GenericStatsCollectorsFactoryTest {
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  @Ignore
  public void testGenericStatsCollectorsFactory()
      throws ConfigurationFailedException, StatsCollectorBadConfigurationException {
    GenericStatsCollectorsFactory f = new GenericStatsCollectorsFactory();
    String token = "8882ca62-0439-40ed-80a8-486690a03f99";
    File propsFile = new File("/opt/spm/spm-monitor/conf/spm-monitor-config-" + token + "-default.properties");
    MonitorConfig monitorConfig = new MonitorConfig(token, propsFile, DataFormat.PLAIN_TEXT, 1);

    Properties monitorProps = new Properties();
    try {
      monitorProps.load(new FileInputStream(propsFile));
    } catch (IOException e) {
      e.printStackTrace();
      throw new ConfigurationFailedException(e.getMessage());
    }

    Collection<? extends StatsCollector> collectors = f
        .create(monitorProps, new ArrayList<StatsCollector<?>>(), monitorConfig);

    for (StatsCollector sc : collectors) {
      Iterator<String> data = sc.collect(Collections.EMPTY_MAP);
      while (data.hasNext())
        System.out.println(data.next());
    }
  }

  @Test
  public void testAttributePlaceholderResolving() {
    Map<String, String> allTags = new HashMap<String, String>();

    GenericStatsCollectorsFactory f = new GenericStatsCollectorsFactory();
    ParsedAttributeName p = f.parseAttributeName("${topic}-${partition}.records-lag");
    Matcher m = p.patternMatcher.matcher("fetch-latency-avg");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("records-lag-max");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("bytes-consumed-rate");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("records-consumed-rate");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-latency-max");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-rate");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-throttle-time-avg");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-size-max");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-throttle-time-max");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-size-avg");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("records-per-request-avg");
    Assert.assertTrue(!m.find());

    m = p.patternMatcher.matcher("myTopic-0.records-lag");
    Assert.assertTrue(m.find());
    m = p.patternMatcher.matcher("myTopic-0.records-lag-max");
    Assert.assertTrue(!m.find());

    p = f.parseAttributeName("${topic}-${partition}.records-lag-avg");
    m = p.patternMatcher.matcher("fetch-latency-avg");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("records-lag-max");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("bytes-consumed-rate");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("records-consumed-rate");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-latency-max");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-rate");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-throttle-time-avg");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-size-max");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-throttle-time-max");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-size-avg");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("records-per-request-avg");
    Assert.assertTrue(!m.find());

    p = f.parseAttributeName("${topic}-${partition}.records-lag-max");
    m = p.patternMatcher.matcher("fetch-latency-avg");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("records-lag-max");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("bytes-consumed-rate");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("records-consumed-rate");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-latency-max");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-rate");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-throttle-time-avg");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-size-max");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-throttle-time-max");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("fetch-size-avg");
    Assert.assertTrue(!m.find());
    m = p.patternMatcher.matcher("records-per-request-avg");
    Assert.assertTrue(!m.find());

    p = f.parseAttributeName("${topic}-${partition}.records-lag-max");
    m = p.patternMatcher.matcher("mytopic-1.records-lag-max");
    Assert.assertTrue(m.find());
    Assert.assertEquals(p.placeholderNames.get(0), "topic");
    Assert.assertEquals(p.placeholderNames.get(1), "partition");
    f.resolveAttributePlaceholders(p, m, allTags);
    Assert.assertEquals(allTags.get("topic"), "mytopic");
    Assert.assertEquals(allTags.get("partition"), "1");
  }
  
  @Test
  public void testGetPropertyVariants() {
    GenericStatsCollectorsFactory f = new GenericStatsCollectorsFactory();
    List<String> variants = f.getPropertyVariants("SPM_SOME_PROP");
    Assert.assertEquals(3, variants.size());
    Assert.assertEquals("ST_SOME_PROP", variants.get(0));
    Assert.assertEquals("SPM_SOME_PROP", variants.get(1));
    Assert.assertEquals("SOME_PROP", variants.get(2));

    variants = f.getPropertyVariants("ST_SOME_PROP");
    Assert.assertEquals(3, variants.size());
    Assert.assertEquals("ST_SOME_PROP", variants.get(0));
    Assert.assertEquals("SPM_SOME_PROP", variants.get(1));
    Assert.assertEquals("SOME_PROP", variants.get(2));

    variants = f.getPropertyVariants("SOME_PROP");
    Assert.assertEquals(3, variants.size());
    Assert.assertEquals("ST_SOME_PROP", variants.get(0));
    Assert.assertEquals("SPM_SOME_PROP", variants.get(1));
    Assert.assertEquals("SOME_PROP", variants.get(2));
  }
}
