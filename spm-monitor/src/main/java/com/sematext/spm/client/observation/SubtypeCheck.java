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

public class SubtypeCheck extends BeanConditionCheck {
  @Override
  protected String getConditionValue() {
    return getMonitorConfig().getSubType();
  }

  @Override
  protected boolean clauseSatisfies(String conditionValue, String expectedValue) {
    // switched initial logic to assume check passes even when sub-type is not defined (makes it possible to simplify
    // installation to not require subType when it may be difficult to provide it during agent setup)
    return noSubtype(conditionValue) || conditionValue.equals(expectedValue);
  }

  private boolean noSubtype(String conditionValue) {
    return conditionValue == null || "".equals(conditionValue.trim());
  }
}
