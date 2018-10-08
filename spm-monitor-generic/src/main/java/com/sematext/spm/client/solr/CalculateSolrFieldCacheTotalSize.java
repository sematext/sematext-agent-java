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

import com.sematext.spm.client.observation.CalculationFunction;

public class CalculateSolrFieldCacheTotalSize implements CalculationFunction {
  private static long KB = 1024;
  private static long MB = 1024 * KB;
  private static long GB = 1024 * MB;
  private static long TB = 1024 * GB;
  private static long PB = 1024 * TB;
  
  @Override
  public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    throw new UnsupportedOperationException("Can't be used in tag context");
  }

  @Override
  public Number calculateAttribute(Map<String, Object> metrics, Object... params) {
    String size = (String) metrics.get("total_size");
    
    if (size == null) return null;
    
    String [] split = size.split(" "); 
    double value = Double.valueOf(split[0].trim());
    String unit = split[1].trim();
    
    if (unit.equalsIgnoreCase("kb")) {
      return (long) (KB * value);
    } else if (unit.equalsIgnoreCase("mb")) {
      return (long) (MB * value);
    } else if (unit.equalsIgnoreCase("gb")) {
      return (long) (GB * value);
    } else if (unit.equalsIgnoreCase("tb")) {
      return (long) (TB * value);
    } else if (unit.equalsIgnoreCase("pb")) {
      return (long) (PB * value);
    } else {
      return value;
    }
  }
}
