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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.sematext.spm.client.monitor.thrift.TCallNode;
import com.sematext.spm.client.monitor.thrift.TCallTree;

public final class CallTree {
  public static class Node {
    private final StackTraceElement element;
    private long samples;
    private long userCpuTime;
    private long cpuTime;
    private long time;
    private final List<Node> children = new ArrayList<Node>();

    public Node(StackTraceElement element, long samples, long userCpuTime, long cpuTime, long time) {
      this.element = element;
      this.samples = samples;
      this.userCpuTime = userCpuTime;
      this.cpuTime = cpuTime;
      this.time = time;
    }

    public StackTraceElement getElement() {
      return element;
    }

    public long getSamples() {
      return samples;
    }

    public long getUserCpuTime() {
      return userCpuTime;
    }

    public long getCpuTime() {
      return cpuTime;
    }

    public long getTime() {
      return time;
    }

    public List<Node> getChildren() {
      return children;
    }

    public void addChild(Node node) {
      children.add(node);
    }

    public void incrSamples(int i) {
      samples += i;
    }

    public void incrTime(long i) {
      time += i;
    }

    public void incrCPUTime(long i) {
      cpuTime += i;
    }

    public void incrUserCPUTime(long i) {
      userCpuTime += i;
    }

    private void merge(Node that) {
      samples += that.samples;
      cpuTime += that.cpuTime;
      userCpuTime += that.userCpuTime;
      time += that.time;

      for (Node thatChild : that.children) {
        Node existing = findChild(thatChild.element);
        if (existing != null) {
          existing.merge(thatChild);
        } else {
          addChild(thatChild);
        }
      }
    }

    public Node findChild(StackTraceElement element) {
      for (Node child : children) {
        if (child.element.equals(element)) {
          return child;
        }
      }
      return null;
    }
  }

  private final Node root = new Node(null, -1, -1, -1, -1);

  public Node getRoot() {
    return root;
  }

  private void dumpRec(int depth, Node node, StringBuilder builder) {
    if (node == root) {
      builder.append("<root>").append("\n");
    } else {
      for (int i = 0; i < depth; i++) {
        builder.append(" ");
      }
      builder.append("[CPU: ").append(TimeUnit.NANOSECONDS.toMillis(node.cpuTime)).append("ms");
      builder.append(", User CPU:").append(TimeUnit.NANOSECONDS.toMillis(node.userCpuTime)).append("ms");
      builder.append(", Time:").append(TimeUnit.NANOSECONDS.toMillis(node.time)).append("ms");
      builder.append(", Samples:").append(node.samples).append("] ");
      builder.append(node.element.getClassName()).append("#").append(node.element.getMethodName()).append("\n");
    }
    for (Node child : node.children) {
      dumpRec(depth + 1, child, builder);
    }
  }

  public String dump() {
    StringBuilder builder = new StringBuilder();
    dumpRec(0, root, builder);
    return builder.toString();
  }

  private void merge(CallTree tree) {
    root.merge(tree.getRoot());
  }

  List<Node> getNodes() {
    List<Node> nodes = new ArrayList<Node>();
    ArrayDeque<Node> queue = new ArrayDeque<Node>();
    queue.add(root);

    while (!queue.isEmpty()) {
      Node node = queue.pop();
      nodes.add(node);

      for (Node child : node.getChildren()) {
        queue.add(child);
      }
    }
    return nodes;
  }

  public static CallTree merge(Iterable<CallTree> trees) {
    final CallTree merged = new CallTree();
    for (CallTree tree : trees) {
      merged.merge(tree);
    }
    return merged;
  }

  public static CallTree fromThrift(TCallTree tCallTree) {
    CallTree callTree = new CallTree();

    Map<Integer, TCallNode> tCallNodes = new HashMap<Integer, TCallNode>();
    for (TCallNode node : tCallTree.getNodes()) {
      tCallNodes.put(node.getId(), node);
    }

    Map<Integer, Node> callNodes = new HashMap<Integer, Node>();

    ArrayDeque<TCallNode> tNodesQueue = new ArrayDeque<TCallNode>();
    tNodesQueue.add(tCallNodes.get(tCallTree.getRootNodeId()));

    while (!tNodesQueue.isEmpty()) {
      TCallNode tNode = tNodesQueue.pop();
      if (tNode.getId() == tCallTree.getRootNodeId()) {
        callNodes.put(tCallTree.getRootNodeId(), callTree.getRoot());
      } else {
        StackTraceElement element = new StackTraceElement(tNode.getDeclaringClass(),
                                                          tNode.getMethodName() == null ?
                                                              "" :
                                                              tNode.getMethodName(), tNode.getFileName(), tNode
                                                              .getLineNumber());
        Node node = new Node(element, tNode.getSamples(), tNode.getUserCPUTime(), tNode.getCpuTime(), tNode.getTime());
        callNodes.put(tNode.getId(), node);
      }

      for (Integer childId : tNode.getChildren()) {
        tNodesQueue.add(tCallNodes.get(childId));
      }
    }

    tNodesQueue.clear();

    tNodesQueue.add(tCallNodes.get(tCallTree.getRootNodeId()));

    while (!tNodesQueue.isEmpty()) {
      TCallNode tNode = tNodesQueue.pop();
      Node node = callNodes.get(tNode.getId());

      for (Integer childId : tNode.getChildren()) {
        node.addChild(callNodes.get(childId));
        tNodesQueue.add(tCallNodes.get(childId));
      }
    }

    return callTree;
  }
}
