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
package com.sematext.spm.client.tag;

import org.eclipse.collections.impl.list.mutable.FastList;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.Serializer;
import com.sematext.spm.client.StatValues;

public class AllTagAliasStatsCollector {
  private static final Log LOG = LogFactory.getLog(AllTagAliasStatsCollector.class);

  private MultipleTagAliasCollector tagsCollector;
  private File monitorPropsFile;
  private String appToken;
  private String jvmName;

  public AllTagAliasStatsCollector(String appToken, String jvmName, String subType, File monitorPropsFile) {
    this.monitorPropsFile = monitorPropsFile;
    this.appToken = appToken;
    this.jvmName = jvmName;

    // for 'plain' tags
    tagsCollector = new MultipleTagAliasCollector(Serializer.INFLUX, appToken, jvmName, subType, monitorPropsFile);
  }

  public List<StatValues> collect() {
    List<StatValues> all = new FastList<StatValues>();

    try {
      Properties monitorProperties = new Properties();
      monitorProperties.load(new FileInputStream(monitorPropsFile));

      if (tagsCollector != null) {
        tagsCollector.refreshTagAliasDefinitions(monitorProperties);
        Iterator<StatValues> iter = tagsCollector.collectRawStatValues(null);
        StatValues val = mergeIntoOne(iter, "config");
        if (val != null) {
          all.add(val);
        }
      }
    } catch (Exception e) {
      LOG.error("Error while gathering and sending tags", e);
    }
    return all;
  }

  private StatValues mergeIntoOne(Iterator<StatValues> iter, String type) {
    StatValues merged = null;

    while (iter.hasNext()) {
      StatValues sv = iter.next();
      if (merged == null) {
        merged = sv;
        merged.getTags().put("tag.alias.type", type);
      } else {
        merged.getTags().putAll(sv.getTags());
        merged.getMetrics().putAll(sv.getMetrics());
      }
    }

    return merged;
  }
}
