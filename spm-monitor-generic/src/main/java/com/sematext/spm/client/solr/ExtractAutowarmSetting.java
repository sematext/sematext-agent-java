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

public class ExtractAutowarmSetting implements CalculationFunction {
  private static final Long ZERO = new Long(0);

  @Override
  public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    throw new UnsupportedOperationException("Can't be used in tag context");
  }

  @Override
  public Number calculateAttribute(Map<String, Object> metrics, Object... params) {
    String autowarm = (String) metrics.get("description");
    if (autowarm.indexOf("autowarmCount=") == -1) {
      return ZERO;
    }
    autowarm = autowarm.substring(autowarm.indexOf("autowarmCount=") + "autowarmCount=".length());
    if (autowarm.indexOf(",") != -1) {
      autowarm = autowarm.substring(0, autowarm.indexOf(","));
    }

    if (autowarm.contains("%")) {
      autowarm = autowarm.substring(0, autowarm.indexOf("%"));
    }

    return Long.valueOf(autowarm.trim());
  }
}
