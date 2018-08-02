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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.tracing.BinarySequentialLog;
import com.sematext.spm.client.tracing.BinarySequentialSender;
import com.sematext.spm.client.tracing.agent.Sink;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;

public final class AsyncFileCallSink implements Sink<PartialTransaction> {
  private static final Log LOG = LogFactory.getLog(AsyncFileCallSink.class);

  private final ArrayBlockingQueue<byte[]> calls = new ArrayBlockingQueue<byte[]>(1000);
  private volatile boolean running = true;
  private final BinarySequentialSender binaryLog;

  private AsyncFileCallSink(BinarySequentialSender binaryLog) {
    this.binaryLog = binaryLog;
  }

  private Thread sinkThread = new Thread() {
    @Override
    public void run() {
      while (running) {
        try {
          final byte[] call = calls.poll(10, TimeUnit.MILLISECONDS);
          if (call != null) {
            binaryLog.write(call);
          }
        } catch (InterruptedException e) {
          LOG.warn("Sink thread was interrupted.", e);
        }
      }
      if (LOG.isTraceEnabled()) {
        LOG.trace("Sink thread exited.");
      }
    }
  };

  @Override
  public void sink(PartialTransaction transaction) {
    try {
      if (!calls.offer(ThriftPartialTransactionSerializer.serialize(transaction), 10, TimeUnit.MILLISECONDS)) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("Queue is full, can't offer new call.");
        }
      }
    } catch (Throwable e) {
      LOG.warn("Call offer was interrupted.", e);
    }
  }

  public static AsyncFileCallSink create(String basedir, int maxFileSize, int retentionCount) {
    final AsyncFileCallSink sink = new AsyncFileCallSink(
        new BinarySequentialSender(BinarySequentialLog
                                       .create(basedir, "tracing", ".bin", maxFileSize, retentionCount)));
    sink.sinkThread.setDaemon(true);
    sink.sinkThread.setName("spm-tracing-sink-thread");
    sink.sinkThread.start();
    return sink;
  }
}
