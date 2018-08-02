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

//import com.sematext.spm.client.tracing.BinarySequentialSender;

public class AsyncLockFreeCallSink /*implements Sink<PartialTransaction>*/ {
//
//  private static final Log LOG = LogFactory.getLog(AsyncFileCallSink.class);
//
//  private final ConcurrentLinkedQueue<Call> calls = new ConcurrentLinkedQueue<Call>();
//  private final BinarySequentialSender binaryLog;
//  private volatile boolean running = true;
//  private Thread sinkThread = new Thread() {
//    @Override
//    public void run() {
//      while (running) {
//        final Call call = calls.poll();
//        if (call != null) {
//          binaryLog.write(ThriftCallSerializer.serialize(call));
//        }
//      }
//      if (LOG.isTraceEnabled()) {
//        LOG.trace("Sink thread exited.");
//      }
//    }
//  };
//
//  private AsyncLockFreeCallSink(BinarySequentialSender binaryLog) {
//    this.binaryLog = binaryLog;
//  }
//
//  public static AsyncLockFreeCallSink create(String basedir, int maxFileSize, int retentionCount) {
//    final AsyncLockFreeCallSink sink = new AsyncLockFreeCallSink(
//        new BinarySequentialSender(BinarySequentialLog.create(basedir, "tracing", ".bin", maxFileSize, retentionCount)));
//    sink.sinkThread.setDaemon(true);
//    sink.sinkThread.setName("spm-tracing-sink-thread");
//    sink.sinkThread.start();
//    return sink;
//  }
//
//  @Override
//  public void sink(PartialTransaction transaction) {
//    throw new IllegalStateException("not implemented.");
//  }
}
