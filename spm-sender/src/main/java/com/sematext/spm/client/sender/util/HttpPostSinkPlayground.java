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
package com.sematext.spm.client.sender.util;

import org.apache.flume.Channel;
import org.apache.flume.ChannelSelector;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.Sink;
import org.apache.flume.SinkRunner;
import org.apache.flume.SourceRunner;
import org.apache.flume.agent.embedded.EmbeddedSource;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.channel.MemoryChannel;
import org.apache.flume.channel.ReplicatingChannelSelector;
import org.apache.flume.conf.Configurables;
import org.apache.flume.event.SimpleEvent;
import org.apache.flume.sink.DefaultSinkProcessor;
import org.apache.flume.source.EventDrivenSourceRunner;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Arrays;
import java.util.Map;

public class HttpPostSinkPlayground {

  private static Event event(String msg) {
    final Event event = new SimpleEvent();
    event.setBody(msg.getBytes());
    return event;
  }

  private static Map<String, String> getProperties() {
    Map<String, String> properties = new UnifiedMap<String, String>();

//    properties.put("channel.type", "memory");
//    properties.put("channel.capacity", "1024");
//    properties.put("sinks", "sink1");
//    properties.put("sink1.type", "com.sematext.spm.client.sender.util.HttpPostSink");
    properties.put("http.post.sink.url", "http://localhost:8089/spm-tracing-receiver/thrift");

    return properties;
  }

  public static void main(String[] args) throws Exception {
    final Channel channel = new MemoryChannel();
    channel.setName("channel-12");
    Context channelContext = new Context(getProperties());
    Configurables.configure(channel, channelContext);

    HttpPostSink sink = new HttpPostSink();
    Context sinkContext = new Context(getProperties());
    sink.configure(sinkContext);
    sink.setChannel(channel);

    SinkRunner sinkRunner = new SinkRunner();
    DefaultSinkProcessor sinkProcessor = new DefaultSinkProcessor();
    sinkProcessor.setSinks(Arrays.<Sink>asList(sink));
    sinkRunner.setSink(sinkProcessor);

    EmbeddedSource source = new EmbeddedSource();
    source.setName("http-post-event-source-2");

    ChannelSelector cs = new ReplicatingChannelSelector();
    cs.setChannels(Arrays.asList(channel));

    ChannelProcessor processor = new ChannelProcessor(cs);
    processor.configure(new Context(getProperties()));

    source.setChannelProcessor(processor);

    SourceRunner sourceRunner = new EventDrivenSourceRunner();
    sourceRunner.setSource(source);

    channel.start();
    sinkRunner.start();
    sourceRunner.start();

    for (final String word : "hello flume and big data world".split(" ")) {
      source.put(event(word));
    }

  }

}
