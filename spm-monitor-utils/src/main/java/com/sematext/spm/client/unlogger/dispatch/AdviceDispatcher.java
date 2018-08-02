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

public interface AdviceDispatcher {

  void logBefore(int dispatchId, JoinPoint joinPoint, Object that, Object[] params);

  void logAfter(int dispatchId, JoinPoint joinPoint, Object that, Object returnValue);

  void logThrow(int dispatchId, JoinPoint joinPoint, Object that, Throwable throwable);

  Type getType();

  public enum Type {
    DEFAULT() {
      @Override
      public AdviceDispatcher make() {
        return new DefaultAdviceDispatcher();
      }
    },
    NO_OP() {
      private final AdviceDispatcher noOp = new AdviceDispatcher() {
        @Override
        public void logThrow(int dispatchId, JoinPoint joinPoint, Object that, Throwable throwable) {
        }

        @Override
        public void logBefore(int dispatchId, JoinPoint joinPoint, Object that, Object[] params) {
        }

        @Override
        public void logAfter(int dispatchId, JoinPoint joinPoint, Object that, Object returnValue) {
        }

        @Override
        public Type getType() {
          return NO_OP;
        }

      };

      @Override
      public AdviceDispatcher make() {
        return noOp;
      }

    };

    public abstract AdviceDispatcher make();
  }

}
