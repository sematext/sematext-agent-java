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

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sematext.spm.client.unlogger.LogLine.Key;

public class LogLineTest {

  private static LogLine make() {
    LogLine logLine = LogLine.Factory
        .make(new LogLine.Key[] { LogLine.Key.THREAD_NAME, LogLine.Key.CLASS_SIMPLE_NAME }).make("section");
    return logLine;
  }

  @Test
  public void iteratorTest() {
    LogLine logLine = make();
    logLine.put(Key.THREAD_NAME, "ix");
    logLine.put(Key.CLASS_SIMPLE_NAME, "iy");

    Assert.assertEquals(list("ix", "iy"), copy(logLine));
  }

  @Test
  public void iteratorTestEmpty() {
    LogLine logLine = make();

    Assert.assertEquals(list(), copy(logLine));
  }

  @Test
  public void iteratorTestFirst() {
    LogLine logLine = make();
    logLine.put(Key.THREAD_NAME, "ix");

    Assert.assertEquals(list("ix"), copy(logLine));
  }

  @Test
  public void iteratorTestSecond() {
    LogLine logLine = make();
    logLine.put(Key.THREAD_NAME, "iy");

    Assert.assertEquals(list("iy"), copy(logLine));
  }

  private static List<Object> copy(LogLine logLine) {
    List<Object> ret = new ArrayList<Object>();
    for (Object val : logLine) {
      ret.add(val);
    }
    return ret;
  }

  private static List<Object> list(Object... objects) {
    return Arrays.asList(objects);
  }
}
