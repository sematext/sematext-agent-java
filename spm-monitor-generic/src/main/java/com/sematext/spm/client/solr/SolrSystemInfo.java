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

import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.http.ServerInfo;
import com.sematext.spm.client.json.JsonDataSourceCachedFactory;

/**
 * Reads Solr's /admin/info/system, which still serves JSON in Solr 10 where JMX metrics are gone.
 */
public final class SolrSystemInfo {
  private static final Log LOG = LogFactory.getLog(SolrSystemInfo.class);

  private static final String SYSTEM_INFO_PATH = "/solr/admin/info/system?wt=json";

  private SolrSystemInfo() {
  }

  public static Map<String, Object> fetch(ServerInfo serverInfo) {
    if (serverInfo == null) {
      return null;
    }
    try {
      return (Map<String, Object>) JsonDataSourceCachedFactory
          .getDataSource(serverInfo, serverInfo.getServer() + SYSTEM_INFO_PATH, false, false).fetchData();
    } catch (Throwable thr) {
      LOG.warn("Can't read Solr system info from " + serverInfo.getServer(), thr);
      return null;
    }
  }

  static String version(Map<String, Object> systemInfo) {
    if (systemInfo == null || !(systemInfo.get("lucene") instanceof Map)) {
      return null;
    }
    Object version = ((Map<String, Object>) systemInfo.get("lucene")).get("solr-spec-version");
    return version == null ? null : version.toString();
  }

  static String mode(Map<String, Object> systemInfo) {
    if (systemInfo == null || systemInfo.get("mode") == null) {
      return null;
    }
    return systemInfo.get("mode").toString();
  }
}
