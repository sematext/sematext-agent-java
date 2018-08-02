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
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.util.jar.JarFile;

import com.sematext.spm.client.MonitorUtil.MonitorArgs;
import com.sematext.spm.client.util.JarUtils;

/**
 * Tricky bootstrap process. Be careful during modification of this class. It can't afford:
 * <ul>
 * <li>any usages of anonymous classes</li>
 * <li>any third party dependencies</li>
 * </ul>
 * Adds itself to bootstrap class path, so previous modifications can lead to problems with class loading.
 */
public final class MonitorAgentBootstrap {
  private static final String SPM_MONITOR_AGENT_CLASS = "com.sematext.spm.client.MonitorAgent";

  private final String jarPath;
  private final File tmpDirPath;
  private final String tmpFilePrefix;

  private MonitorAgentBootstrap(String jarPath, MonitorUtil.MonitorArgs args) {
    this.jarPath = jarPath;
    System.out.println("Monitor tmp dir: " + MonitorUtil.getMonitorTmpDirPath());
    this.tmpDirPath = new File(MonitorUtil.getMonitorTmpDirPath());
    this.tmpFilePrefix = buildTmpFilePrefix(args);
  }

  private void bootstrap(String klassName, String entrypoint, Class<?>[] types, Object[] params,
                         boolean useBootstrapClasspath)
      throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
      IllegalAccessException {
    MonitorAgentBootstrapThread bootstrapThread = new MonitorAgentBootstrapThread(klassName, entrypoint, types, params, useBootstrapClasspath, jarPath, tmpDirPath, tmpFilePrefix);
    bootstrapThread.setDaemon(true);

    //in agent mode we have to start separate thread to avoid overlapping with current thread
    // context classloader
    if (useBootstrapClasspath) {
      //start new thread
      bootstrapThread.start();
      try {
        // we have to block current thread while bootstrapThread finish initializing its part
        bootstrapThread.join();
      } catch (InterruptedException e) {
        System.out.println("InterruptedException while waiting bootstrapThread thread. Message: " + e.getMessage());
      }
    } else {
      //run code in the same thread
      bootstrapThread.run();
    }

  }

  private static String buildTmpFilePrefix(MonitorUtil.MonitorArgs args) {
    String tmpFilePrefix = args.getToken() + "-" + args.getJvmName();
    if (args.getSubType() != null && !args.getSubType().trim().isEmpty()) {
      tmpFilePrefix = tmpFilePrefix + "-" + args.getSubType().trim();
    }
    return tmpFilePrefix;
  }

  @SuppressWarnings("deprecated")

  private void appendAgentToBootstrapClassLoader(Instrumentation instr) throws IOException {
    instr.appendToBootstrapClassLoaderSearch(new JarFile(jarPath));
  }

  /**
   * Bootstraps agent:
   * <ul>
   * <li>Loads current jar in bootstrap classloader</li> - it allows instrumentation of jdk classes, like java.net.URL
   * <li>Creates classloader with all jars in monitor-libs/ folder</li> - to isolate our classes which are using 3rd party
   * libraries from host process
   * <li>Invokes premain method of agent (com.sematext.spm.client.MonitorAgent) using previously created classloader</li>
   * </ul>
   *
   * @param args  args passed to premain entrypoint
   * @param instr instrumentation instance passed to premain entrypoint
   * @throws Exception exception
   */
  public static void bootstrap(String args, Instrumentation instr) throws Exception {
    final String jarPath = JarUtils.getJarPath(MonitorAgentBootstrap.class);
    System.out.println("SPM Monitor Agent Path: " + jarPath + ".");

    final MonitorArgs monitorArgs = MonitorUtil.extractMonitorArgs(args);
    final MonitorAgentBootstrap bootstrap = new MonitorAgentBootstrap(jarPath, monitorArgs);
    if (instr != null) {
      bootstrap.appendAgentToBootstrapClassLoader(instr);
    }
    bootstrap.bootstrap(SPM_MONITOR_AGENT_CLASS, "premain", new Class<?>[] { String.class, Instrumentation.class },
                        new Object[] { args, instr }, instr != null);

    System.out.println("SPM Monitor Agent Initialized.");
  }

  public static void bootstrapStandalone(String args) throws Exception {
    final String jarPath = JarUtils.getJarPath(MonitorAgentBootstrap.class);
    System.out.println("SPM Monitor Agent Path: " + jarPath + ".");

    final MonitorArgs monitorArgs = MonitorUtil.extractMonitorArgs(args);
    final MonitorAgentBootstrap bootstrap = new MonitorAgentBootstrap(jarPath, monitorArgs);
    bootstrap
        .bootstrap("com.sematext.spm.client.StandaloneMonitorInternalAgent", "main", new Class<?>[] { String[].class },
                   new Object[] { new String[] { args } }, false);

    System.out.println("SPM Monitor Standalone Agent Initialized.");
  }
}
