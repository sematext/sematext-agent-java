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

public final class CallArray {
  private Call[] array;
  private int n = 0;

  public CallArray(int initialSize) {
    array = new Call[initialSize];

    for (int i = 0; i < initialSize; i++) {
      array[i] = new Call();
    }
  }

  public Call add() {
    resizeIfNeed();
    return array[n++];
  }

  private void resizeIfNeed() {
    if (n == array.length) {
      final Call[] resized = new Call[array.length * 2];
      System.arraycopy(array, 0, resized, 0, array.length);
      for (int i = array.length; i < resized.length; i++) {
        resized[i] = new Call();
      }

      array = resized;
    }
  }

  public void clean() {
    n = 0;
  }

  public int size() {
    return n;
  }

  public Call get(int i) {
    if (i < 0 || i >= n) {
      throw new IllegalArgumentException("index should be >= 0 and < " + n + ".");
    }
    return array[i];
  }

}
