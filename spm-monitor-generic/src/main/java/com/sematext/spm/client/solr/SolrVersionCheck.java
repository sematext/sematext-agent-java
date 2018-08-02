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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.jmx.JmxMBeanServerConnectionWrapper;
import com.sematext.spm.client.jmx.JmxServiceContext;
import com.sematext.spm.client.observation.BaseVersionConditionCheck;

public class SolrVersionCheck extends BaseVersionConditionCheck {
  private static final Log LOG = LogFactory.getLog(SolrVersionCheck.class);
  private static final String SOLR_7_PLUS_VERSION = "solr:dom1=node,category=CONTAINER,scope=version,name=specification Value";
  private static final String SOLR_6_VERSION = "solr:dom1=code,dom2=*,reporter=*,category=OTHER,scope=debug,name=debug version";
  private static final String SOLR_5_VERSION = "solr/*:type=debug,id=debug version";
  private static final String SOLR_4_VERSION = "solr/*:type=debug,id=debug version";

  // newer versions should go first since they expose the version, but also we want to first check the versions that are most likely
  // to be used (lower overhead on them)
  private static final List<String> BEAN_PATTERNS = Arrays.asList(SOLR_7_PLUS_VERSION.trim(), SOLR_6_VERSION.trim(),
                                                                  SOLR_5_VERSION.trim(), SOLR_4_VERSION.trim());

  @Override
  protected String readVersion() {
    MBeanServerConnection conn = null;
    JmxMBeanServerConnectionWrapper wrapper = JmxMBeanServerConnectionWrapper.getInstance(
        JmxServiceContext.getContext(getMonitorConfig().getMonitorPropertiesFile()));

    if (wrapper != null) {
      conn = wrapper.getMbeanServerConnection();
    }

    if (conn != null) {
      for (String pattern : BEAN_PATTERNS) {
        String objectNamePattern = pattern.substring(0, pattern.lastIndexOf(" "));
        String attribName = pattern.substring(pattern.lastIndexOf(" ") + 1);

        String version = extract(conn, objectNamePattern, attribName);

        if (version != null) {
          return version;
        }
      }
    }

    // in case we couldn't find the version, we'll assume "3" (because version is exposed in some way only from 4.0)
    LOG.info("Solr version not recognized, returning version number (as fallback): 3");
    return "3";
  }

  private String extract(MBeanServerConnection conn, String objectNamePattern, String attribName) {
    try {
      Set<ObjectInstance> versionObject = conn.queryMBeans(new ObjectName(objectNamePattern), null);
      if (!versionObject.isEmpty()) {
        ObjectInstance obj = versionObject.iterator().next();

        try {
          Object versionObj = conn.getAttribute(obj.getObjectName(), attribName);
          if (versionObj != null) {
            LOG.info("Detected Solr version : " + versionObj);

            return versionObj.toString();
          } else {
            return null;
          }
        } catch (AttributeNotFoundException anfe) {
          return null;
        } catch (Throwable thr) {
          return null;
        }
      } else {
        return null;
      }
    } catch (Throwable thr) {
      LOG.warn("Error while reading Solr version, returning null", thr);
      return null;
    }
  }
}
