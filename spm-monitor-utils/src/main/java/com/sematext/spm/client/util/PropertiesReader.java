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
package com.sematext.spm.client.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class PropertiesReader {
  private PropertiesReader() {
  }

  public static Map<String, String> read(Properties properties) {
    final Map<String, String> props = new HashMap<String, String>();
    for (String key : properties.stringPropertyNames()) {
      props.put(key, properties.getProperty(key));
    }
    return props;
  }

  public static Map<String, String> tryRead(Class<?> klass, String path) {
    final InputStream is = klass.getResourceAsStream(path);
    if (is == null) {
      return Collections.emptyMap();
    }
    final Properties properties = new Properties();
    try {
      properties.load(is);
    } catch (IOException e) {
      /* */
    }
    return read(properties);
  }

  public static Map<String, String> tryRead(File file) {
    final Properties properties = new Properties();
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);
      properties.load(fis);
    } catch (IOException e) {
      /* */
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          /* */
        }
      }
    }
    return read(properties);
  }
}
