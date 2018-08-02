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

package com.sematext.spm.client.solr;

import java.util.Map;

import com.sematext.spm.client.jmx.JmxExtractorUtil;
import com.sematext.spm.client.observation.CalculationFunction;

public class CalculateSegmentsCount implements CalculationFunction {
  @Override
  public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    throw new UnsupportedOperationException("Can't be used in tag context");
  }

  @Override
  public Number calculateAttribute(Map<String, Object> metrics, Object... params) {

    if (params != null && params.length == 1) {
      String metricName = params[0].toString();
      String reader = (String) metrics.get(metricName);

      if (reader.indexOf("segments=") != -1) {
        // solr 3.x and earlier
        return JmxExtractorUtil.extractFromField(reader, "segments", -1L);
      } else {
        // solr 4.0 and up
        // format like :
        //   StandardDirectoryReader(segments_6:11 _0(4.0):C2 _1(4.0):C11 _2(4.0):C2 _3(4.0):C4 _4(4.0):C5)
        //   StandardDirectoryReader(segments_2:3 _0(4.0):C2)
        return Long.valueOf(reader.split(" ").length);
      }
    } else {
      throw new IllegalArgumentException("Missing metric name in params");
    }
  }
}
