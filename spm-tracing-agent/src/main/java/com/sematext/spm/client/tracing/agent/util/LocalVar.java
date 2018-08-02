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

import java.util.Map;

import com.sematext.spm.client.tracing.agent.Trace;
import com.sematext.spm.client.tracing.agent.Tracing;
import com.sematext.spm.client.unlogger.utils.ConcurrentWeakHashMap;

public final class LocalVar {
  private LocalVar() {
  }

  private static final Map<Thread, Trace> vars = new ConcurrentWeakHashMap<Thread, Trace>();

  public static void put(Thread thread, Trace tracing) {
    vars.put(thread, tracing);
  }

  public static void put(Thread thread) {
    put(thread, Tracing.current());
  }

  public static void preStart() {
    try {
      Trace t = vars.get(Thread.currentThread());
      if (t != null) {
        Tracing.setTrace(t.fork());
      }
    } catch (Exception e) { /* */ }
  }

  public static void postStart() {
    try {
      vars.remove(Thread.currentThread());
      Tracing.endTrace();
    } catch (Exception e) { /* */ }
  }

  public static Trace get(Thread thread) {
    return vars.get(thread);
  }

}
