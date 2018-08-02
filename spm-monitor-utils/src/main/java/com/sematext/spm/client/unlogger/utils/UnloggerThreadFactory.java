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
package com.sematext.spm.client.unlogger.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class UnloggerThreadFactory implements ThreadFactory {

  private final String prefix;

  private final AtomicInteger threadCounter = new AtomicInteger();
  private final ThreadGroup threadGroup;

  public UnloggerThreadFactory(String prefix) {
    this.prefix = prefix;

    SecurityManager s = System.getSecurityManager();
    threadGroup = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
  }

  @Override
  public Thread newThread(Runnable runnable) {
    String name = runnable instanceof NamedRunnable ? prefix + "-" + ((NamedRunnable) runnable).getName() : prefix;
    Thread thread = new Thread(threadGroup, runnable, name + "-" + threadCounter.incrementAndGet());
    thread.setDaemon(true);
    thread.setPriority(Thread.MIN_PRIORITY);
    return thread;
  }

  public interface NamedRunnable extends Runnable {
    String getName();
  }

}
