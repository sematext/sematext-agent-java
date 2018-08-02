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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.tracing.agent.errors.TracingError;
import com.sematext.spm.client.util.Clocks;
import com.sematext.spm.client.util.Clocks.Mock;

public class TracingErrorFixedRateSamplerTest {

  private final TracingError error = new TracingError();

  @Test
  public void testSample() throws Exception {
    final Mock clock = Clocks.mock();
    final TracingErrorFixedRateSampler sampler = new TracingErrorFixedRateSampler(4, 10, TimeUnit.MILLISECONDS, clock);

    for (int i = 0; i < 4; i++) {
      assertTrue(sampler.sample(error));
      clock.increment();
    }

    assertFalse(sampler.sample(error));

    clock.set(10);

    for (int i = 0; i < 4; i++) {
      assertTrue(i + " event should be sampled", sampler.sample(error));
      clock.increment();
    }
  }

}
