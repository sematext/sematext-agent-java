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
package com.sematext.spm.client.agent.profiler.cpu;

import java.util.Arrays;
import java.util.List;

public final class BlockingMethods {

  static class MethodInfo {
    private final String className;
    private final String methodName;
    private final boolean isNative;

    public MethodInfo(String className, String methodName, boolean isNative) {
      this.className = className;
      this.methodName = methodName;
      this.isNative = isNative;
    }
  }

  /**
   * List from netbeans profiler sources.
   * http://hg.netbeans.org/main/file/10004984f2f4/lib.profiler/src/org/netbeans/lib/profiler/results/cpu/StackTraceSnapshotBuilder.java#l70
   */
  private static final List<MethodInfo> KNOWN_BLOCKING_METHODS = Arrays.asList(
      new MethodInfo("java.net.PlainSocketImpl", "socketAccept", true),
      new MethodInfo("java.net.PlainSocketImpl", "socketAccept(java.net.SocketImpl) : void", true),
      new MethodInfo("sun.awt.windows.WToolkit", "eventLoop", true),
      new MethodInfo("sun.awt.windows.WToolkit", "eventLoop() : void", true),
      new MethodInfo("java.lang.UNIXProcess", "waitForProcessExit", true),
      new MethodInfo("java.lang.UNIXProcess", "waitForProcessExit(int) : int", true),
      new MethodInfo("sun.awt.X11.XToolkit", "waitForEvents", true),
      new MethodInfo("sun.awt.X11.XToolkit", "waitForEvents(long) : void", true),
      new MethodInfo("apple.awt.CToolkit", "doAWTRunLoop", true),
      new MethodInfo("apple.awt.CToolkit", "doAWTRunLoop(long, boolean, boolean) : void", true),
      new MethodInfo("java.lang.Object", "wait", true),
      new MethodInfo("java.lang.Object", "wait(long) : void", true),
      new MethodInfo("java.lang.Thread", "sleep", true),
      new MethodInfo("java.lang.Thread", "sleep(long) : void", true),
      new MethodInfo("sun.net.dns.ResolverConfigurationImpl", "notifyAddrChange0", true),
      new MethodInfo("sun.net.dns.ResolverConfigurationImpl", "notifyAddrChange0() : int", true),
      new MethodInfo("java.lang.ProcessImpl", "waitFor", true),
      new MethodInfo("java.lang.ProcessImpl", "waitFor() : int", true),
      new MethodInfo("sun.nio.ch.EPollArrayWrapper", "epollWait", true),
      new MethodInfo("sun.nio.ch.EPollArrayWrapper", "epollWait(long, int, long, int) : int", true),
      new MethodInfo("java.net.DualStackPlainSocketImpl", "accept0", true),
      new MethodInfo("java.net.DualStackPlainSocketImpl", "accept0(int, java.net.InetSocketAddress[]) : int", true),
      new MethodInfo("java.lang.ProcessImpl", "waitForInterruptibly", true),
      new MethodInfo("java.lang.ProcessImpl", "waitForInterruptibly(long) : void", true),
      new MethodInfo("sun.print.Win32PrintServiceLookup", "notifyPrinterChange", true),
      new MethodInfo("sun.print.Win32PrintServiceLookup", "notifyPrinterChange(long) : int", true),
      new MethodInfo("java.net.DualStackPlainSocketImpl", "waitForConnect", true),
      new MethodInfo("java.net.DualStackPlainSocketImpl", "waitForConnect(int, int) : void", true),
      new MethodInfo("sun.nio.ch.KQueueArrayWrapper", "kevent0", true),
      new MethodInfo("sun.nio.ch.KQueueArrayWrapper", "kevent0(int, long, int, long) : int", true),
      new MethodInfo("sun.nio.ch.WindowsSelectorImpl$SubSelector", "poll0", true),
      new MethodInfo("sun.nio.ch.WindowsSelectorImpl$SubSelector", "poll0(long, int, int[], int[], int[], long) : int", true)
  );

  private BlockingMethods() {
  }

  public static boolean isThreadBlocked(StackTraceElement[] stackTrace) {
    if (stackTrace.length == 0) {
      return false;
    }
    final StackTraceElement topCall = stackTrace[0];
    for (final MethodInfo info : KNOWN_BLOCKING_METHODS) {
      if (info.className.equals(topCall.getClassName()) && info.methodName.equals(topCall.getMethodName())
          && info.isNative == topCall.isNativeMethod()) {
        return true;
      }
    }
    return false;
  }
}
