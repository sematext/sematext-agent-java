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

import java.io.File;
import java.util.Map;

import com.sematext.spm.client.observation.CalculationFunction;

public class CalculateCoreNumFiles implements CalculationFunction {
  @Override
  public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    throw new UnsupportedOperationException("Can't be used in tag context");
  }

  @Override
  public Number calculateAttribute(Map<String, Object> metrics, Object... params) {
    if (params != null && params.length == 2) {
      String metricName = params[0].toString();
      boolean extractIndexDir = Boolean.parseBoolean(params[1].toString());
      String readerDir = (String) metrics.get(metricName);
      String indexDir = readerDir;
      if (extractIndexDir) {
        indexDir = extractIndexDir(readerDir);
      }
      File f = new File(indexDir);
      if (f.exists()) {
        return f.listFiles().length;
      } else {
        return 0;
      }
    } else {
      throw new IllegalArgumentException("Missing metric name and extractIndexDir params");
    }
  }

  private String extractIndexDir(String readerDir) {
    // NRTCachingDirectory(org.apache.lucene.store.NIOFSDirectory@/home/user/Downloads/solr-4.1.0/exampleMultiple/solr/consumer4_ac_shard1_replica1/data/index lockFactory=org.apache.lucene.store.NativeFSLockFactory@4e6c17; maxCacheMB=48.0 maxMergeSizeMB=4.0)
    // NRTCachingDirectory(org.apache.lucene.store.NIOFSDirectory@/home/user/Downloads/solr-4.1.0/exampleMultiple/solr/consumer4_ac_shard1_replica1/data/index.20130502123432 lockFactory=org.apache.lucene.store.NativeFSLockFactory@4e6c17; maxCacheMB=48.0 maxMergeSizeMB=4.0)
    if (readerDir.contains("/index")) {
      if (readerDir.contains("/index.")) {
        String tmp = readerDir.substring(readerDir.indexOf("@") + 1);
        String tmp2 = tmp.substring(tmp.indexOf("/index."));
        int indexOfSpace = tmp2.indexOf(" ");
        return tmp.substring(0, tmp.indexOf("/index.") + indexOfSpace);
      } else {
        return readerDir.substring(readerDir.indexOf("@") + 1, readerDir.indexOf("/index") + "/index".length());
      }
    } else if (readerDir.contains("/Index")) {
      if (readerDir.contains("/Index.")) {
        String tmp = readerDir.substring(readerDir.indexOf("@") + 1);
        String tmp2 = tmp.substring(tmp.indexOf("/Index."));
        int indexOfSpace = tmp2.indexOf(" ");
        return tmp.substring(0, tmp.indexOf("/Index.") + indexOfSpace);
      } else {
        return readerDir.substring(readerDir.indexOf("@") + 1, readerDir.indexOf("/Index") + "/Index".length());
      }
    } else if (readerDir.contains(" lockFactory")) {
      return readerDir.substring(readerDir.indexOf("@") + 1, readerDir.indexOf(" lockFactory"));
    } else {
      return readerDir.substring(readerDir.indexOf("@") + 1, readerDir.indexOf(" "));
    }
  }
}
