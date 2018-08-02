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

package com.sematext.spm.client.functions;

import java.io.File;
import java.util.Map;

import com.sematext.spm.client.observation.CalculationFunction;

/**
 * Count the files present under directory pointed by specified metric. Not recursive
 */
public class CountFiles implements CalculationFunction {
  @Override public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    if (params != null && params.length == 1) {
      String metricName = params[0].toString();
      String dir = (String) metrics.get(metricName);
      int fileCount = 0;
      File f = new File(dir);
      if (f.exists()) {
        File files[] = f.listFiles();
        if (files != null) {
          fileCount = files.length;
        }
      }
      return fileCount;
    } else {
      throw new IllegalArgumentException("Missing metric name param");
    }
  }

  @Override public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    return null;
  }
}
