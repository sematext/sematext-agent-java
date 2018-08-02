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

import java.util.List;

public final class ProfileCallTree {

  public static class Node {
    final List<Node> children;
    final StackTraceElement element;
    int calls = 1;

    public Node(List<Node> children, StackTraceElement element, int calls) {
      this.children = children;
      this.element = element;
      this.calls = calls;
    }

    Node(StackTraceElement element) {
      this(new FastList<Node>(), element, 0);
    }

    public List<Node> getChildren() {
      return children;
    }

    public StackTraceElement getElement() {
      return element;
    }

    public int getCalls() {
      return calls;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Node node = (Node) o;

      if (calls != node.calls) return false;
      if (children != null ? !children.equals(node.children) : node.children != null) return false;
      if (element != null ? !element.equals(node.element) : node.element != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = children != null ? children.hashCode() : 0;
      result = 31 * result + (element != null ? element.hashCode() : 0);
      result = 31 * result + calls;
      return result;
    }

    @Override
    public String toString() {
      return "Node{" +
          "children=" + children +
          ", element=" + element +
          ", calls=" + calls +
          '}';
    }
  }

  final Node root;

  public ProfileCallTree(Node root) {
    this.root = root;
  }

  public ProfileCallTree() {
    this(new Node(null));
  }

  void merge(StackTraceElement[] stackTrace) {
    int i = stackTrace.length - 1;
    Node parent = root;

    while (i >= 0) {
      Node existing = null;
      for (Node node : parent.children) {
        if (node.element.equals(stackTrace[i])) {
          existing = node;
          break;
        }
      }

      if (existing != null) {
        existing.calls++;
        i--;
        parent = existing;
      } else {
        break;
      }
    }

    while (i >= 0) {
      Node node = new Node(new FastList<Node>(), stackTrace[i], 1);
      parent.children.add(node);
      parent = node;
      i--;
    }
  }

  public Node getRoot() {
    return root;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ProfileCallTree that = (ProfileCallTree) o;

    if (root != null ? !root.equals(that.root) : that.root != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return root != null ? root.hashCode() : 0;
  }

  public static ProfileCallTree build(List<StackTraceElement[]> stackTraces) {
    final ProfileCallTree tree = new ProfileCallTree();
    for (final StackTraceElement[] trace : stackTraces) {
      tree.merge(trace);
    }
    return tree;
  }

  @Override
  public String toString() {
    return "ProfileCallTree{" +
        "root=" + root +
        '}';
  }
}
