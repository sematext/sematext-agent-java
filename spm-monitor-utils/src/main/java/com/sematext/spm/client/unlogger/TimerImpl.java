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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.unlogger.LoggerContext.Timer;

public final class TimerImpl implements Timer {

  private static final Log LOG = LogFactory.getLog(TimerImpl.class);

  private Long beginTotalMs;
  private Long beginTotalNs;

  private Long endTotalMs;
  private Long endTotalNs;

  private Long beginCpuNs;
  private Long endCpuNs;

  private long childTotalNs = 0;
  private long childCpuNs = 0;

  private final TimerImpl parent;

  public TimerImpl() {
    this(null);
  }

  private TimerImpl(TimerImpl parent) {
    this.parent = parent;
  }

  @Override
  public void begin() {
    beginTotalMs = System.currentTimeMillis();
    beginTotalNs = System.nanoTime();

    beginCpuNs = CpuTimeProvider.getCpuTime();
    if (beginCpuNs == -1) {
      beginCpuNs = beginTotalNs;
    }
  }

  @Override
  public void end() {
    if (endTotalNs != null) {
      LOG.warn("Timer end called multiple times!!!!!");
      return;
    }

    endTotalMs = System.currentTimeMillis();
    // So, our monitored application is more IO-bound rather then CPU
    // so we can switch to CPU time when needed
    // and did not forget about UseLinuxPosixThreadCPUClocks
    // threadMXBean.getCurrentThreadCpuTime();
    endTotalNs = System.nanoTime();

    endCpuNs = CpuTimeProvider.getCpuTime();
    if (endCpuNs == -1) {
      endCpuNs = endTotalNs;
    }

    if (parent != null) {
      parent.registerChildTime(getDurationNs(Measure.TOTAL), getDurationNs(Measure.CPU));
    }
  }

  @Override
  public Timer createChild() {
    return new TimerImpl(this);
  }

  protected void registerChildTime(Long durationTotalNs, Long durationCpuNs) {
    if (durationTotalNs != null && durationCpuNs != null) {
      childTotalNs += durationTotalNs;
      childCpuNs += durationCpuNs;
    }
  }

  @Override
  public Long getStartTimeMs() {
    return beginTotalMs;
  }

  @Override
  public Long getEndTimeMs() {
    return endTotalMs;
  }

  @Override
  public Long getDurationNs(Measure measure) {
    switch (measure) {
      case TOTAL:
        return duration(beginTotalNs, endTotalNs);
      case CPU:
        return duration(beginCpuNs, endCpuNs);
      case OWN_TOTAL:
        Long totalDuration = duration(beginTotalNs, endTotalNs);
        return totalDuration == null ? null : totalDuration - childTotalNs;
      case OWN_CPU:
        Long cpuDuration = duration(beginCpuNs, endCpuNs);
        return cpuDuration == null ? null : cpuDuration - childCpuNs;

      default:
        throw new IllegalStateException();
    }
  }

  private static Long duration(Long begin, Long end) {
    return begin == null || end == null ? null : end - begin;
  }

  @Override
  public void clear() {
    beginTotalMs = beginTotalNs = null;
    endTotalMs = endTotalNs = null;
    childTotalNs = 0;
    childCpuNs = 0;
  }

  public static final class CpuTimeProvider {

    private static ThreadMXBean provider;

    private CpuTimeProvider() {
      // It's utility class.
    }

    static {
      ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
      provider = mxBean.isThreadCpuTimeSupported() && mxBean.isThreadCpuTimeEnabled() ? mxBean : null;
    }

    public static long getCpuTime() {
      // Return -1, as standart error code for ThreadMXBean#getCurrentThreadCpuTime
      try {
        //It seems, on PH box we have a huge overhead from that,
        //add that feature configurable, and only for "heavy methods"
        return -1;
        // return (provider != null) ? provider.getCurrentThreadCpuTime() : -1;
      } catch (Exception e) {
        // Almost impossible, but JDK can throw exception in some cases.
        LOG.error("Can't get CPU time", e);
        return -1;
      }
    }
  }
}
