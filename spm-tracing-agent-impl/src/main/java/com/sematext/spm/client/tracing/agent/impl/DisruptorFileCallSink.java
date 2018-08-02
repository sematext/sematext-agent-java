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

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.sematext.spm.client.tracing.BinarySequentialLog;
import com.sematext.spm.client.tracing.BinarySequentialSender;
import com.sematext.spm.client.tracing.agent.Sink;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.tracing.agent.sampling.SamplerStatistics;

public final class DisruptorFileCallSink implements Sink<PartialTransaction> {
  private static final EventFactory<PartialTransaction> NEW_PARTIAL_TRANSACTION = new EventFactory<PartialTransaction>() {
    @Override
    public PartialTransaction newInstance() {
      return new PartialTransaction();
    }
  };
  private static final ThreadFactory DAEMON_THREAD_FACTORY = new ThreadFactory() {
    @Override
    public Thread newThread(final Runnable r) {
      final Thread t = new Thread(r, "disruptor-file-call-sink");
      t.setDaemon(true);
      return t;
    }
  };

  private final BinarySequentialSender binaryLog;
  private final EventHandler<PartialTransaction> eventHandler = new EventHandler<PartialTransaction>() {
    @Override
    public void onEvent(PartialTransaction transaction, long sequence, boolean endOfBatch) throws Exception {
      long t = System.nanoTime();
      binaryLog.write(ThriftPartialTransactionSerializer.serialize(transaction));
      SamplerStatistics.INSTANCE.update(System.nanoTime() - t);
    }
  };

  private final Disruptor<PartialTransaction> disruptor;

  private DisruptorFileCallSink(BinarySequentialSender binaryLog) {
    this.binaryLog = binaryLog;
    this.disruptor = new Disruptor<PartialTransaction>(NEW_PARTIAL_TRANSACTION, 1024,
                                                       Executors.newSingleThreadExecutor(DAEMON_THREAD_FACTORY));
  }

  public static Sink<PartialTransaction> create(String basedir, int maxFileSize, int retentionCount) {
    BinarySequentialSender log = new BinarySequentialSender(BinarySequentialLog
                                                                .create(basedir, "tracing", ".bin", maxFileSize, retentionCount));
    final DisruptorFileCallSink sink = new DisruptorFileCallSink(log);
    sink.start();
    return sink;
  }

  @SuppressWarnings("unchecked")
  private void start() {
    this.disruptor.handleEventsWith(eventHandler);
    this.disruptor.start();
  }

  @Override
  public void sink(PartialTransaction transaction) {
    final RingBuffer<PartialTransaction> buffer = this.disruptor.getRingBuffer();
    long seq = buffer.next();
    try {
      final PartialTransaction event = buffer.get(seq);
      transaction.copy(event);
    } finally {
      buffer.publish(seq);
    }
  }
}
