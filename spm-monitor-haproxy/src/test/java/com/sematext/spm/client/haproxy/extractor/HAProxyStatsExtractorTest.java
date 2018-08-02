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
package com.sematext.spm.client.haproxy.extractor;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import com.sematext.spm.client.StatsCollectionFailedException;
import com.sematext.spm.client.haproxy.extractor.HAProxyStatsExtractor.HAProxyStats;
import com.sematext.spm.client.http.ServerInfo;

public class HAProxyStatsExtractorTest {

  @Test
  public void test() throws StatsCollectionFailedException {
    String url = "http://demo.1wt.eu/;csv";
    ServerInfo serverInfo = new ServerInfo(url, null, null, null, null);
    HAProxyStatsExtractor extractor = new HAProxyStatsExtractor(serverInfo, url, null);

    List<HAProxyStats> stats = extractor.getStats();
    Assert.assertFalse(stats.isEmpty());
  }

}
