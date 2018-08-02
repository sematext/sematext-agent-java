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

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;
import com.sematext.spm.client.util.Tuple;

public final class CallDebug {

  public static String print(List<Call> calls) {
    StringBuilder tree = new StringBuilder();
    for (Call call : calls) {
      if (call.getParentCallId() == Call.ROOT_CALL_ID) {
        tree.append(printCallTree(calls, call)).append("\n");
      }
    }
    return tree.toString();
  }

  public static String printCallTree(List<PartialTransaction> transactions) {
    final List<Call> calls = new ArrayList<Call>();
    for (PartialTransaction transaction : transactions) {
      for (Call call : transaction.getCalls()) {
        calls.add(call);
      }
    }
    return print(calls);
  }

  private static String printCallTree(List<Call> calls, Call rootCall) {
    final ArrayListMultimap<Long, Call> callsByParentId = ArrayListMultimap.create();
    for (final Call call : calls) {
      callsByParentId.put(call.getParentCallId(), call);
    }

    ArrayDeque<Tuple<Integer, Call>> deque = new ArrayDeque<Tuple<Integer, Call>>();
    deque.add(Tuple.tuple(0, rootCall));

    final StringBuilder builder = new StringBuilder();

    while (!deque.isEmpty()) {
      final Tuple<Integer, Call> callAndIndent = deque.poll();
      final int indent = callAndIndent.getFirst();
      final Call call = callAndIndent.getSecond();

      builder.append(Strings.repeat(" ", indent))
          .append(" ")
          .append(call.getSignature())
          .append(" - ")
          .append(call.getDuration())
          .append("ms")
          .append("\n");

      for (Call child : callsByParentId.get(call.getCallId())) {
        deque.push(Tuple.tuple(indent + 1, child));
      }
    }

    return builder.toString();
  }
}
