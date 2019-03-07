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
package com.sematext.spm.client.sender.config;

import com.google.common.collect.Lists;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.util.IOUtils;

public final class InstallationProperties {
  private static final Log LOG = LogFactory.getLog(InstallationProperties.class);

  public static interface Loader {
    boolean needsReload();

    Map<String, String> load();

    ChangeWatcher.Watch createWatch(String dependentConfigFilePath);
  }

  public static final class FileLoader implements Loader {
    private final List<File> files;
    private long lastModified = 0L;

    public FileLoader(List<File> files) {
      this.files = files;
    }

    @Override
    public ChangeWatcher.Watch createWatch(String dependentConfigFilePath) {
      return ChangeWatcher.files(files, dependentConfigFilePath);
    }

    public boolean needsReload() {
      for (File file : files) {
        if (file.lastModified() > lastModified) {
          return true;
        }
      }
      return false;
    }

    public Map<String, String> load() {
      final Map<String, String> properties = new UnifiedMap<String, String>();
      for (final File file : files) {
        if (file.lastModified() > lastModified) {
          lastModified = file.lastModified();
        }
        final Properties props = new Properties();
        FileInputStream is = null;
        try {
          is = new FileInputStream(file);
          props.load(is);
        } catch (IOException e) {
          throw new IllegalStateException("Can't load properties.", e);
        } finally {
          IOUtils.closeQuietly(is);
        }
        for (final String key : props.stringPropertyNames()) {
          properties.put(key, props.getProperty(key));
        }
      }
      return properties;
    }
  }

  public static final class ClassPathLoader implements Loader {
    private final AtomicBoolean reload = new AtomicBoolean(false);
    private final List<String> resources;

    public ClassPathLoader(List<String> resources) {
      this.resources = resources;
    }

    @Override
    public ChangeWatcher.Watch createWatch(String dependentConfigFilePath) {
      return new ChangeWatcher.Watch() {
        @Override
        public boolean isChanged() {
          return ClassPathLoader.this.needsReload();
        }
      };
    }

    public boolean needsReload() {
      return reload.compareAndSet(false, true);
    }

    public Map<String, String> load() {
      final Map<String, String> properties = new UnifiedMap<String, String>();
      for (final String path : resources) {
        final Properties props = new Properties();
        InputStream is = null;
        try {
          is = InstallationProperties.class.getResourceAsStream(path);
          props.load(is);
        } catch (IOException e) {
          throw new IllegalStateException("Can't load properties.", e);
        } finally {
          IOUtils.closeQuietly(is);
        }
        for (final String key : props.stringPropertyNames()) {
          properties.put(key, props.getProperty(key));
        }
      }
      return properties;
    }
  }

  public static final class StaticLoader implements Loader {
    private final Map<String, String> props;
    private final AtomicBoolean reload = new AtomicBoolean(false);

    public StaticLoader(Map<String, String> props) {
      this.props = props;
    }

    @Override
    public ChangeWatcher.Watch createWatch(String dependentConfigFilePath) {
      return new ChangeWatcher.Watch() {
        @Override
        public boolean isChanged() {
          return StaticLoader.this.needsReload();
        }
      };
    }

    @Override
    public boolean needsReload() {
      return reload.compareAndSet(false, true);
    }

    @Override
    public Map<String, String> load() {
      return props;
    }
  }

  private Map<String, String> properties = new UnifiedMap<String, String>();
  private final Loader loader;
  private final Lock lock = new ReentrantLock();
  private final Map<String, String> defaults;

  private InstallationProperties(Loader loader, Map<String, String> defaults) {
    this.loader = loader;
    this.defaults = defaults;
  }

  public ChangeWatcher.Watch createWatch(String dependentConfigFilePath) {
    return loader.createWatch(dependentConfigFilePath);
  }

  public void reload() {
    lock.lock();
    try {
      try {
        properties = new UnifiedMap<String, String>(defaults);

        properties.putAll(loader.load());
      } catch (Exception e) {
        LOG.error("Properties can't be loaded.", e);
      }
    } finally {
      lock.unlock();
    }
  }

  public Map<String, String> getProperties() {
    if (loader.needsReload()) {
      reload();
    }

    lock.lock();
    try {
      return properties;
    } finally {
      lock.unlock();
    }
  }

  public InstallationProperties fallbackTo(final InstallationProperties fallback) {
    fallback.getProperties();
    final AtomicBoolean firstReload = new AtomicBoolean(false);
    final Loader fallbackLoader = new Loader() {
      @Override
      public boolean needsReload() {
        return InstallationProperties.this.loader.needsReload() || fallback.loader.needsReload() || firstReload
            .compareAndSet(false, true);
      }

      @Override
      public Map<String, String> load() {
        final Map<String, String> p = new UnifiedMap<String, String>();
        try {
          p.putAll(fallback.loader.load());
        } catch (Exception e) {
          LOG.warn("Can't load properties.", e);
        }
        try {
          p.putAll(InstallationProperties.this.loader.load());
        } catch (Exception e) {
          LOG.warn("Can't load properties.", e);
        }
        return p;
      }

      @Override
      public ChangeWatcher.Watch createWatch(String dependentConfigFilePath) {
        return ChangeWatcher.or(fallback.createWatch(dependentConfigFilePath), InstallationProperties.this
            .createWatch(dependentConfigFilePath));
      }
    };
    return new InstallationProperties(fallbackLoader, this.defaults);
  }

  public static InstallationProperties loadSpmSenderInstallationProperties(Configuration config) {
    final File dir = new File(config.getPropertiesDir());
    final File spmProperties = new File(dir, "agent.properties");
    final List<File> propertyFiles = Lists.newArrayList();
    final List<File> legacyPropertyFiles = Lists.newArrayList();
    final File[] files = dir.listFiles();
    if (files != null) {
      Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
      for (final File file : files) {
        if (file.getName().startsWith("spm-setup") && file.getName().endsWith(".properties")) {
          legacyPropertyFiles.add(file);
          break;
        }
      }
    }
    if (legacyPropertyFiles.isEmpty() || spmProperties.exists()) {
      propertyFiles.add(spmProperties);
      if (!spmProperties.exists()) {
        LOG.warn(
            "SPM Sender property file (" + spmProperties + ") not exists, default properties will be used instead.");
      }
    } else {
      propertyFiles.addAll(legacyPropertyFiles);
    }
    final Map<String, String> defaults = new UnifiedMap<String, String>();
    defaults.put("server_base_url", config.getDefaultReceiverUrl());
    defaults.put("metrics_endpoint", config.getDefaultMetricsEndpoint());
    defaults.put("tag_alias_endpoint", config.getDefaultTagAliasEndpoint());
    defaults.put("metainfo_endpoint", config.getDefaultMetainfoEndpoint());

    LOG.info("Using installation properties: " + propertyFiles + ".");
    return new InstallationProperties(new FileLoader(propertyFiles), defaults);
  }

  public static InstallationProperties fromFile(final File file) {
    return new InstallationProperties(new FileLoader(Lists.newArrayList(file)), Collections.EMPTY_MAP);
  }

  public static InstallationProperties fromResources(final List<String> resources) {
    return new InstallationProperties(new ClassPathLoader(resources), Collections.EMPTY_MAP);
  }

  public static InstallationProperties fromResource(final String resource) {
    return fromResources(Arrays.asList(resource));
  }

  public static InstallationProperties staticProperties(Map<String, String> props) {
    return new InstallationProperties(new StaticLoader(props), Collections.EMPTY_MAP);
  }
}
