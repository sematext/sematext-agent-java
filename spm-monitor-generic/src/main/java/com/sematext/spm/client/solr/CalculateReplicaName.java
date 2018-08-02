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

public class CalculateReplicaName implements CalculationFunction {
  @Override
  public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    throw new UnsupportedOperationException("Can't be used in attribute context");
  }

  @Override
  public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    String coreName = objectNameTags.get("core");

    // take into account old Solr setups, not every Solr setup is a cloud (1.* versions for example)
    if (coreName.matches("(.)*_(.)*_replica(.)*")) {
      return coreName.substring(coreName.lastIndexOf("_") + 1);
    } else {
      return null;
    }
  }
}
