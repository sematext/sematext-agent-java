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

import org.eclipse.collections.impl.list.mutable.FastList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.util.IOUtils;

public final class SenderConfigSource {

  private static final Log LOG = LogFactory.getLog(SenderConfigSource.class);

  private final Iterable<File> files;
  private final String monitorType;
  private final String token;
  private final String jvmName;
  private final String confSubtype;

  public SenderConfigSource(Iterable<File> files, String monitorType, String token, String jvmName,
                            String confSubtype) {
    this.files = files;
    this.monitorType = monitorType;
    this.token = token;
    this.jvmName = jvmName;
    this.confSubtype = confSubtype;
  }

  public String getMonitorType() {
    return monitorType;
  }

  public Iterable<File> getFiles() {
    return files;
  }

  public String getToken() {
    return token;
  }

  public String getJvmName() {
    return jvmName;
  }

  public String getConfSubtype() {
    return confSubtype;
  }

  @Override
  public String toString() {
    return "SenderConfigSource{" +
        "files=" + files +
        ", monitorType='" + monitorType + '\'' +
        ", token='" + token + '\'' +
        ", jvmName='" + jvmName + '\'' +
        '}';
  }

  public Iterable<Properties> getProperties() throws IOException {
    final List<Properties> propertiesList = new FastList<Properties>();
    for (final File file : files) {
      final Properties props = new Properties();
      if (file.exists()) {
        FileInputStream is = null;
        try {
          is = new FileInputStream(file);
          props.load(is);
        } finally {
          IOUtils.closeQuietly(is);
        }
      }
      propertiesList.add(props);
    }
    return propertiesList;
  }

}
