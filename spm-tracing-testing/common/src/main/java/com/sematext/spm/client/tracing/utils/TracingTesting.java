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
package com.sematext.spm.client.tracing.utils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

import com.sematext.spm.client.tracing.agent.config.ServiceLocator;
import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;

public final class TracingTesting {

  private TracingTesting() {
  }

  public static Iterable<Call> getCalls(Collection<? extends PartialTransaction> transactions) {
    final Iterable<List<Call>> calls = Iterables
        .transform(transactions, new Function<PartialTransaction, List<Call>>() {
          @Override
          public List<Call> apply(PartialTransaction transaction) {
            return transaction.getCalls();
          }
        });
    return Iterables.concat(calls);
  }

  public static List<Call> findMatching(final String regexp, List<PartialTransaction> transactions) {
    final Iterable<Call> filtered = Iterables.filter(getCalls(transactions), new Predicate<Call>() {
      @Override
      public boolean apply(Call call) {
        return call.getSignature().matches(regexp);
      }
    });
    return Lists.newArrayList(filtered);
  }

  public static List<PartialTransaction> findAsync(List<PartialTransaction> transactions) {
    Iterable<PartialTransaction> filtered = Iterables.filter(transactions, new Predicate<PartialTransaction>() {
      @Override
      public boolean apply(PartialTransaction input) {
        return input.isAsynchronous();
      }
    });
    return Lists.newArrayList(filtered);
  }

  public static MockTransactionSink setupSink() {
    MockTransactionSink sink = new MockTransactionSink();
    ServiceLocator.getTransactionSinks().clear();
    ServiceLocator.getTransactionSinks().add(sink);
    return sink;
  }

}
