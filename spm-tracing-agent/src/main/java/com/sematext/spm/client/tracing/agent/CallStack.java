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
package com.sematext.spm.client.tracing.agent;

import com.sematext.spm.client.tracing.agent.model.Call;

public final class CallStack {
  private Call[] calls;
  private int n = 0;

  public CallStack(int size) {
    this.calls = new Call[size];
    for (int i = 0; i < size; i++) {
      this.calls[i] = new Call();
    }
  }

  public Call push() {
    if (n == calls.length) {
      return null;
    }
    return calls[n++];
  }

  public Call pop() {
    if (n == 0) {
      return null;
    }
    return calls[--n];
  }

  public Call peek() {
    if (n == 0) {
      return null;
    }
    return calls[n - 1];
  }

  public void clear() {
    n = 0;
  }

  public int size() {
    return n;
  }

  public boolean isEmpty() {
    return n == 0;
  }
}
