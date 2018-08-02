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
package com.sematext.spm.client.tracing.agent.config;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.tracing.agent.MuxSink;
import com.sematext.spm.client.tracing.agent.Sink;
import com.sematext.spm.client.tracing.agent.TracingAgentControl;
import com.sematext.spm.client.tracing.agent.errors.TracingError;
import com.sematext.spm.client.tracing.agent.impl.DisruptorThriftEventSink;
import com.sematext.spm.client.tracing.agent.impl.TracingAgentControlImpl;
import com.sematext.spm.client.tracing.agent.impl.TracingStatisticsImpl;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.sampling.FixedRateSampler;
import com.sematext.spm.client.tracing.agent.sampling.Sampler;
import com.sematext.spm.client.tracing.agent.sampling.TracingErrorFixedRateSampler;
import com.sematext.spm.client.tracing.agent.sampling.WeightedFixedRateSampler;
import com.sematext.spm.client.tracing.agent.stats.TracingStatistics;
import com.sematext.spm.client.util.StorageUnit;

public class DefaultServiceConfigurer implements ServiceConfigurer {
  private Config config;
  private Sampler<String> transactionSampler;
  private Sampler<TracingError> tracingErrorSampler;
  private List<Sink<PartialTransaction>> transactionSinks;
  private List<Sink<TracingError>> errorSinks;
  private TracingStatistics tracingStatistics;
  private TracingAgentControl tracingAgentControl;

  private DefaultServiceConfigurer() {
  }

  @Override
  public Config getConfig() {
    return config;
  }

  @Override
  public Sampler<String> getTransactionSampler() {
    return transactionSampler;
  }

  @Override
  public Sampler<TracingError> getTracingErrorSampler() {
    return tracingErrorSampler;
  }

  @Override
  public List<Sink<PartialTransaction>> getTransactionSinks() {
    return transactionSinks;
  }

  @Override
  public List<Sink<TracingError>> getErrorSinks() {
    return errorSinks;
  }

  @Override
  public TracingStatistics getTracingStatistics() {
    return tracingStatistics;
  }

  @Override
  public TracingAgentControl getTracingAgentControl() {
    return tracingAgentControl;
  }

  @SuppressWarnings("unchecked")
  public static DefaultServiceConfigurer embeddedAgentConfigurer(final String args, Instrumentation instrumentation,
                                                                 boolean tracingEnabled, ClassLoader loader) {
    final DefaultServiceConfigurer configurer = new DefaultServiceConfigurer();

    configurer.config = Config.embeddedAgentConfig(args);
    configurer.transactionSampler = new FixedRateSampler(1, TimeUnit.MINUTES, 10000);

    final DisruptorThriftEventSink eventSink = DisruptorThriftEventSink
        .create(configurer.config.getLogPath(), (int) StorageUnit.MEGABYTES.toBytes(100), 3);
    configurer.transactionSinks = Arrays.<Sink<PartialTransaction>>asList(new MuxSink<PartialTransaction>(eventSink));
    configurer.errorSinks = Arrays.<Sink<TracingError>>asList(new MuxSink<TracingError>(eventSink));

    configurer.transactionSampler = new WeightedFixedRateSampler(1, 1, 1, TimeUnit.SECONDS);
    configurer.tracingErrorSampler = new TracingErrorFixedRateSampler(100, 1, TimeUnit.MINUTES);
    configurer.tracingStatistics = new TracingStatisticsImpl();
    configurer.tracingAgentControl = TracingAgentControlImpl
        .create(tracingEnabled, instrumentation, loader, configurer.config);
    return configurer;
  }
}
