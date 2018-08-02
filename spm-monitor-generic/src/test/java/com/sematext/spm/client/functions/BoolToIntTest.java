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

package com.sematext.spm.client.functions;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import java.util.Arrays;

public class BoolToIntTest {
  @Test
  public void testConvertBool() {
    BoolToInt func = new BoolToInt();

    assertEquals(1, func.calculateAttribute(ImmutableMap.of(
        "testField", (Object) true
    ), "testField"));

    assertEquals(1, func.calculateAttribute(ImmutableMap.of(
        "testField", (Object) "true"
    ), "testField"));

    assertEquals(0, func.calculateAttribute(ImmutableMap.of(
        "testField", (Object) false
    ), "testField"));

    assertEquals(0, func.calculateAttribute(ImmutableMap.of(
        "testField", (Object) "false"
    ), "testField"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowException() {
    BoolToInt func = new BoolToInt();

    func.calculateAttribute(ImmutableMap.of(
        "testField", (Object) Arrays.asList("123")
    ), "testField");
  }
}
