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
package com.sematext.spm.client.command;

import org.eclipse.collections.impl.list.mutable.FastList;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.sematext.spm.client.jmx.JmxMBeanServerConnectionWrapper;
import com.sematext.spm.client.jmx.JmxServiceContext;

public final class JMXClient {

  private JMXClient() {
  }

  public static List<GarbageCollectorMXBean> getGarbageCollectorMXBeans(JmxServiceContext ctx) {
    final MBeanServerConnection serverConnection = JmxMBeanServerConnectionWrapper.getInstance(ctx)
        .getMbeanServerConnection();
    final List<GarbageCollectorMXBean> beans = new FastList<GarbageCollectorMXBean>();
    try {
      for (final ObjectName name : serverConnection
          .queryNames(new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE), null)) {
        beans.add(ManagementFactory
                      .newPlatformMXBeanProxy(serverConnection, name.getCanonicalName(), GarbageCollectorMXBean.class));
      }
    } catch (IOException e) {
      throw new IllegalStateException("Can't get garbage collector mx beans.", e);
    } catch (MalformedObjectNameException e) {
      throw new IllegalStateException("Can't get garbage collector mx beans.", e);
    }

    return beans;
  }

  public static ThreadMXBean getThreadMXBean(JmxServiceContext ctx) {
    final JmxMBeanServerConnectionWrapper wrapper = JmxMBeanServerConnectionWrapper.getInstance(ctx);
    if (wrapper == null) {
      throw new IllegalStateException("JMX connection not initialized.");
    }
    final MBeanServerConnection serverConnection = wrapper.getMbeanServerConnection();
    try {
      return ManagementFactory
          .newPlatformMXBeanProxy(serverConnection, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
    } catch (IOException e) {
      throw new IllegalStateException("Can't get thread mx bean.", e);
    }
  }
}
