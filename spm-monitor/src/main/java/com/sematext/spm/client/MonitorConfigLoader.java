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

import org.eclipse.collections.impl.list.mutable.FastList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Properties;

import com.sematext.spm.client.util.StringUtils;

/**
 * Prepares and loads MonitorConfig instance. Preparing means adjusting configuration file by replacing various
 * placeholders.
 */
public class MonitorConfigLoader {
  private static final Log LOG = LogFactory.getLog(MonitorConfigLoader.class);

  // private static final String JAVA_DEFAULTS_64M = "\"-server -Xmx64m -Xms64m -Xss256k\"";
  private static final String JAVA_DEFAULTS_192M = "\"-server -Xmx192m -Xms64m -Xss256k\"";
  // private static final String JAVA_DEFAULTS_256M = "\"-server -Xmx256m -Xms64m -Xss256k\"";
  // private static final String JAVA_DEFAULTS_384M = "\"-server -Xmx384m -Xms64m -Xss256k\"";

  private String monitorType;
  private String token;
  private String jvmName;
  private String confSubtype;

  public MonitorConfigLoader(String monitorType, String token, String jvmName, String confSubtype) {
    this.monitorType = monitorType;
    this.token = token;
    this.jvmName = jvmName;
    this.confSubtype = confSubtype;
  }

  public MonitorConfig loadConfig(File monitorProperties, DataFormat format, Integer processOrdinal)
      throws ConfigurationFailedException {
    // load the config file, replace any placeholders found in there, save the file
    LOG.info("Loading configuration for config file: " + monitorProperties);

    try {
      addMissingProperties(monitorProperties);
    } catch (Throwable thr) {
      // handle the error, don't propagate
      LOG.error("Can't add missing properties", thr);
    }

    String confSubType = MonitorUtil.extractConfSubtype(monitorProperties.getName());
    File propsFile = MonitorUtil.fetchSpmMonitorPropertiesFileObject(token, jvmName, confSubType);
    return new MonitorConfig(token, propsFile, format, processOrdinal);
  }

  private void writeProperties(File propertyFile, List<Property> properties) {
    if (properties.size() == 0) {
      return;
    }

    PrintWriter out = null;
    try {
      out = new PrintWriter(new BufferedWriter(new FileWriter(propertyFile, true)));
      out.write(StringUtils.LINE_SEPARATOR);
      for (Property property : properties) {
        property.write(out);
        LOG.info("Added missed property: " + property);
        out.write(StringUtils.LINE_SEPARATOR);
      }
    } catch (IOException e) {
      LOG.error("Can't add missed properties to file: " + propertyFile, e);
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

  protected void addMissingProperties(File monitorProperties) throws IOException {
    if (monitorProperties != null && monitorProperties.exists()) {
      Properties props = new Properties();
      props.load(new FileInputStream(monitorProperties));

      String tags = props.getProperty("SPM_MONITOR_TAGS");

      List<Property> missedProperties = new FastList<Property>();

      if (tags == null) {
        missedProperties
            .add(new Property("SPM_MONITOR_TAGS", "", "add tags if you want to use them, example: SPM_MONITOR_TAGS=env:foo, role:bar"));
      }

      String suppressTags = props.getProperty("SPM_SUPPRESS_TAGS");
      if (suppressTags == null) {
        missedProperties
            .add(new Property("SPM_SUPPRESS_TAGS", "", "add tags which should be excluded, example: SPM_SUPPRESS_TAGS=project:baz, node:qux"));
      }

      String javaDefaults = props.getProperty("JAVA_DEFAULTS");
      if (javaDefaults == null) {
        missedProperties.add(new Property("JAVA_DEFAULTS", JAVA_DEFAULTS_192M, null));
      }

      writeProperties(monitorProperties, missedProperties);
    }

  }

  private static class Property {
    private String name;
    private String value;
    private boolean commented;
    private String comment;

    private Property(String name, String value) {
      this.name = name;
      this.value = value;
    }

    private Property(String name, String value, String comment) {
      this.name = name;
      this.value = value;
      this.comment = comment;
    }

    public void setCommented(boolean commented) {
      this.commented = commented;
    }

    public void setComment(String comment) {
      this.comment = comment;
    }

    public void write(PrintWriter writer) {
      if (comment != null && comment.length() > 0) {
        writer.write("# " + comment);
      }
      writer.write(StringUtils.LINE_SEPARATOR);
      writer.write(name + "=" + value);
      writer.write(StringUtils.LINE_SEPARATOR);
    }

    @Override
    public String toString() {
      return "Property{" +
          "name='" + name + '\'' +
          ", value='" + value + '\'' +
          ", commented=" + commented +
          ", comment='" + comment + '\'' +
          '}';
    }
  }
}
