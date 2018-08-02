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
package com.sematext.spm.client.unlogger.dispatch;

import java.util.concurrent.atomic.AtomicInteger;

import com.sematext.spm.client.unlogger.JoinPoint;
import com.sematext.spm.client.unlogger.Pointcut;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.dispatch.DefaultAdviceDispatcher.DefaultLoggerContext;

public final class DispatchUnit {

  private static final int MAX_DISPATCH_UNITS = 0x400;

  private static final DispatchUnit[] DISPATCH_UNITS = new DispatchUnit[MAX_DISPATCH_UNITS];
  private static final AtomicInteger DISPATCH_UNIT_ID_GENERATOR = new AtomicInteger();
  private static volatile int flush = 0;

  public static DispatchUnit getDispatchUnit(int dispatchId) {
    int bar = flush;
    return DISPATCH_UNITS[dispatchId];
  }

  public static int registerDispatchUnit(String name, Pointcut pointcut, UnloggableLogger advice) {
    int id = DISPATCH_UNIT_ID_GENERATOR.incrementAndGet();
    if (id >= MAX_DISPATCH_UNITS) {
      throw new IllegalStateException("Can't insert " + name + " pointcut to dispatch table, pointcut id -> " + id +
                                          " , table size -> " + MAX_DISPATCH_UNITS);
    }
    if (advice == null) {
      throw new IllegalStateException("Advice is null for -> " + name + " " + pointcut);
    }
    DISPATCH_UNITS[id] = make(name, pointcut, advice);
    // O-ho-ho, piggybacking flush here, to make Alexey calm.
    // Pointcuts will work in threads which be created after that
    // So, the simple write to volatile it will be enough (at least on intels)
    flush = id;
    return id;
  }

  private final String sectionName;
  private final UnloggableLogger advice;
  private final Pointcut pointcut;

  private DispatchUnit(String name, Pointcut pointcut, UnloggableLogger advice) {
    this.sectionName = name;
    this.pointcut = pointcut;
    this.advice = advice;
  }

  private static DispatchUnit make(String name, Pointcut pointcut, UnloggableLogger advice) {
    return new DispatchUnit(name, pointcut, advice);
  }

  public void logBefore(DefaultLoggerContext context, JoinPoint joinPoint, Object that, Object[] params) {
    context.setSection(sectionName);
    context.setThat(that);
    context.setJoinPoint(joinPoint);

    context.setPointcut(pointcut, params);

    advice.logBefore(context);
  }

  public void logAfter(DefaultLoggerContext context, JoinPoint joinPoint, Object that, Object returnValue) {
    advice.logAfter(context, returnValue);
  }

  public void logThrow(DefaultLoggerContext context, JoinPoint joinPoint, Object that, Throwable throwable) {
    advice.logThrow(context, throwable);
  }

}
