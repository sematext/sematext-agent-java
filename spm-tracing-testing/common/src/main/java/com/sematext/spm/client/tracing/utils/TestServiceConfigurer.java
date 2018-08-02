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
package com.sematext.spm.client.tracing.utils;

import java.util.ArrayList;
import java.util.List;

import com.sematext.spm.client.tracing.agent.Sink;
import com.sematext.spm.client.tracing.agent.TracingAgentControl;
import com.sematext.spm.client.tracing.agent.config.Config;
import com.sematext.spm.client.tracing.agent.config.ServiceConfigurer;
import com.sematext.spm.client.tracing.agent.errors.TracingError;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.sampling.NoSampling;
import com.sematext.spm.client.tracing.agent.sampling.Sampler;
import com.sematext.spm.client.tracing.agent.stats.TracingStatistics;

public class TestServiceConfigurer implements ServiceConfigurer {
  private final Config config;
  private final Sampler<String> transactionSampler;
  private final Sampler<TracingError> tracingErrorSampler;
  private final List<Sink<PartialTransaction>> sinks = new ArrayList<Sink<PartialTransaction>>();
  private final List<Sink<TracingError>> errorSinks = new ArrayList<Sink<TracingError>>();

  public TestServiceConfigurer(String token) {
    this.config = Config.getConfig("token=" + token);
    this.transactionSampler = new NoSampling<String>();
    this.tracingErrorSampler = new NoSampling<TracingError>();
  }

  @Override
  public Config getConfig() {
    return this.config;
  }

  @Override
  public Sampler<String> getTransactionSampler() {
    return this.transactionSampler;
  }

  @Override
  public Sampler<TracingError> getTracingErrorSampler() {
    return this.tracingErrorSampler;
  }

  @Override
  public List<Sink<PartialTransaction>> getTransactionSinks() {
    return sinks;
  }

  @Override
  public List<Sink<TracingError>> getErrorSinks() {
    return errorSinks;
  }

  @Override
  public TracingStatistics getTracingStatistics() {
    return null;
  }

  @Override
  public TracingAgentControl getTracingAgentControl() {
    return null;
  }
}
