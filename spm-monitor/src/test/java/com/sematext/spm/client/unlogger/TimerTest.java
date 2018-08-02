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

import org.junit.Test;

import java.math.BigDecimal;

import com.sematext.spm.client.unlogger.LoggerContext.Timer;
import com.sematext.spm.client.unlogger.LoggerContext.Timer.Measure;

public class TimerTest {

  @Test
  public void basicIOTest() throws Exception {
    Timer timer = new TimerImpl();
    timer.begin();
    io(timer, 1000);
    timer.end();
    dump(timer);
  }

  @Test
  public void basicCpuTest() throws Exception {
    Timer timer = new TimerImpl();
    timer.begin();
    cpu(timer, 2157);
    timer.end();
    dump(timer);
  }

  @Test
  public void basicExtCpuIntIoTest() throws Exception {
    Timer timer = new TimerImpl();
    timer.begin();
    cpu(timer, 2157);
    Timer child = timer.createChild();
    child.begin();
    io(child, 1000);
    child.end();
    timer.end();
    System.out.println("ExtCpuIntIoTest -> Parent:");
    dump(timer);
    System.out.println("ExtCpuIntIoTest -> Child:");
    dump(child);
  }

  @Test
  public void basicExtIoIntCpuTest() throws Exception {
    Timer timer = new TimerImpl();
    timer.begin();
    io(timer, 1000);
    Timer child = timer.createChild();
    child.begin();
    cpu(child, 21578);
    child.end();
    timer.end();
    System.out.println("ExtIoIntCpuTest -> Parent:");
    dump(timer);
    System.out.println("ExtIoIntCpuTest -> Child:");
    dump(child);
  }

  private static void io(Timer timer, long delayMs) throws Exception {
    Thread.currentThread().sleep(1000);
  }

  private static volatile long var;

  private static void cpu(Timer timer, long numOfIterations) {
    int ix = 1;
    BigDecimal somethingCPU = new BigDecimal(ix);
    while (ix++ < numOfIterations) {
      somethingCPU = somethingCPU.multiply(new BigDecimal(ix));
    }
    var = somethingCPU.toString().length();
  }

  private static void dump(Timer timer) {
    System.out.println("TOTAL -> " + ((double) timer.getDurationNs(Measure.TOTAL)) / 1000000000);
    System.out.println("СPU -> " + ((double) timer.getDurationNs(Measure.CPU)) / 1000000000);
    System.out.println("OWN_TOTAL -> " + ((double) timer.getDurationNs(Measure.OWN_TOTAL)) / 1000000000);
    System.out.println("OWN_CPU -> " + ((double) timer.getDurationNs(Measure.OWN_CPU)) / 1000000000);
  }

}
