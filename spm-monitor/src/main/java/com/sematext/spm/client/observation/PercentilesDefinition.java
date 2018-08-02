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
package com.sematext.spm.client.observation;

import java.util.Map;

import com.sematext.spm.client.util.PercentileUtils;

public class PercentilesDefinition {

  private String baseMetricName;
  private Map<Long, String> pctlsToNames;

  /**
   * @param definition
   * @param attribute        either this or derivedAttribute has to be passed as a param
   * @param derivedAttribute
   */
  public PercentilesDefinition(String definition, AttributeObservation<?> attribute,
                               DerivedAttribute derivedAttribute) {
    this(definition, attribute != null ? attribute.getFinalName() : derivedAttribute.getName());
  }

  public PercentilesDefinition(String definition, String baseMetricName) {
    if (baseMetricName == null || "".equals(baseMetricName.trim())) {
      throw new IllegalArgumentException("Base metric name missing or empty");
    }
    this.baseMetricName = baseMetricName;
    this.pctlsToNames = PercentileUtils.getPctlToNameMap(definition, baseMetricName);
  }

  public String getBaseMetricName() {
    return baseMetricName;
  }

  public Map<Long, String> getPctlsToNames() {
    return pctlsToNames;
  }
}
