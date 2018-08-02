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

package com.sematext.spm.client;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class PriorityThreadFactory implements ThreadFactory {
  private static final Log LOG = LogFactory.getLog(PriorityThreadFactory.class);
  private final ThreadFactory threadFactory;
  private final int priority;
  private final String name;
  private final AtomicInteger idGenerator = new AtomicInteger();

  public PriorityThreadFactory(ThreadFactory threadFactory, String name, int priority) {
    this.threadFactory = threadFactory;
    this.priority = priority;
    this.name = name;
  }

  @Override
  public Thread newThread(Runnable runnable) {
    Thread thread = threadFactory.newThread(runnable);
    thread.setName(name + "-" + idGenerator.incrementAndGet() + "-thread");
    thread.setPriority(priority);

    thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        try {
          LOG.error("Uncaught Exception", e);
        } catch (Throwable thr) {
          System.err
              .println("com.sematext.spm.client.MonitorAgent ERROR: error while writing following UNCAUGHT error: '" +
                           e.getMessage() + "' into monitor log. Error produced by writing was: " + thr.getMessage());
        }
      }
    });

    // in-process setup shouldn't interfere with regular start/stop procedure of host process, so we have to
    // mark monitor threads as daemon threads; on the other hand, that presents a problem for standalone monitor,
    // since JVM automatically exits if only daemon threads are left running. So, we need different setting
    if (MonitorUtil.MONITOR_RUNTIME_SETUP_JAVAAGENT.get()) {
      thread.setDaemon(true);
    } else {
      thread.setDaemon(false);
    }

    return thread;
  }
}
