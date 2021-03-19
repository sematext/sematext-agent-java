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
package com.sematext.spm.client.sender.flume;

import com.google.common.base.Preconditions;

import org.apache.flume.Channel;
import org.apache.flume.ChannelSelector;
import org.apache.flume.Context;
import org.apache.flume.FlumeException;
import org.apache.flume.Sink;
import org.apache.flume.Sink.Status;
import org.apache.flume.SinkRunner;
import org.apache.flume.Source;
import org.apache.flume.SourceRunner;
import org.apache.flume.agent.embedded.EmbeddedSource;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.channel.DaemonSpillableMemoryChannel;
import org.apache.flume.channel.MemoryChannel;
import org.apache.flume.channel.ReplicatingChannelSelector;
import org.apache.flume.conf.Configurables;
import org.apache.flume.lifecycle.DaemonLifecycleSupervisor;
import org.apache.flume.lifecycle.LifecycleAware;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.flume.sink.DefaultSinkProcessor;
import org.apache.flume.source.EventDrivenSourceRunner;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.sender.flume.influx.InfluxSink;
import com.sematext.spm.client.sender.util.DynamicUrlParamSink;

/**
 * Sematext specific variant of flume's EmbeddedAgent used for Sender. It uses 1 sink, 1 channel and 1 source.
 * In case processing requires handling of N separate applications, N instances of SenderEmbeddedAgent will
 * have to be used.
 */
public class SenderEmbeddedAgent {
  private static final Log LOGGER = LogFactory.getLog(SenderEmbeddedAgent.class);

  public static String SINK_CLASS_PARAM = "sinkClass";

  private State state;
  private final DaemonLifecycleSupervisor supervisor;
  private Channel channel;
  private SinkRunner sinkRunner;
  private DynamicUrlParamSink sink;
  private List<SourceRunner> sourceRunners = new FastList<SourceRunner>();
  private Map<String, String> sourceInterceptorConfigs = new UnifiedMap<String, String>();

  private Map<String, String> properties;

  private String appTokens = "";

  public SenderEmbeddedAgent() {
    this.state = State.NEW;
    this.supervisor = new DaemonLifecycleSupervisor();
  }

  /*CHECKSTYLE:OFF*/
  public void configure(Map<String, String> properties) throws FlumeException {
    if (state == State.STARTED) {
      throw new IllegalStateException("Cannot be configured while started");
    }

    this.properties = properties;
    doConfigure(properties);
    state = State.STOPPED;
  }
  /*CHECKSTYLE:ON*/

  @SuppressWarnings("unchecked")
  private static DynamicUrlParamSink newSinkInstance(Map<String, String> properties) {
    Preconditions.checkNotNull(properties);
    final String sinkClassName = properties.get(SINK_CLASS_PARAM);
    Preconditions.checkNotNull(sinkClassName, SINK_CLASS_PARAM + " should be defined.");

    try {
      Class<? extends DynamicUrlParamSink> klass = (Class<? extends DynamicUrlParamSink>) Class.forName(sinkClassName);
      return klass.newInstance();
    } catch (Exception e) {
      throw new IllegalStateException("Can't instantiate sink class '" + sinkClassName + "'.", e);
    }
  }

  private void doConfigure(Map<String, String> properties) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Agent configuration values");
      for (String key : new TreeSet<String>(properties.keySet())) {
        LOGGER.debug(key + " = " + properties.get(key));
      }
    }

    // create channel
    if (properties.get("checkpointDir") == null) {
      channel = new MemoryChannel();
      channel.setName("Sender event - memory channel");
    } else {
      channel = new DaemonSpillableMemoryChannel();
      channel.setName("Sender event - spillable channel");
    }

    Context channelContext = new Context(properties);
    Configurables.configure(channel, channelContext);

    // create sink
    sink = newSinkInstance(properties);
    Context sinkContext = new Context(properties);
    sink.configure(sinkContext);
    sink.setChannel(channel);
    sinkRunner = new SinkRunner();
    DefaultSinkProcessor sinkProcessor = new DefaultSinkProcessor();
    List<Sink> sinks = new FastList<Sink>();
    sinks.add(sink);
    sinkProcessor.setSinks(sinks);
    sinkRunner.setSink(sinkProcessor);

    // copy interceptor configs
    for (String key : properties.keySet()) {
      if (key.startsWith("interceptors")) {
        sourceInterceptorConfigs.put(key, properties.get(key));
      }
    }
  }

  /*CHECKSTYLE:OFF*/
  public void start() throws FlumeException {
    if (state == State.STARTED) {
      throw new IllegalStateException("Cannot be started while started");
    } else if (state == State.NEW) {
      throw new IllegalStateException("Cannot be started before being configured");
    }

    doStart();

    state = State.STARTED;
  }
  /*CHECKSTYLE:ON*/

  private void doStart() {
    boolean error = true;
    try {
      try {
        channel.start();
      } catch (Throwable thr) {
        LOGGER.warn("Unable to start channel, switching to alternative dirs", thr);

        LOGGER.warn("Stopping previous channel...");
        try {
          // first stop the channel
          channel.stop();
        } catch (Throwable thr2) {
          LOGGER.warn("Error while stopping previous channel, ignoring the error...");
        }

        // try changing checkpointDir and dataDirs
        properties.put("checkpointDir", properties.get("checkpointDirAlter"));
        properties.put("checkpointDirAlter", null);
        properties.put("dataDirs", properties.get("dataDirsAlter"));
        properties.put("dataDirsAlter", null);

        doConfigure(properties);

        try {
          channel.start();
        } catch (Throwable thr3) {
          LOGGER.warn("Unable to start channel even with alternatives, switching to Memory channel", thr);
          properties.put("checkpointDir", null);
          properties.put("checkpointDirAlter", null);
          properties.put("dataDirs", null);
          properties.put("dataDirsAlter", null);

          doConfigure(properties);

          channel.start();
        }
      }

      sinkRunner.start();

      supervisor.supervise(channel,
                           new DaemonLifecycleSupervisor.SupervisorPolicy.AlwaysRestartPolicy(), LifecycleState.START);
      supervisor.supervise(sinkRunner,
                           new DaemonLifecycleSupervisor.SupervisorPolicy.AlwaysRestartPolicy(), LifecycleState.START);

      error = false;
    } catch (Throwable thr) {
      LOGGER.error("Error while starting channel and sink", thr);
    } finally {
      if (error) {
        stopLogError(channel);
        stopLogError(sinkRunner);
        supervisor.stop();
      }
    }
  }

  private void stopLogError(LifecycleAware lifeCycleAware) {
    try {
      if (LifecycleState.START.equals(lifeCycleAware.getLifecycleState())) {
        lifeCycleAware.stop();
      }
    } catch (Exception e) {
      LOGGER.warn("Exception while stopping " + lifeCycleAware, e);
    }
  }

  /*CHECKSTYLE:OFF*/
  public void stop() throws FlumeException {
    if (state != State.STARTED) {
      throw new IllegalStateException("Cannot be stopped unless started");
    }
    supervisor.stop();
    state = State.STOPPED;
    // clear references to sources?
  }
  /*CHECKSTYLE:ON*/

  /**
   * Creates and starts simple source.
   *
   * @return freshly started source, or already existing source if it was created before
   */
  public Source createAndStartSource() {
    if (sourceRunners.size() == 1) {
      return sourceRunners.get(0).getSource();
    } else if (sourceRunners.size() == 0) {
      Source source = new EmbeddedSource();
      source.setName("Sender event source");

      ChannelSelector cs = new ReplicatingChannelSelector();
      List<Channel> sourceChannels = new FastList<Channel>();
      sourceChannels.add(channel);
      cs.setChannels(sourceChannels);

      ChannelProcessor cp = new ChannelProcessor(cs);
      Context cpCtx = new Context(sourceInterceptorConfigs);
      cp.configure(cpCtx);

      source.setChannelProcessor(cp);

      SourceRunner sourceRunner = new EventDrivenSourceRunner();
      sourceRunner.setSource(source);

      sourceRunner.start();
      sourceRunners.add(sourceRunner);

      supervisor.supervise(sourceRunner,
                           new DaemonLifecycleSupervisor.SupervisorPolicy.AlwaysRestartPolicy(), LifecycleState.START);

      return source;
    } else {
      throw new IllegalStateException("More than one source in this sink! " + sourceRunners);
    }
  }

  private static enum State {
    NEW(),
    STOPPED(),
    STARTED();
  }

  protected Channel getChannel() {
    return channel;
  }

  protected void setChannel(Channel channel) {
    this.channel = channel;
  }

  protected List<SourceRunner> getSourceRunners() {
    return sourceRunners;
  }

  protected void setSourceRunners(List<SourceRunner> sourceRunners) {
    this.sourceRunners = sourceRunners;
  }

  protected Map<String, String> getSourceInterceptorConfigs() {
    return sourceInterceptorConfigs;
  }

  protected void setSourceInterceptorConfigs(Map<String, String> sourceInterceptorConfigs) {
    this.sourceInterceptorConfigs = sourceInterceptorConfigs;
  }

  protected DaemonLifecycleSupervisor getSupervisor() {
    return supervisor;
  }

  public long getLastSinkEventTakeTimestamp() {
    if (sink != null) {
      return sink.getLastEventTakeTimestamp();
    } else {
      return -1;
    }
  }

  public State getState() {
    return state;
  }

  public Status getSinkStatus() {
    if (sink != null) {
      if (sink instanceof InfluxSink) {
        return ((InfluxSink) sink).getLastSinkProcessStatus();
      }
    }

    return null;
  }
}
