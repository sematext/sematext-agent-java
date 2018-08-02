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
package com.sematext.spm.client.unlogger.dispatch;

import com.sematext.spm.client.unlogger.JoinPoint;

public final class LastInchOfTrampoline {

  private LastInchOfTrampoline() {

  }

  // Now, we use a simple as possible strategy
  // of dispatcher replacement. - We simple replace it!
  // Potentially it may cause errors, i.e when we enable/disable
  // dispatcher in the middle of chain of calls.
  // But more sophisticated way - wait for each thread
  // when call chain is finished and switch dispatcher
  // to each thread only for new chain is hard to implement
  // and potentially we can get a situation when chain is never ended
  // (for example long scanning daemon with pointcut on init method)
  // Also "volatile" here can add additional overheads.
  private static volatile AdviceDispatcher dispatcher = AdviceDispatcher.Type.NO_OP.make();

  // thunk pointed here
  public static void logBefore(int dispatchId, JoinPoint joinPoint, Object that, Object[] params) {
    dispatcher.logBefore(dispatchId, joinPoint, that, params);
  }

  // thunk pointed here
  public static void logAfter(int dispatchId, JoinPoint joinPoint, Object that, Object returnValue) {
    dispatcher.logAfter(dispatchId, joinPoint, that, returnValue);
  }

  // thunk pointed here
  public static void logThrow(int dispatchId, JoinPoint joinPoint, Object that, Throwable throwable) {
    dispatcher.logThrow(dispatchId, joinPoint, that, throwable);
  }

  public static synchronized AdviceDispatcher switchTo(AdviceDispatcher.Type type) {
    AdviceDispatcher dispatcher = type.make();
    LastInchOfTrampoline.dispatcher = dispatcher;
    return dispatcher;
  }

  public static synchronized AdviceDispatcher switchToIfNeeded(AdviceDispatcher.Type type) {
    return (dispatcher.getType() == type) ? dispatcher : switchTo(type);
  }

}
