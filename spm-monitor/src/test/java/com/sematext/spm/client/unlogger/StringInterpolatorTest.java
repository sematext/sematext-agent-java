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

package com.sematext.spm.client.unlogger;

import static com.sematext.spm.client.util.StringInterpolator.interpolator;

import org.junit.Assert;
import org.junit.Test;

import com.sematext.spm.client.util.StringInterpolator;

public class StringInterpolatorTest {

  @Test
  public void simpleReplaceTest() {
    StringInterpolator stringInterpolator = interpolator("#").addParam("ix", "IX").addParam("iy", "IY");
    Assert.assertEquals("someIX someIY", stringInterpolator.interpolate("some#ix# some#iy#"));

  }

  @Test
  public void specialSymbolsReplaceTest() {
    StringInterpolator stringInterpolator = interpolator("#").addParam("thunkClassName", "thunk.class.Name$Ix")
        .addParam("adviceId", 17).addParam("methodName", "methodX");
    Assert
        .assertEquals(
            "{thunk.class.Name$Ix.logBefore(17, new com.sematext.spm.client.unlogger.MethodJoinPoint($class, \"methodX\", $sig), $0, $args);}",
            stringInterpolator
                .interpolate("{#thunkClassName#.logBefore(#adviceId#, new com.sematext.spm.client.unlogger.MethodJoinPoint($class, \"#methodName#\", $sig), $0, $args);}"));
  }

  @Test
  public void simpleBracketReplaceTest() {
    StringInterpolator stringInterpolator = interpolator("${", "}").addParam("ix", "IX").addParam("iy", "IY");
    Assert.assertEquals("someIX someIY", stringInterpolator.interpolate("some${ix} some${iy}"));

  }

}
