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
package com.sematext.spm.client.agent.profiler.cpu;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.agent.profiler.CallTree;

public class ThreadsCPUProfiler {
  private static final String AGENT_CLASSES_PKG_PREFIX = "com.sematext.spm.client";

  private final Map<Long, ThreadProfile> profiles = new UnifiedMap<Long, ThreadProfile>();
  private final SafeThreadMXBean threadMXBean;
  private final List<GarbageCollectorMXBean> gcMXBeans;
  private final boolean excludeAgentClasses;
  private long baselineGCTIme;

  public ThreadsCPUProfiler(ThreadMXBean threadMXBean, List<GarbageCollectorMXBean> gcMXBeans,
                            boolean excludeAgentClasses) {
    this.threadMXBean = SafeThreadMXBean.make(threadMXBean);
    this.excludeAgentClasses = excludeAgentClasses;
    this.gcMXBeans = gcMXBeans;
  }

  public void sample() {
    for (ThreadInfo info : threadMXBean.dumpAllThreads(false, false)) {

      boolean skipThreadInfo = false;

      if ((info.getThreadId() == Thread.currentThread().getId()) && excludeAgentClasses) {
        skipThreadInfo = true;
      }

      if (excludeAgentClasses && info.getStackTrace() != null) {
        for (StackTraceElement el : info.getStackTrace()) {
          if (el.getClassName() != null && el.getClassName().startsWith(AGENT_CLASSES_PKG_PREFIX)) {
            skipThreadInfo = true;
            break;
          }
        }
      }

      if (skipThreadInfo) {
        continue;
      }

      ThreadProfile profile = profiles.get(info.getThreadId());

      long timestamp = System.nanoTime();
      long cpuTime = threadMXBean.getThreadCpuTime(info.getThreadId());
      long userCPUTime = threadMXBean.getThreadUserTime(info.getThreadId());
      if (profile == null) {
        profile = new ThreadProfile(timestamp, cpuTime, userCPUTime);
        profiles.put(info.getThreadId(), profile);
      }

      profile.update(info, cpuTime, userCPUTime, timestamp);
    }
  }

  private long getGCCollectionTime() {
    long time = 0;
    for (GarbageCollectorMXBean bean : gcMXBeans) {
      time += bean.getCollectionTime();
    }
    return time;
  }

  public void preStart() {
    baselineGCTIme = getGCCollectionTime();
  }

  public AllThreadsProfileSnapshot mkAllThreadsSnapshot() {
    long cpuTime = 0, userCPUTime = 0, waitedTime = 0, blockedTime = 0, samples = 0, ts0 = 0, baselineTs = 0;

    if (!profiles.isEmpty()) {
      ThreadProfile first = profiles.values().iterator().next();
      ts0 = first.getTimestamp0();
      baselineTs = first.getBaselineTimestamp();
    }

    List<CallTree> trees = new FastList<CallTree>();
    for (ThreadProfile profile : profiles.values()) {
      cpuTime += profile.getTotalCPUTimeMs();
      userCPUTime += profile.getTotalUserCPUTimeMs();
      waitedTime += profile.getWaitedTime();
      blockedTime += profile.getBlockedTime();
      samples += profile.getSamples();

      ts0 = Math.max(ts0, profile.getTimestamp0());
      baselineTs = Math.min(baselineTs, profile.getBaselineTimestamp());

      trees.add(profile.getCallTree());
    }

    return AllThreadsProfileSnapshot.builder()
        .time(TimeUnit.NANOSECONDS.toMillis(ts0 - baselineTs))
        .cpuTime(cpuTime)
        .userCpuTime(userCPUTime)
        .samples(samples)
        .waitedTime(waitedTime)
        .blockedTime(blockedTime)
        .gcTime(getGCCollectionTime() - baselineGCTIme)
        .cpuTimeSupported(threadMXBean.isCpuTimeSupported())
        .tree(CallTree.merge(trees))
        .build();
  }

  public String dump() {
    StringBuilder dump = new StringBuilder();
    dump.append("==============================").append("\n");
    dump.append("DUMP (Merged)").append("\n");
    dump.append("==============================").append("\n");

    AllThreadsProfileSnapshot snapshot = mkAllThreadsSnapshot();

    double cpuPerc = ((double) snapshot.getCpuTime() / snapshot.getTime()) * 100.0, userCPUPerc =
        ((double) snapshot.getUserCpuTime() / snapshot.getTime()) * 100.0;

    dump.append("[Wait Time: ").append(snapshot.getWaitedTime()).append("ms, Block time: ")
        .append(snapshot.getBlockedTime()).append("ms")
        .append(", CPU: ").append(String.format("%2.2f", cpuPerc)).append("% (").append(snapshot.getCpuTime())
        .append("ms)")
        .append(", User CPU:").append(String.format("%2.2f", userCPUPerc)).append("% (")
        .append(snapshot.getUserCpuTime()).append("ms)")
        .append(", Time:").append(snapshot.getTime()).append("ms")
        .append("]\n");

    dump.append(snapshot.getTree().dump());

    return dump.toString();
  }
}
