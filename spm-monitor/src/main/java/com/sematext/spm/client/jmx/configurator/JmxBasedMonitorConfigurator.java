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
package com.sematext.spm.client.jmx.configurator;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;

import com.sematext.spm.client.MonitorConfigurator;
import com.sematext.spm.client.jmx.JmxMBeanServerConnectionWrapper;
import com.sematext.spm.client.jmx.JmxServiceContext;

public abstract class JmxBasedMonitorConfigurator extends MonitorConfigurator {
  protected static QueryExp getQueryExpression(String... ignoredObjectKeys) {
    try {
      QueryExp queryExp = null;
      for (String key : ignoredObjectKeys) {
        QueryExp clause = Query.not(new ObjectName(key));
        if (queryExp == null) {
          queryExp = clause;
        } else {
          queryExp = Query.and(queryExp, clause);
        }
      }
      return queryExp;
    } catch (MalformedObjectNameException e) {
      throw new RuntimeException("Invalid ignored object key", e);
    }
  }

  protected static final QueryExp IGNORE_SOLR_FIELD_CACHE = getQueryExpression("*:type=fieldCache,*",
                                                                               "*:type=fieldValueCache,*");

  public abstract void readConfiguration(JmxServiceContext ctx) throws IOException;

  /**
   * Finds specified attribute of given ObjectName. If it doesn't exist, returns null;
   *
   * @param objectName
   * @param attributeName
   * @return
   * @throws IOException
   */
  protected String findAttribute(JmxServiceContext ctx, ObjectName objectName, String attributeName) {
    try {
      MBeanServerConnection conn = getMbeanServer(ctx);

      if (conn == null) {
        return null;
      }

      Object val = conn.getAttribute(objectName, attributeName);

      if (val != null) {
        return String.valueOf(val);
      } else {
        return null;
      }
    } catch (Throwable thr) {
      // in case of any error, just ignore, since that probably means that there is no such attribute
      return null;
    }
  }

  /**
   * Can return null in case connection can't be created at some point
   *
   * @return
   */
  protected MBeanServerConnection getMbeanServer(JmxServiceContext ctx) {
    JmxMBeanServerConnectionWrapper wrapper = JmxMBeanServerConnectionWrapper.getInstance(ctx);

    if (wrapper != null) {
      return wrapper.getMbeanServerConnection();
    } else {
      return null;
    }
  }
}
