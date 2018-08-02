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

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

import org.apache.thrift.TException;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.tracing.BinarySequentialLog;
import com.sematext.spm.client.tracing.BinarySequentialSender;
import com.sematext.spm.client.tracing.agent.Sink;
import com.sematext.spm.client.tracing.agent.SinkEvent;
import com.sematext.spm.client.tracing.agent.errors.TracingError;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.thrift.TTracingEvent;
import com.sematext.spm.client.tracing.thrift.TTracingEventType;
import com.sematext.spm.client.util.Preconditions;

public final class DisruptorThriftEventSink implements Sink<SinkEvent> {

  private static final Log LOG = LogFactory.getLog(DisruptorThriftEventSink.class);

  private static class Event {
    private EventDescriptor descriptor;
    private final PartialTransaction partialTransaction = new PartialTransaction();
    private final TracingError tracingError = new TracingError();

    public EventDescriptor getDescriptor() {
      return descriptor;
    }

    public PartialTransaction getPartialTransaction() {
      return partialTransaction;
    }

    public TracingError getTracingError() {
      return tracingError;
    }
  }

  private static enum EventDescriptor {
    PARTIAL_TRANSACTION(PartialTransaction.class) {
      @Override
      void copy(Object from, Event event) {
        ((PartialTransaction) from).copy(event.getPartialTransaction());
        event.descriptor = this;
      }

      @Override
      byte[] toThrift(Event event) throws TException {
        final TTracingEvent thriftEvent = new TTracingEvent(TTracingEventType.PARTIAL_TRANSACTION);
        thriftEvent.setPartialTransaction(ThriftPartialTransactionSerializer.toThrift(event.getPartialTransaction()));
        return ThriftUtils.binaryProtocolSerializer().serialize(thriftEvent);
      }
    },
    TRACING_ERROR(TracingError.class) {
      @Override
      void copy(Object from, Event event) {
        ((TracingError) from).copy(event.getTracingError());
        event.descriptor = this;
      }

      @Override
      byte[] toThrift(Event event) throws TException {
        final TTracingEvent thriftEvent = new TTracingEvent(TTracingEventType.TRACING_ERROR);
        thriftEvent.setTracingError(ThriftTracingErrorSerializer.toThrift(event.getTracingError()));
        return ThriftUtils.binaryProtocolSerializer().serialize(thriftEvent);
      }
    };

    final Class<?> klass;

    abstract void copy(Object from, Event event);

    abstract byte[] toThrift(Event event) throws TException;

    EventDescriptor(Class<?> klass) {
      this.klass = klass;
    }

    static EventDescriptor get(Class<?> klass) {
      for (EventDescriptor descriptor : values()) {
        if (descriptor.klass.equals(klass)) {
          return descriptor;
        }
      }
      throw new IllegalArgumentException("Unknown event type: " + klass);
    }
  }

  private final EventFactory<Event> eventFactory = new EventFactory<Event>() {
    @Override
    public Event newInstance() {
      return new Event();
    }
  };

  private static final ThreadFactory DAEMON_THREAD_FACTORY = new ThreadFactory() {
    @Override
    public Thread newThread(final Runnable r) {
      final Thread t = new Thread(r, "disruptor-thrift-event-sink");
      t.setDaemon(true);
      return t;
    }
  };

  private final EventHandler<Event> eventHandler = new EventHandler<Event>() {
    @Override
    public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
      try {
        binaryLog.write(event.getDescriptor().toThrift(event));
      } catch (Exception e) {
        LOG.error("Can't write event to binary log. Event = " + event + ".", e);
      }
    }
  };

  private final BinarySequentialSender binaryLog;
  private final Disruptor<Event> disruptor;

  private DisruptorThriftEventSink(BinarySequentialSender binaryLog) {
    this.binaryLog = binaryLog;
    this.disruptor = new Disruptor<Event>(eventFactory, 1024, Executors.newSingleThreadExecutor(DAEMON_THREAD_FACTORY));
  }

  private void start() {
    this.disruptor.handleEventsWith(eventHandler);
    this.disruptor.start();
  }

  public static DisruptorThriftEventSink create(String basedir, int maxFileSize, int retentionCount) {
    return create(basedir, "tracing", ".bin", maxFileSize, retentionCount);
  }

  public static DisruptorThriftEventSink create(String basedir, String prefix, String suffix, int maxFileSize,
                                                int retentionCount) {
    BinarySequentialSender log = new BinarySequentialSender(BinarySequentialLog
                                                                .create(basedir, prefix, suffix, maxFileSize, retentionCount));
    final DisruptorThriftEventSink sink = new DisruptorThriftEventSink(log);
    sink.start();
    return sink;
  }

  @Override
  public void sink(SinkEvent sinkEvent) {
    Preconditions.checkNotNull(sinkEvent);

    final Object obj = sinkEvent.getObj();
    final EventDescriptor descriptor = EventDescriptor.get(obj.getClass());
    final RingBuffer<Event> ringBuffer = this.disruptor.getRingBuffer();
    final long seq = ringBuffer.next();
    try {
      descriptor.copy(obj, ringBuffer.get(seq));
    } finally {
      ringBuffer.publish(seq);
    }
  }
}
