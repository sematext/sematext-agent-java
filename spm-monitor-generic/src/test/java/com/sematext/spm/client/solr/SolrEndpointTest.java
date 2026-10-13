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
import java.util.Properties;

import com.sematext.spm.client.http.ServerInfo;

public class SolrEndpointTest {
  private static Map<String, String> props(String... keyValues) {
    Map<String, String> props = new HashMap<String, String>();
    for (int i = 0; i < keyValues.length; i += 2) {
      props.put(keyValues[i], keyValues[i + 1]);
    }
    return props;
  }

  @Test
  public void testSolr10PortOnBindAllUsesJmxHost() {
    Map<String, String> sysProps = props("solr.port.listen", "8984", "solr.host.bind", "0.0.0.0",
                                         "sun.java.command", "start.jar --module=http --module=gzip");

    Assert.assertEquals("http://10.0.0.5:8984", SolrEndpoint.fromSystemProperties(sysProps, "10.0.0.5"));
  }

  @Test
  public void testLocalJvmUsesLocalhost() {
    Map<String, String> sysProps = props("solr.port.listen", "8983");

    Assert.assertEquals("http://localhost:8983", SolrEndpoint.fromSystemProperties(sysProps, null));
  }

  @Test
  public void testSolr9JettyPort() {
    Map<String, String> sysProps = props("jetty.port", "8985");

    Assert.assertEquals("http://localhost:8985", SolrEndpoint.fromSystemProperties(sysProps, null));
  }

  @Test
  public void testSolr10PortWinsOverJettyPort() {
    Map<String, String> sysProps = props("solr.port.listen", "8984", "jetty.port", "8983");

    Assert.assertEquals("http://localhost:8984", SolrEndpoint.fromSystemProperties(sysProps, null));
  }

  @Test
  public void testRemoteJmxHostWinsOverSpecificBindAddress() {
    Map<String, String> sysProps = props("solr.port.listen", "8983", "solr.host.bind", "172.17.0.2");

    Assert.assertEquals("http://10.0.0.5:8983", SolrEndpoint.fromSystemProperties(sysProps, "10.0.0.5"));
  }

  @Test
  public void testSpecificBindAddressWithLocalJmx() {
    Map<String, String> sysProps = props("solr.port.listen", "8983", "solr.host.bind", "192.168.1.7");

    Assert.assertEquals("http://192.168.1.7:8983", SolrEndpoint.fromSystemProperties(sysProps, null));
    Assert.assertEquals("http://192.168.1.7:8983", SolrEndpoint.fromSystemProperties(sysProps, "localhost"));
    Assert.assertEquals("http://192.168.1.7:8983", SolrEndpoint.fromSystemProperties(sysProps, "127.0.0.1"));
  }

  @Test
  public void testUnsetBindNeverPointsAtTheAgentsOwnHost() {
    Map<String, String> sysProps = props("solr.port.listen", "8983");

    Assert.assertEquals("http://10.0.0.5:8983", SolrEndpoint.fromSystemProperties(sysProps, "10.0.0.5"));
    Assert.assertEquals("http://localhost:8983", SolrEndpoint.fromSystemProperties(sysProps, "127.0.0.1"));
  }

  @Test
  public void testIpv6BindAddressIsBracketed() {
    Map<String, String> sysProps = props("solr.port.listen", "8983", "solr.host.bind", "fe80::5");

    Assert.assertEquals("http://[fe80::5]:8983", SolrEndpoint.fromSystemProperties(sysProps, null));
  }

  @Test
  public void testRejectsUnsafeValues() {
    Assert.assertNull(SolrEndpoint.fromSystemProperties(props("solr.port.listen", "8983/x"), null));
    Assert.assertNull(SolrEndpoint.fromSystemProperties(props("solr.port.listen", "${X}"), null));
    Assert.assertNull(SolrEndpoint.fromSystemProperties(
        props("solr.port.listen", "8983", "solr.host.bind", "evil@host/${ST_MONITOR_SOLR_NODE_BASICAUTH_PASSWORD}"),
        null));
  }

  @Test
  public void testBlankOverrideIsUnset() {
    Properties empty = new Properties();
    empty.setProperty("SPM_MONITOR_SOLR_NODE_HOSTPORT", "");
    Assert.assertNull(SolrEndpoint.configured(empty));

    Properties quotedEmpty = new Properties();
    quotedEmpty.setProperty("ST_MONITOR_SOLR_NODE_HOSTPORT", "\"\"");
    Assert.assertNull(SolrEndpoint.configured(quotedEmpty));

    Properties set = new Properties();
    set.setProperty("ST_MONITOR_SOLR_NODE_HOSTPORT", "\"https://solr-1:8984/\"");
    Assert.assertEquals("https://solr-1:8984", SolrEndpoint.configured(set));
  }

  @Test
  public void testServerInfoSplitsLikeJsonConfigs() {
    ServerInfo info = SolrEndpoint.serverInfo("localhost:8983", null, null);
    Assert.assertEquals("http://localhost:8983", info.getServer());
    Assert.assertEquals("localhost", info.getHostname());
    Assert.assertEquals("8983", info.getPort());

    ServerInfo proxied = SolrEndpoint.serverInfo("https://proxy:8443/solr1", null, null);
    Assert.assertEquals("https://proxy:8443/solr1", proxied.getServer());
    Assert.assertEquals("proxy", proxied.getHostname());
    Assert.assertEquals("8443", proxied.getPort());
  }

  @Test
  public void testUnderscoreHostnames() {
    Map<String, String> sysProps = props("solr.port.listen", "8983", "solr.host.bind", "0.0.0.0");

    Assert.assertEquals("http://project_solr_1:8983", SolrEndpoint.fromSystemProperties(sysProps, "project_solr_1"));
  }

  @Test
  public void testIpv6BindAllIsNotAHost() {
    Map<String, String> sysProps = props("solr.port.listen", "8983", "solr.host.bind", "::");

    Assert.assertEquals("http://localhost:8983", SolrEndpoint.fromSystemProperties(sysProps, null));
  }

  @Test
  public void testHttpsModule() {
    Map<String, String> sysProps = props("solr.port.listen", "8983",
                                         "sun.java.command", "start.jar --module=https --lib=x");

    Assert.assertEquals("https://localhost:8983", SolrEndpoint.fromSystemProperties(sysProps, null));
  }

  @Test
  public void testNoSolrPortMeansNoEndpoint() {
    Assert.assertNull(SolrEndpoint.fromSystemProperties(props("java.version", "21"), null));
  }

  @Test
  public void testOnlySolrEndpointPlaceholderNeedsResolving() {
    Assert.assertTrue(SolrEndpoint.isNeededBy("server: ${ST_MONITOR_SOLR_NODE_HOSTPORT}"));
    Assert.assertTrue(SolrEndpoint.isNeededBy("server: ${SPM_MONITOR_SOLR_NODE_HOSTPORT}"));
    Assert.assertFalse(SolrEndpoint.isNeededBy("server: ${ST_MONITOR_ES_NODE_HOSTPORT}"));
    Assert.assertFalse(SolrEndpoint.isNeededBy("objectName: solr:dom1=core"));
  }

  @Test
  public void testFillsEveryPlaceholderVariant() {
    String content = "server: ${ST_MONITOR_SOLR_NODE_HOSTPORT}\nother: ${SPM_MONITOR_SOLR_NODE_HOSTPORT}\n" +
        "user: ${ST_MONITOR_SOLR_NODE_BASICAUTH_USERNAME}";

    Assert.assertEquals("server: http://localhost:8984\nother: http://localhost:8984\n" +
                            "user: ${ST_MONITOR_SOLR_NODE_BASICAUTH_USERNAME}",
                        SolrEndpoint.replacePlaceholders(content, "http://localhost:8984"));
  }

  @Test
  public void testJmxHostFromUrl() {
    Assert.assertNull(SolrEndpoint.jmxHost(null));
    Assert.assertEquals("10.0.0.5", SolrEndpoint.jmxHost("10.0.0.5:18983"));
    Assert.assertEquals("solr-host", SolrEndpoint.jmxHost("service:jmx:rmi:///jndi/rmi://solr-host:18983/jmxrmi"));
    Assert.assertEquals("[fd00::5]", SolrEndpoint.jmxHost("[fd00::5]:9010"));
  }

  @Test
  public void testIpv6JmxHostIsUsable() {
    Map<String, String> sysProps = props("solr.port.listen", "8983", "solr.host.bind", "0.0.0.0");

    Assert.assertEquals("http://[fd00::5]:8983", SolrEndpoint.fromSystemProperties(sysProps, "[fd00::5]"));
  }
}
