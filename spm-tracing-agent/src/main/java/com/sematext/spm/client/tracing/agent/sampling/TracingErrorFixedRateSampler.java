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
package com.sematext.spm.client.tracing.agent.sampling;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.sematext.spm.client.tracing.agent.errors.TracingError;
import com.sematext.spm.client.util.Clock;
import com.sematext.spm.client.util.Clocks;

public final class TracingErrorFixedRateSampler implements Sampler<TracingError> {

  private static class State {
    final long startIntervalTsMs;
    final long events;

    public State(long startIntervalTsMs, long events) {
      this.startIntervalTsMs = startIntervalTsMs;
      this.events = events;
    }
  }

  private final long maxEventsCount;
  private final long intervalMillis;
  private final Clock clock;
  private final AtomicReference<State> state = new AtomicReference<State>(null);

  public TracingErrorFixedRateSampler(long maxEventsCount, long interval, TimeUnit intervalTU, Clock clock) {
    this.maxEventsCount = maxEventsCount;
    this.intervalMillis = intervalTU.toMillis(interval);
    this.clock = clock;
  }

  public TracingErrorFixedRateSampler(long maxEventsCount, long interval, TimeUnit intervalTU) {
    this(maxEventsCount, interval, intervalTU, Clocks.WALL);
  }

  @Override
  public boolean sample(TracingError event) {
    State currentState;
    State nextState;
    boolean sample;

    do {
      sample = false;
      currentState = state.get();

      if (currentState == null) {
        nextState = new State(clock.now(), 1);
        sample = true;
      } else if (clock.now() - currentState.startIntervalTsMs >= intervalMillis) {
        nextState = new State(clock.now(), 1);
        sample = true;
      } else if (currentState.events < maxEventsCount) {
        nextState = new State(currentState.startIntervalTsMs, currentState.events + 1);
        sample = true;
      } else {
        nextState = currentState;
      }
    } while (!state.compareAndSet(currentState, nextState));

    return sample;
  }
}
