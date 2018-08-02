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
package com.sematext.spm.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class LogFactoryTest {
  DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

  class StubLogWriter implements LogWriter<String> {
    StringBuffer output = new StringBuffer();
    Throwable throwable;

    @Override
    public void write(String logLine, Throwable throwable) {
      output.append(logLine);
      this.throwable = throwable;
    }

    @Override
    public void write(String logLine) {
      output.append(logLine);
    }
  }

  ;

  StubLogWriter writer;

  @Before
  public void setup() {
    writer = new StubLogWriter();
    Thread.currentThread().setName("test");
  }

  @Test
  public void testSimpleLogImplInfo() throws ParseException {
    LogFactory.init(writer);
    Log log = new LogFactory.SimpleLogImpl("test");
    log.info("this is message");

    String line = writer.output.toString();
    assertTrue(line.endsWith(" INFO [test] test - this is message"));
    // make sure the line starts with the date and time.
    dateFormat.parse(String.valueOf(line.subSequence(0, 24)));
    assertNull(writer.throwable);
  }

  @Test
  public void testSimpleLogImplInfoException() throws ParseException {
    LogFactory.init(writer);
    Log log = new LogFactory.SimpleLogImpl("test");
    Exception err = new Exception("bla");
    log.info("this is message", err);

    String line = writer.output.toString();
    assertTrue(line.endsWith(" INFO [test] test - this is message"));
    // make sure the line starts with the date and time.
    dateFormat.parse(String.valueOf(line.subSequence(0, 24)));
    assertEquals(err, writer.throwable);
  }

  @Test
  public void testSimpleLogImplError() throws ParseException {
    LogFactory.init(writer);
    Log log = new LogFactory.SimpleLogImpl("test");
    log.error("this is message");

    String line = writer.output.toString();
    assertTrue(line.endsWith(" ERROR [test] test - this is message"));
    // make sure the line starts with the date and time.
    dateFormat.parse(String.valueOf(line.subSequence(0, 24)));
    assertNull(writer.throwable);
  }

  @Test
  public void testSimpleLogImplErrorException() throws ParseException {
    LogFactory.init(writer);
    Log log = new LogFactory.SimpleLogImpl("test");
    Exception err = new Exception("bla");
    log.error("this is message", err);

    String line = writer.output.toString();
    assertTrue(line.endsWith(" ERROR [test] test - this is message"));
    // make sure the line starts with the date and time.
    dateFormat.parse(String.valueOf(line.subSequence(0, 24)));
    assertEquals(err, writer.throwable);
  }

  @Test
  public void testSimpleLogImplWarn() throws ParseException {
    LogFactory.init(writer);
    Log log = new LogFactory.SimpleLogImpl("test");
    log.warn("this is message");

    String line = writer.output.toString();
    assertTrue(line.endsWith(" WARN [test] test - this is message"));
    // make sure the line starts with the date and time.
    dateFormat.parse(String.valueOf(line.subSequence(0, 24)));
    assertNull(writer.throwable);
  }

  @Test
  public void testSimpleLogImplWarnError() throws ParseException {
    LogFactory.init(writer);
    Log log = new LogFactory.SimpleLogImpl("test");
    Exception err = new Exception("bla");
    log.warn("this is message", err);

    String line = writer.output.toString();
    assertTrue(line.endsWith(" WARN [test] test - this is message"));
    // make sure the line starts with the date and time.
    dateFormat.parse(String.valueOf(line.subSequence(0, 24)));
    assertEquals(err, writer.throwable);
  }
}
