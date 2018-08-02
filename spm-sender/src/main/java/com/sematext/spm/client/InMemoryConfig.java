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
import java.io.InputStream;
import java.util.Properties;

public abstract class InMemoryConfig {
  private static final Log LOG = LogFactory.getLog(InMemoryConfig.class);

  protected File monitorPropertiesFile;
  private long monitorPropertiesFileLastModified = -1;
  private Properties loadedMonitorProperties;

  public InMemoryConfig(File monitorPropertiesFile) throws ConfigurationFailedException {
    this.monitorPropertiesFile = monitorPropertiesFile;

    if (monitorPropertiesFile != null) {
      loadConfig();
    }
  }

  protected void loadConfig() throws ConfigurationFailedException {
    InputStream is = null;
    try {
      long currentLastModified = monitorPropertiesFile.lastModified();
      if (currentLastModified != monitorPropertiesFileLastModified) {
        LOG.info("Loading config file: " + monitorPropertiesFile);
        is = new FileInputStream(monitorPropertiesFile);
        loadedMonitorProperties = new Properties();
        loadedMonitorProperties.load(is);
        monitorPropertiesFileLastModified = currentLastModified;
      }

      LOG.info("Monitor properties : " + loadedMonitorProperties);

      read(loadedMonitorProperties);
    } catch (IOException ioe) {
      throw new ConfigurationFailedException(
          "Error while reading " + monitorPropertiesFile + " " + ioe.getMessage(), ioe);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          LOG.warn("Error while closing file input stream for " + monitorPropertiesFile + " " + e.getMessage(), e);
        }
      }
    }
  }

  public void reloadConfig() throws ConfigurationFailedException {
    loadConfig();
  }

  protected abstract void readFields(Properties properties) throws ConfigurationFailedException;

  protected void read(Properties monitorProperties) throws ConfigurationFailedException {
    readFields(monitorProperties);
  }

  @SuppressWarnings("unchecked")
  public static <T> T createInstance(String className) throws Exception {
    Class c = Class.forName(className);
    return (T) c.newInstance();
  }

  public File getMonitorPropertiesFile() {
    return monitorPropertiesFile;
  }
}
