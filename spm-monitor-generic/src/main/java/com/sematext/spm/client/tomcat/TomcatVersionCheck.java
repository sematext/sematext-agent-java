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

package com.sematext.spm.client.tomcat;

import com.sematext.spm.client.jmx.JmxHelper;
import com.sematext.spm.client.jmx.JmxServiceContext;
import com.sematext.spm.client.observation.BaseVersionConditionCheck;

public class TomcatVersionCheck extends BaseVersionConditionCheck {

  private static final String TOMCAT_SERVER_OBJECT_NAME = "Catalina:type=Server";
  private static final String TOMCAT_VERSION_ATTRIBUTE_NAME = "serverNumber";

  @Override protected String readVersion() {
    Object version = JmxHelper.queryJmx(
        JmxServiceContext.getContext(getMonitorConfig().getMonitorPropertiesFile()),
        TOMCAT_SERVER_OBJECT_NAME, TOMCAT_VERSION_ATTRIBUTE_NAME);
    return version != null ? version.toString() : null;
  }
}
