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
package com.sematext.spm.client.unlogger.dynamic;

/**
 * State of instrumentation behavior:
 * - should be instrumented or not
 * - should be entryPoint (start new transaction when behavior called
 */
public final class BehaviorState {
  private final boolean entryPoint;
  private final boolean enabled;

  public BehaviorState(boolean entryPoint, boolean enabled) {
    this.entryPoint = entryPoint;
    this.enabled = enabled;
  }

  public boolean isEntryPoint() {
    return entryPoint;
  }

  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public String toString() {
    return "BehaviorState{" +
        "entryPoint=" + entryPoint +
        ", enabled=" + enabled +
        '}';
  }
}
