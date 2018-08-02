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
package com.sematext.spm.client.tracing.agent.impl;

import java.lang.instrument.Instrumentation;
import java.util.Set;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.tracing.agent.TracingAgentControl;
import com.sematext.spm.client.tracing.agent.config.Config;
import com.sematext.spm.client.unlogger.dynamic.AgentStatistics;
import com.sematext.spm.client.unlogger.dynamic.DynamicInstrumentation;
import com.sematext.spm.client.unlogger.dynamic.InstrumentationSettings;

public final class TracingAgentControlImpl implements TracingAgentControl {
  private final Log log = LogFactory.getLog(TracingAgentControlImpl.class);

  private final DynamicInstrumentation dynamicInstrumentation;
  private final InstrumentationSettings instrumentationSettings = new InstrumentationSettings();
  private final AgentStatistics statistics = new AgentStatistics();

  public TracingAgentControlImpl(boolean tracingEnabled, Instrumentation instrumentation, TracingArtifacts artifacts) {
    dynamicInstrumentation = new DynamicInstrumentation(instrumentationSettings, statistics, artifacts
        .getDynamicTransformers(),
                                                        artifacts
                                                            .getClassesToBeRetransformed(), instrumentation, tracingEnabled, artifacts
                                                            .getUserTracedMethodEntryPointDispatchId(),
                                                        artifacts.getUserTracedMethodNonEntryPointDispatchId());
  }

  private void initialize() {
    dynamicInstrumentation.init();
  }

  @Override
  public boolean enable() {
    return dynamicInstrumentation.enable();
  }

  @Override
  public boolean disable() {
    return dynamicInstrumentation.disable();
  }

  @Override
  public boolean isEnabled() {
    return dynamicInstrumentation.isEnabled();
  }

  public AgentStatistics getStatistics() {
    return statistics;
  }

  public Set<String> getWeavedClasses() {
    return dynamicInstrumentation.getWeavedClasses();
  }

  @Override
  public InstrumentationSettings getInstrumentationSettings() {
    return instrumentationSettings;
  }

  @Override
  public boolean applyInstrumentationSettings() {
    return dynamicInstrumentation.applyTransformationSettings();
  }

  public static TracingAgentControlImpl create(boolean tracingEnabled, Instrumentation instrumentation,
                                               ClassLoader loader, Config config) {
    final TracingAgentControlImpl control = new TracingAgentControlImpl(tracingEnabled, instrumentation, TracingArtifacts
        .create(loader, config));
    control.initialize();
    return control;
  }
}
