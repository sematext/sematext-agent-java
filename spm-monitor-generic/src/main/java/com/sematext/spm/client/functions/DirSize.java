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
import com.sematext.spm.client.util.FileUtil;

/**
 * Calculate the size of the directory in bytes, the specified metricName points to.
 */
public class DirSize implements CalculationFunction {
  @Override public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    if (params != null && params.length == 1) {
      String metricName = params[0].toString();
      String indexDir = (String) metrics.get(metricName);
      File f = new File(indexDir);
      if (f.exists()) {
        return FileUtil.sizeOfDirectory(f);
      } else {
        return 0;
      }
    } else {
      throw new IllegalArgumentException("Missing metric name in params");
    }
  }

  @Override public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    throw new UnsupportedOperationException("Can't be used in tag context");
  }
}
