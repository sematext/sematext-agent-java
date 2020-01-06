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
package com.sematext.spm.client.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class AgentArgsParserTest {
  @Test
  public void testParseConfigFileName() {
    Map<String, String> params = AgentArgsParser.parseConfigFileName("/opt/spm/spm-monitor/conf/spm-monitor-config" +
        "-dddddddd-5555-4444-bbbb-ffffffffffff-solr_stack_my_solr.1.l2ch2x0f0p0h7ykhorjlsf12s.properties");
    
    Assert.assertEquals("dddddddd-5555-4444-bbbb-ffffffffffff", params.get(AgentArgsParser.TOKEN_PARAM));
    Assert.assertEquals("solr_stack_my_solr.1.l2ch2x0f0p0h7ykhorjlsf12s", params.get(AgentArgsParser.JVM_NAME_PARAM));
    Assert.assertEquals("", params.get(AgentArgsParser.SUB_TYPE_PARAM));
    
   params = AgentArgsParser.parseConfigFileName("/opt/spm/spm-monitor/conf/spm-monitor-master-config" +
        "-dddddddd-5555-4444-bbbb-ffffffffffff-default.properties");
    
    Assert.assertEquals("dddddddd-5555-4444-bbbb-ffffffffffff", params.get(AgentArgsParser.TOKEN_PARAM));
    Assert.assertEquals("default", params.get(AgentArgsParser.JVM_NAME_PARAM));
    Assert.assertEquals("master", params.get(AgentArgsParser.SUB_TYPE_PARAM));
  }
}
