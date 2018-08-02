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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.sematext.spm.client.jmx.JmxServiceContext;

/**
 * Uses MonitorAgent to provide most of its logic. Only deals with JMX parameters and setting up service context according to them.
 */
public class StandaloneMonitorInternalAgent {
  private static Log log;

  protected StandaloneMonitorInternalAgent() {
  }

  public static void main(String[] args) {
    log = LogFactory.getLog(StandaloneMonitorInternalAgent.class);

    try {
      prepareJmxServiceContext(args);

      MonitorUtil.MONITOR_RUNTIME_SETUP_JAVAAGENT.set(false);

      log.info("Starting with JVM args : " + System.getProperties().toString());

      // iterate over all agent configs and start the monitor for each
      MonitorAgent.startMonitoring(args[0], null);
    } catch (Throwable thr) {
      log.error("Error while starting the monitor!", thr);
      throw new IllegalStateException("Error while starting the monitor!", thr);
    }
  }

  public static JmxServiceContext prepareJmxServiceContext(String[] args) throws IOException {
    String propsFile = args[0];
    File monitorConfigFile = new File(propsFile);
    Properties monitorProps = new Properties();
    monitorProps.load(new FileInputStream(monitorConfigFile));
    String token = MonitorUtil.extractToken(propsFile);
    String jvmName = MonitorUtil.extractJvmName(propsFile, token);
    String subtype = MonitorUtil.extractConfSubtype(propsFile);

    return JmxServiceContext.init(monitorProps, token, jvmName, subtype);
  }
}
