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
package com.sematext.spm.client.tracing.agent.util;

import com.sematext.spm.client.tracing.agent.Trace;
import com.sematext.spm.client.tracing.agent.Tracing;

public class TracedRunnable implements Runnable {
  private final Runnable runnable;
  private final Trace trace;

  public TracedRunnable(Runnable runnable) {
    if (runnable.getClass().isAssignableFrom(TracedRunnable.class)) {
      this.runnable = ((TracedRunnable) runnable).runnable;
    } else {
      this.runnable = runnable;
    }
    this.trace = Tracing.current();
  }

  @Override
  public void run() {
    try {
      Tracing.setTrace(this.trace.fork());
    } catch (Exception e) {
      /* pass */
    }
    try {
      runnable.run();
    } finally {
      try {
        Tracing.endTrace();
      } catch (Exception e) {
        /* pass */
      }
    }
  }

  public static Runnable tryCreate(Runnable from) {
    if (from == null) {
      return null;
    }
    if (from.getClass().getInterfaces().length == 1 && from.getClass().getSuperclass().equals(Object.class)) {
      return new TracedRunnable(from);
    }
    return from;
  }

}
