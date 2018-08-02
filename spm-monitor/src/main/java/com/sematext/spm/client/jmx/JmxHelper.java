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

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

public final class JmxHelper {
  static final Log LOG = LogFactory.getLog(JmxHelper.class);

  private JmxHelper() {
  }

  public static Object queryJmx(JmxServiceContext ctx, String objectName, String attributeName) {
    MBeanServerConnection conn = null;
    JmxMBeanServerConnectionWrapper wrapper = JmxMBeanServerConnectionWrapper.getInstance(ctx);

    if (wrapper != null) {
      conn = wrapper.getMbeanServerConnection();
    }

    Object jmxObject;
    try {
      jmxObject = conn.getAttribute(new ObjectName(objectName), attributeName);
    } catch (Throwable thr) {
      LOG.warn("Error while reading attribute " + attributeName + " of jmx object " + objectName + ", error was: " +
                   thr.getMessage());
      jmxObject = null;
    }
    return jmxObject;
  }
}
