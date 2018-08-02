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
package com.sematext.spm.client.tracing.agent.stats;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.util.TTLCache;

public abstract class Statistics<ID, M extends Statistics.Metric<ID>> {
  public static abstract class Metric<ID> {
    private final ID id;

    public Metric(ID id) {
      this.id = id;
    }

    public ID getId() {
      return id;
    }

    protected abstract void update(PartialTransaction transaction);

    protected abstract void update(PartialTransaction transaction, Call call);
  }

  private final TTLCache<ID, M> metrics = new TTLCache<ID, M>(TimeUnit.MINUTES.toMillis(2));

  protected abstract M newMetric(ID id);

  private M getOrCreateMetric(ID id) {
    M metric = metrics.get(id);
    if (metric == null) {
      metric = newMetric(id);
      M existing = metrics.putIfAbsent(id, metric);
      if (existing != null) {
        metric = existing;
      }
    } else {
      metrics.touch(id);
    }
    return metric;
  }

  public final void update(ID id, PartialTransaction transaction, Call call) {
    getOrCreateMetric(id).update(transaction, call);
  }

  public final void update(ID id, PartialTransaction transaction) {
    getOrCreateMetric(id).update(transaction);
  }

  public final Collection<M> values() {
    return metrics.values();
  }
}
