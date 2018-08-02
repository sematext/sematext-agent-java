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

import java.lang.management.ThreadInfo;
import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.agent.profiler.CallTree;

public class ThreadProfile {

  private final CallTree callTree = new CallTree();

  private long timestamp0;
  private long cpuTime0;
  private long userCPUTime0;
  private long waitedTime;
  private long blockedTime;
  private long samples;
  private final long baselineTimestamp;
  private final long baselineCPUTime;
  private final long baselineUserCPUTime;

  public ThreadProfile(long timestamp0, long cpuTime0, long userCPUTime0) {
    this.timestamp0 = timestamp0;
    this.cpuTime0 = cpuTime0;
    this.userCPUTime0 = userCPUTime0;
    this.baselineTimestamp = timestamp0;
    this.baselineCPUTime = cpuTime0;
    this.baselineUserCPUTime = userCPUTime0;
  }

  private static boolean isRunnable(ThreadInfo info) {
    return info.getThreadState() == Thread.State.RUNNABLE && !BlockingMethods.isThreadBlocked(info.getStackTrace());
  }

  public void update(ThreadInfo info, long cpuTime, long userCPUTime, long timestamp) {
    if (isRunnable(info)) {
      if (info.getStackTrace().length > 0) {
        samples++;
      }
      update(info.getStackTrace(), cpuTime, userCPUTime, timestamp);
    }
    cpuTime0 = cpuTime;
    userCPUTime0 = userCPUTime;
    timestamp0 = timestamp;
    waitedTime = info.getWaitedTime();
    blockedTime = info.getBlockedTime();
  }

  public void update(StackTraceElement[] elements, long cpuTime, long userCPUTime, long timestamp) {
    int i = elements.length - 1;
    CallTree.Node parent = callTree.getRoot();

    while (i >= 0) {
      CallTree.Node existing = parent.findChild(elements[i]);

      if (existing != null) {
        existing.incrSamples(1);

        existing.incrCPUTime(cpuTime - cpuTime0);
        existing.incrTime(timestamp - timestamp0);
        existing.incrUserCPUTime(userCPUTime - userCPUTime0);
        i--;
        parent = existing;
      } else {
        break;
      }
    }

    while (i >= 0) {
      CallTree.Node node = new CallTree.Node(elements[i], 1,
                                             userCPUTime - userCPUTime0, cpuTime - cpuTime0, timestamp - timestamp0);
      parent.addChild(node);

      parent = node;
      i--;
    }
  }

  public CallTree getCallTree() {
    return callTree;
  }

  public long getTotalCPUTimeMs() {
    return TimeUnit.NANOSECONDS.toMillis(cpuTime0 - baselineCPUTime);
  }

  public long getTotalUserCPUTimeMs() {
    return TimeUnit.NANOSECONDS.toMillis(userCPUTime0 - baselineUserCPUTime);
  }

  public long getTotalTimeMs() {
    return TimeUnit.NANOSECONDS.toMillis(timestamp0 - baselineTimestamp);
  }

  public long getWaitedTime() {
    return waitedTime;
  }

  public long getBlockedTime() {
    return blockedTime;
  }

  public long getSamples() {
    return samples;
  }

  public long getTimestamp0() {
    return timestamp0;
  }

  public long getBaselineTimestamp() {
    return baselineTimestamp;
  }

  public String dump() {
    StringBuilder builder = new StringBuilder();

    double cpuPerc = ((double) getTotalCPUTimeMs() / getTotalTimeMs()) * 100.0, userCPUPerc =
        ((double) getTotalUserCPUTimeMs() / getTotalTimeMs()) * 100.0;

    builder.append("[Wait Time: ").append(waitedTime).append("ms, Block time: ").append(blockedTime).append("ms")
        .append(", CPU: ").append(String.format("%2.2f", cpuPerc)).append("%, User CPU:")
        .append(String.format("%2.2f", userCPUPerc)).append("%")
        .append("]\n");
    builder.append(callTree.dump());
    return builder.toString();
  }
}
