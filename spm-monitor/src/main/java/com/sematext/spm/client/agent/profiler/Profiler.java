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
package com.sematext.spm.client.agent.profiler;

import org.eclipse.collections.impl.list.mutable.FastList;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.agent.profiler.cpu.AllThreadsProfileSnapshot;
import com.sematext.spm.client.agent.profiler.cpu.ThreadsCPUProfiler;
import com.sematext.spm.client.command.SimpleCancellable;

public final class Profiler {

  private static final Log LOG = LogFactory.getLog(Profiler.class);

  private static final String AGENT_CLASSES_PACKAGE = "com.sematext.spm.client";

  private long duration;
  private TimeUnit durationUnit;
  private long sampleInterval;
  private TimeUnit sampleIntervalUnit;
  private boolean excludeAgentClasses;
  private ThreadMXBean threadMXBean;
  private List<GarbageCollectorMXBean> gcMXBeans = Collections.emptyList();

  public static interface Consumer {
    void consume(AllThreadsProfileSnapshot callTree, Throwable e);
  }

  private Profiler() {
  }

  public AllThreadsProfileSnapshot profile(SimpleCancellable cancellable) {
    final long deadlineNs = System.nanoTime() + durationUnit.toNanos(duration);

    final ThreadsCPUProfiler profile = new ThreadsCPUProfiler(threadMXBean, gcMXBeans, excludeAgentClasses);

    profile.preStart();
    while (System.nanoTime() < deadlineNs && !cancellable.isCancelled()) {
      profile.sample();

      try {
        sampleIntervalUnit.sleep(sampleInterval);
      } catch (InterruptedException e) {
        LOG.warn("Profiling thread interrupted, stopping.", e);
        break;
      }
    }

    cancellable.done();

    return profile.mkAllThreadsSnapshot();
  }

  public void profile(final ExecutorService service, final SimpleCancellable cancellable, final Consumer consumer) {
    service.submit(new Runnable() {
      @Override
      public void run() {
        try {
          consumer.consume(profile(cancellable), null);
        } catch (Exception e) {
          consumer.consume(null, e);
        }
      }
    });
  }

  static StackTraceElement[] processStackTrace(boolean excludeAgentClasses, StackTraceElement[] elements) {
    if (!excludeAgentClasses) {
      return elements;
    }

    final List<StackTraceElement> filteredElements = new FastList<StackTraceElement>();
    for (StackTraceElement element : elements) {
      if (element.getClassName().equals(Profiler.class.getName())) {
        return new StackTraceElement[0];
      }
      if (!element.getClassName().startsWith(AGENT_CLASSES_PACKAGE)) {
        filteredElements.add(element);
      }
    }
    return filteredElements.toArray(new StackTraceElement[filteredElements.size()]);
  }

  public static class Builder {
    private Profiler profiler = new Profiler();

    private Builder() {
    }

    public Builder duration(long duration, TimeUnit unit) {
      profiler.duration = duration;
      profiler.durationUnit = unit;
      return this;
    }

    public Builder sampleInterval(long interval, TimeUnit unit) {
      profiler.sampleInterval = interval;
      profiler.sampleIntervalUnit = unit;
      return this;
    }

    public Builder excludeAgentClasses(boolean exclude) {
      profiler.excludeAgentClasses = exclude;
      return this;
    }

    public Builder threadMXBean(ThreadMXBean threadMXBean) {
      profiler.threadMXBean = threadMXBean;
      return this;
    }

    public Builder gcMXBeans(List<GarbageCollectorMXBean> gcMXBeans) {
      profiler.gcMXBeans = gcMXBeans;
      return this;
    }

    public Profiler build() {
      return profiler;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

}
