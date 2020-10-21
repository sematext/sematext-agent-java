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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sematext.spm.client.jmx.JmxExtractorUtil;
import com.sematext.spm.client.observation.CalculationFunction;

public class CalculateSegmentsCount implements CalculationFunction {
  private static final String SEGMENTS_COUNT_REGEXP = "( |\\()_{1}[0-9]+\\(";
  private static final Pattern SEGMENTS_COUNT_PATTERN = Pattern.compile(SEGMENTS_COUNT_REGEXP);
  
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
        
        // ExitableDirectoryReader(UninvertingDirectoryReader(Uninverting(_7(7.1.0):C23/14:delGen=1) Uninverting(_8(7.1.0):C14)))
        
        // solr 8.5.2
        // ExitableDirectoryReader(UninvertingDirectoryReader(Uninverting(_0(8.5.2):C3:[diagnostics={java.vendor=Oracle Corporation, os=Linux, java.version=1.8.0_151, java.vm.version=25.151-b12, lucene.version=8.5.2, os.arch=amd64, java.runtime.version=1.8.0_151-b12, source=flush, os.version=4.4.0-31-generic, timestamp=1603261473913}]:[attributes={Lucene50StoredFieldsFormat.mode=BEST_SPEED}]) Uninverting(_2(8.5.2):C4:[diagnostics={java.vendor=Oracle Corporation, os=Linux, java.version=1.8.0_151, java.vm.version=25.151-b12, lucene.version=8.5.2, os.arch=amd64, java.runtime.version=1.8.0_151-b12, source=flush, os.version=4.4.0-31-generic, timestamp=1603261550321}]:[attributes={Lucene50StoredFieldsFormat.mode=BEST_SPEED}])))
        
        Matcher matcher = SEGMENTS_COUNT_PATTERN.matcher(reader);
        int count = 0;
        while (matcher.find()) {
          count++;
        }
        return count;
      }
    } else {
      throw new IllegalArgumentException("Missing metric name in params");
    }
  }
}
