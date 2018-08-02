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
package com.sematext.spm.client.agent.profiler;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.agent.profiler.CallTree.Node;
import com.sematext.spm.client.agent.profiler.cpu.AllThreadsProfileSnapshot;
import com.sematext.spm.client.monitor.thrift.TAllThreadsProfileSnapshot;
import com.sematext.spm.client.monitor.thrift.TCallNode;
import com.sematext.spm.client.monitor.thrift.TCallTree;

public final class ProfileSnapshotThriftSerializer {
  private ProfileSnapshotThriftSerializer() {
  }

  public static TAllThreadsProfileSnapshot toThrift(AllThreadsProfileSnapshot snapshot) {
    TAllThreadsProfileSnapshot thrift = new TAllThreadsProfileSnapshot();
    thrift.setTree(toThrift(snapshot.getTree()));
    thrift.setTime((int) snapshot.getTime());
    thrift.setCpuTime((int) snapshot.getCpuTime());
    thrift.setUserCPUTime((int) snapshot.getUserCpuTime());
    thrift.setSamples((int) snapshot.getSamples());
    thrift.setWaitedTime((int) snapshot.getWaitedTime());
    thrift.setBlockedTime((int) snapshot.getBlockedTime());
    thrift.setGcTime((int) snapshot.getGcTime());
    thrift.setCpuTimeSupported(snapshot.isCpuTimeSupported());
    return thrift;
  }

  public static TCallTree toThrift(CallTree callTree) {
    List<TCallNode> thriftCallNodes = new FastList<TCallNode>();
    ArrayDeque<Node> queue = new ArrayDeque<Node>();

    int id = 0;
    Map<Node, Integer> ids = new UnifiedMap<Node, Integer>();
    ids.put(callTree.getRoot(), -1);

    queue.add(callTree.getRoot());

    while (!queue.isEmpty()) {
      Node node = queue.pop();

      TCallNode thriftNode = new TCallNode();
      List<Integer> children = new FastList<Integer>();
      for (Node child : node.getChildren()) {
        int childId = id++;
        queue.add(child);
        children.add(childId);
        ids.put(child, childId);
      }

      thriftNode.setId(ids.get(node));
      thriftNode.setChildren(children);
      if (node != callTree.getRoot()) {
        thriftNode.setDeclaringClass(node.getElement().getClassName());
        thriftNode.setMethodName(node.getElement().getMethodName());
        thriftNode.setLineNumber(node.getElement().getLineNumber());
        thriftNode.setFileName(node.getElement().getFileName());
        thriftNode.setSamples((int) node.getSamples());
        thriftNode.setTime(node.getTime());
        thriftNode.setCpuTime(node.getCpuTime());
        thriftNode.setUserCPUTime(node.getUserCpuTime());
      }

      thriftCallNodes.add(thriftNode);
    }
    return new TCallTree(ids.get(callTree.getRoot()), thriftCallNodes);
  }
}
