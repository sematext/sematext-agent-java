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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MonitorAgentBootstrapThread extends Thread {
  private static final Log LOG = LogFactory.getLog(MonitorAgentBootstrapThread.class);
  private String klassName;
  private String entrypoint;
  private Class<?>[] types;
  private Object[] params;
  private boolean useBootstrapClasspath;
  private final String jarPath;
  private final File tmpDirPath;
  private final String tmpFilePrefix;

  public MonitorAgentBootstrapThread(String klassName, String entrypoint, Class<?>[] types, Object[] params,
                                     boolean useBootstrapClasspath, String jarPath, File tmpDirPath,
                                     String tmpFilePrefix) {
    super("MonitorAgentBootstrapThread");
    this.klassName = klassName;
    this.entrypoint = entrypoint;
    this.types = types;
    this.params = params;
    this.useBootstrapClasspath = useBootstrapClasspath;
    this.jarPath = jarPath;
    this.tmpDirPath = tmpDirPath;
    this.tmpFilePrefix = tmpFilePrefix;
  }

  @Override
  public void run() {
    try {
      final long startTs = System.currentTimeMillis();
      try {
        prepareTmpFileDir();
        removeOldTmpFiles();
      } catch (Exception e) {
        System.out.println("Can't remove old tmp jar files");
        LOG.printStackTrace(e);
      }

      final List<JarEntry> entriesToBootstrap = new ArrayList<JarEntry>();
      final JarFile jarFile = new JarFile(jarPath);
      final Enumeration<JarEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        final JarEntry entry = entries.nextElement();
        if (entry.getName().endsWith(".jar")) {
          entriesToBootstrap.add(entry);
        }
      }

      final URL[] urls = materializeJars(jarFile, entriesToBootstrap);
      final URLClassLoader loader;

      File commonJarDir = new File(MonitorUtil.getMonitorCommonJarDirPath());
      List<URL> commonJarURLs = new ArrayList<URL>();
      if (commonJarDir.exists() && commonJarDir.isDirectory()) {
        File[] commonJarFiles = commonJarDir.listFiles(new ExtensionFileFilter("jar"));
        for (File commonJarFile : commonJarFiles) {
          commonJarURLs.add(commonJarFile.toURI().toURL());
        }
      } else {
        System.out.println("Common jars dir doesn't exist, path: " + MonitorUtil.getMonitorCommonJarDirPath());
      }

      File monitorConfigFile;
      if (params[0] instanceof String) {
        // javaagent
        String javaagentParam = (String) params[0];
        MonitorUtil.MonitorArgs monitorArgs = MonitorUtil.extractMonitorArgs(javaagentParam);
        monitorConfigFile = MonitorUtil
            .fetchSpmMonitorPropertiesFileObject(monitorArgs.getToken(), monitorArgs.getJvmName(), monitorArgs
                .getSubType());
      } else {
        // standalone
        monitorConfigFile = new File(((String[]) params[0])[0]);
      }

      System.out.println("Monitor config file is : " + monitorConfigFile);
      Properties monitorProps = MonitorUtil.loadMonitorProperties(monitorConfigFile);

      if(monitorProps == null) {
        System.out.println("Unable to open monitor config file.");
        return;
      }

      String collectors = MonitorUtil.stripQuotes(monitorProps.getProperty("SPM_MONITOR_COLLECTORS").trim()).trim();
      for (String collector : collectors.split(",")) {
        collector = collector.trim();
        if (collector.equals("")) continue;
        File configDir = new File(MonitorUtil
                                      .createPathString(MonitorUtil.SPM_HOME, "spm-monitor", "collectors", collector, "lib"));
        System.out.println("Looking at collector config dir: " + configDir);
        if (configDir.isDirectory()) {
          File[] configDirJarFiles = configDir.listFiles(new ExtensionFileFilter("jar"));
          for (File configDirJarFile : configDirJarFiles) {
            System.out.println("Found custom jar file: " + configDirJarFile);
            commonJarURLs.add(configDirJarFile.toURI().toURL());
          }
        }
      }

      File toplLevelAgentConfigDir = new File(MonitorUtil
                                                  .createPathString(MonitorUtil.SPM_HOME, "spm-monitor", "collectors", "lib"));
      if (toplLevelAgentConfigDir.isDirectory()) {
        File[] configDirJarFiles = toplLevelAgentConfigDir.listFiles(new ExtensionFileFilter("jar"));
        for (File configDirJarFile : configDirJarFiles) {
          System.out.println("Found top-level custom jar file: " + configDirJarFile);
          commonJarURLs.add(configDirJarFile.toURI().toURL());
        }
      }

      System.out.println("Following common jars will be added to classloader: " + commonJarURLs);

      URLClassLoader commonJarsClassloader;

      if (useBootstrapClasspath) {
        System.out.println("Running on Java version " + MonitorUtil.JAVA_VERSION);
        if (MonitorUtil.JAVA_MAJOR_VERSION >= 9) {
          System.out.println("Using Platform ClassLoader as a parent...");
          Method method = ClassLoader.class.getMethod("getPlatformClassLoader");
          commonJarsClassloader = new URLClassLoader(commonJarURLs.toArray(new URL[commonJarURLs.size()]),
                                                     (ClassLoader) method.invoke(null));
        } else {
          System.out.println("Using Bootstrap ClassLoader as a parent...");
          commonJarsClassloader = new URLClassLoader(commonJarURLs.toArray(new URL[commonJarURLs.size()]), null);
        }
      } else {
        commonJarsClassloader = new URLClassLoader(commonJarURLs.toArray(new URL[commonJarURLs.size()]));
      }
      commonJarsClassloader.setDefaultAssertionStatus(false);

      loader = new URLClassLoader(urls, commonJarsClassloader);
      loader.setDefaultAssertionStatus(false);

      System.out.println("SPM Monitor Agent Bootstrapped in " + (System.currentTimeMillis() - startTs) + " ms.");

      // we need this to avoid any intersections between app classloader
      // and classloaders we use for our agent
      if (useBootstrapClasspath) {
        this.setContextClassLoader(loader);
      }

      final Class<?> klass = Class.forName(klassName, true, loader);
      final Method method = klass.getMethod(entrypoint, types);
      method.invoke(null, params);
    } catch (IOException e) {
      System.out.println("Can't start MonitorAgentBootstrapThread");
      e.printStackTrace();
      LOG.printStackTrace(e);
    } catch (InvocationTargetException e) {
      System.out.println("Can't start MonitorAgentBootstrapThread");
      e.printStackTrace();
      LOG.printStackTrace(e);
    } catch (NoSuchMethodException e) {
      System.out.println("Can't start MonitorAgentBootstrapThread");
      e.printStackTrace();
      LOG.printStackTrace(e);
    } catch (IllegalAccessException e) {
      System.out.println("Can't start MonitorAgentBootstrapThread");
      e.printStackTrace();
      LOG.printStackTrace(e);
    } catch (ClassNotFoundException e) {
      System.out.println("Can't start MonitorAgentBootstrapThread");
      e.printStackTrace();
      LOG.printStackTrace(e);
    }
  }

  private URL[] materializeJars(JarFile jarFile, List<JarEntry> entries) throws IOException {
    final List<URL> urls = new ArrayList<URL>();
    for (final JarEntry entry : entries) {
      urls.add(materializeJar(jarFile, entry));
    }
    System.out.println("Added following urls to classpath: " + urls + ".");
    return urls.toArray(new URL[urls.size()]);
  }

  private String processTmpFileName(String name) {
    if (tmpFilePrefix != null) {
      return tmpFilePrefix + "-" + name;
    } else {
      return name;
    }
  }

  private URL materializeJar(JarFile jarFile, JarEntry jarEntry) throws IOException {
    InputStream input = null;
    OutputStream output = null;
    try {
      String name = jarEntry.getName().replace(File.separatorChar, '_');
      int i = name.lastIndexOf(".");
      String extension = i > -1 ? name.substring(i) : "";
      File file;
      try {
        file = File.createTempFile(
            processTmpFileName(name.substring(0, name.length() - extension.length())) + ".", extension, tmpDirPath);
      } catch (IOException e) {
        throw new IOException("File not found: " + tmpDirPath + ".", e);
      }
      file.deleteOnExit();
      input = jarFile.getInputStream(jarEntry);
      output = new FileOutputStream(file);
      int readCount;
      byte[] buffer = new byte[4096];
      while ((readCount = input.read(buffer)) != -1) {
        output.write(buffer, 0, readCount);
      }
      return file.toURL();
    } finally {
      if (input != null) {
        input.close();
      }
      if (output != null) {
        output.close();
      }
    }
  }

  private void prepareTmpFileDir() {
    try {
      if (!tmpDirPath.exists()) {
        tmpDirPath.setReadable(true, false);
        tmpDirPath.setWritable(true, false);
        tmpDirPath.mkdirs();
        System.out.println("Create dir: " + tmpDirPath.getAbsolutePath());
      }
      MonitorUtil.adjustPermissions(tmpDirPath.getCanonicalPath(), "777");
    } catch (IOException e) {
      System.out.println("Can't adjust permission, dir: " + tmpDirPath.getAbsolutePath());
      LOG.printStackTrace(e);
    }
  }

  private void removeOldTmpFiles() {
    removeFiles(new File(System.getProperty("java.io.tmpdir")));
  }

  private void removeFiles(File tmpDir) {
    if (!tmpDir.isDirectory()) {
      System.out.println("TmpDir is not directory, path: " + tmpDir.getAbsolutePath());
      return;
    }

    File[] filesToDelete = tmpDir.listFiles();

    System.out.println("Tmp dir:" + tmpDir.getAbsolutePath() + ", files for deletion count " + filesToDelete.length);

    for (final File file : filesToDelete) {
      if (!file.getName().startsWith("monitor-libs_spm") || file.getName().endsWith(".jar")) {
        continue;
      }
      try {
        if (!file.delete()) {
          LOG.warn("Can't remove " + file.getAbsolutePath());
        } else {
          System.out.println("Removed file: " + file.getAbsolutePath());
        }
      } catch (Exception e) {
        System.out.println("Can't remove " + file.getAbsolutePath());
      }
    }
  }

}
