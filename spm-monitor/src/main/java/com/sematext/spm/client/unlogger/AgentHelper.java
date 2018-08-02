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

import static com.sematext.spm.client.unlogger.dispatch.DispatchUnit.registerDispatchUnit;
import static com.sematext.spm.client.util.ReflectionUtils.ClassValue.cv;
import static java.util.Collections.singletonMap;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.unlogger.dispatch.AdviceDispatcher;
import com.sematext.spm.client.unlogger.dispatch.AdviceDispatcher.Type;
import com.sematext.spm.client.unlogger.dispatch.LastInchOfTrampoline;

/**
 * Before we eliminate paralles agents, let it be here.
 */
public final class AgentHelper {

  private static final Log LOG = LogFactory.getLog(AgentHelper.class);

  private AgentHelper() {
    // It's utility class and can't be instantiated
  }

  public static void registerLoggers(Collection<? extends Logspect> loggers, LogLineCollector logLineCollector,
                                     Instrumentation instrumentation) {

    Map<Pointcut, Integer> pointcutToDispatchId = new UnifiedMap<Pointcut, Integer>();
    for (Logspect logger : loggers) {
      processLoggerClass(logger, pointcutToDispatchId, logLineCollector);
    }

    LogPointWeaver weaver = new LogPointWeaver(pointcutToDispatchId, LastInchOfTrampoline.class.getName());
    instrumentation.addTransformer(new UnLoggerClassTransformer(weaver));
  }

  private static void processLoggerClass(Logspect logspect, Map<Pointcut, Integer> toWeave,
                                         LogLineCollector logLineCollector) {

    UnloggableLogger logger = logspect.makeInstance(cv(Map.class,
                                                       singletonMap(LogLineCollector.WIRING_NAME, logLineCollector)));

    for (Pointcut pointcut : logspect.getPointcuts()) {
      int dispatchId = registerDispatchUnit(logspect.getName(), pointcut, logger);
      toWeave.put(pointcut, dispatchId);
    }
  }

  public static void dynamicSwitchTo(DynamicSwitch dynamic) {
    LastInchOfTrampoline.switchToIfNeeded(dynamic.getType());
  }

  public enum DynamicSwitch {
    ON(AdviceDispatcher.Type.DEFAULT), OFF(AdviceDispatcher.Type.NO_OP);

    private final AdviceDispatcher.Type type;

    private DynamicSwitch(Type type) {
      this.type = type;
    }

    protected AdviceDispatcher.Type getType() {
      return type;
    }
  }

}
