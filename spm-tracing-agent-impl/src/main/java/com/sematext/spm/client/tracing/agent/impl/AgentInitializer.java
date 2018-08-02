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

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.DataFormat;
import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.tracing.agent.config.DefaultServiceConfigurer;
import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.sampling.Stats;
import com.sematext.spm.client.tracing.agent.sampling.StatsReporter;
import com.sematext.spm.client.unlogger.dynamic.BehaviorDescription;
import com.sematext.spm.client.unlogger.dynamic.BehaviorState;
import com.sematext.spm.client.util.FifoInterface;

public class AgentInitializer {
  private static final Log LOG = LogFactory.getLog(AgentInitializer.class);

  public static void premain(String args, Instrumentation instrumentation) throws Exception {
    init(args, instrumentation, true, true);
  }

  private static void startFifo() {
    final String fifoPath = System.getProperty("spm.tracing.agent.fifo");
    if (fifoPath != null && new File(fifoPath).exists()) {
      final TracingAgentControlImpl ctl = (TracingAgentControlImpl) ServiceLocator.getTracingAgentControl();
      LOG.info("Starting FIFO debug interface for " + fifoPath);

      FifoInterface.create()
          .addHandler("enableTracing", new FifoInterface.Handler() {
            @Override
            public void handle(String... args) {
              LOG.info("Got enable tracing command. Tracing enabled: " + ctl.enable());
            }
          })
          .addHandler("disableTracing", new FifoInterface.Handler() {
            @Override
            public void handle(String... args) {
              LOG.info("Got disable tracing command. Tracing disabled: " + ctl.disable());
            }
          })
          .addHandler("statistics", new FifoInterface.Handler() {
            @Override
            public void handle(String... args) {
              LOG.info(ctl.getStatistics());
            }
          })
          .addHandler("getWeavedClasses", new FifoInterface.Handler() {
            @Override
            public void handle(String[] arguments) {
              LOG.info(ctl.getWeavedClasses());
            }
          })
          .addHandler("instrumentMethod", new FifoInterface.Handler() {
            @Override
            public void handle(String[] args) {
              if (args.length == 1) {
                String signature = args[0];

                ctl.getInstrumentationSettings()
                    .updateBehaviorState(new BehaviorDescription(signature), new BehaviorState(true, true));
                LOG.info("Got updateBehaviorState command: " + ctl.applyInstrumentationSettings());
              }
            }
          })
          .addHandler("uninstrumentMethod", new FifoInterface.Handler() {
            @Override
            public void handle(String[] args) {
              if (args.length == 1) {
                String signature = args[0];

                ctl.getInstrumentationSettings()
                    .updateBehaviorState(new BehaviorDescription(signature), new BehaviorState(false, false));
                LOG.info("Got uninstrumentMethod command: " + ctl.applyInstrumentationSettings());
              }
            }
          })
          .start(fifoPath);
    }
  }

  public static void init(String args, Instrumentation instrumentation, boolean tracingEnabled, boolean initLogging)
      throws Exception {
    ServiceLocator.configure(DefaultServiceConfigurer
                                 .embeddedAgentConfigurer(args, instrumentation, tracingEnabled, AgentInitializer.class
                                     .getClassLoader()));

    // createRuntimeConfig();

    if (initLogging) {
      MonitorUtil.MonitorArgs monitorArgs = MonitorUtil.extractMonitorArgs(args);
      Integer processOrdinal = MonitorUtil
          .obtainMonitorLock(monitorArgs.getToken(), monitorArgs.getJvmName(), monitorArgs.getSubType());

      LogFactory.init(ServiceLocator.getConfig().getLogPath(), 1024 * 1024 * 100, 10,
                      System.getProperty("spm.tracing.loglevel", "INFO"), DataFormat.PLAIN_TEXT,
                      processOrdinal);
    }

    startFifo();

    LOG.info("SPM Tracing Agent Initialized.");

    if (Boolean.getBoolean("spm.tracing.sampler.stats")) {
      StatsReporter.start(Stats.INSTANCE, Long.getLong("spm.tracing.sampler.interval", 500), TimeUnit.MILLISECONDS);
    }
  }
}
