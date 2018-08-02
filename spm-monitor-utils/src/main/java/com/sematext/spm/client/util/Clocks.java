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
package com.sematext.spm.client.util;

public final class Clocks {
  private Clocks() {
  }

  public static final Clock WALL = new Clock() {
    @Override
    public long now() {
      return System.currentTimeMillis();
    }
  };

  public static class Mock implements Clock {
    private long time = 0;

    public void increment() {
      time++;
    }

    public void increment(long v) {
      time += v;
    }

    public void set(long time) {
      this.time = time;
    }

    @Override
    public long now() {
      return time;
    }
  }

  public static Mock mock() {
    return new Mock();
  }
}
