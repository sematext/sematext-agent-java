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

import com.sematext.spm.client.agent.profiler.CallTree;
import com.sematext.spm.client.monitor.thrift.TAllThreadsProfileSnapshot;

public final class AllThreadsProfileSnapshot {
  private CallTree tree;
  private long time;
  private long cpuTime;
  private long gcTime;
  private long userCpuTime;
  private long waitedTime;
  private long blockedTime;
  private long samples;
  private boolean cpuTimeSupported;

  private AllThreadsProfileSnapshot() {
  }

  public CallTree getTree() {
    return tree;
  }

  public long getTime() {
    return time;
  }

  public long getCpuTime() {
    return cpuTime;
  }

  public long getUserCpuTime() {
    return userCpuTime;
  }

  public long getWaitedTime() {
    return waitedTime;
  }

  public long getBlockedTime() {
    return blockedTime;
  }

  public long getGcTime() {
    return gcTime;
  }

  public long getSamples() {
    return samples;
  }

  public boolean isCpuTimeSupported() {
    return cpuTimeSupported;
  }

  public static class Builder {
    private final AllThreadsProfileSnapshot snapshot = new AllThreadsProfileSnapshot();

    private Builder() {
    }

    public Builder tree(CallTree tree) {
      snapshot.tree = tree;
      return this;
    }

    public Builder time(long time) {
      snapshot.time = time;
      return this;
    }

    public Builder gcTime(long gcTime) {
      snapshot.gcTime = gcTime;
      return this;
    }

    public Builder cpuTime(long cpuTime) {
      snapshot.cpuTime = cpuTime;
      return this;
    }

    public Builder userCpuTime(long userCpuTime) {
      snapshot.userCpuTime = userCpuTime;
      return this;
    }

    public Builder waitedTime(long waitedTime) {
      snapshot.waitedTime = waitedTime;
      return this;
    }

    public Builder blockedTime(long blockedTime) {
      snapshot.blockedTime = blockedTime;
      return this;
    }

    public Builder cpuTimeSupported(boolean cpuTimeSupported) {
      snapshot.cpuTimeSupported = cpuTimeSupported;
      return this;
    }

    public Builder samples(long samples) {
      snapshot.samples = samples;
      return this;
    }

    public AllThreadsProfileSnapshot build() {
      return snapshot;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static AllThreadsProfileSnapshot fromThrift(TAllThreadsProfileSnapshot thrift) {
    AllThreadsProfileSnapshot snapshot = new AllThreadsProfileSnapshot();
    snapshot.tree = CallTree.fromThrift(thrift.getTree());
    snapshot.time = thrift.getTime();
    snapshot.cpuTime = thrift.getCpuTime();
    snapshot.userCpuTime = thrift.getUserCPUTime();
    snapshot.samples = thrift.getSamples();
    snapshot.waitedTime = thrift.getWaitedTime();
    snapshot.blockedTime = thrift.getBlockedTime();
    snapshot.gcTime = thrift.getGcTime();
    snapshot.cpuTimeSupported = thrift.isCpuTimeSupported();
    return snapshot;
  }
}
