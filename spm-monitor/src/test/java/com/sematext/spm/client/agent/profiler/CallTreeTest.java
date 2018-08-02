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

import static org.junit.Assert.assertEquals;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Iterator;
import java.util.StringTokenizer;

import com.sematext.spm.client.monitor.thrift.TCallNode;
import com.sematext.spm.client.monitor.thrift.TCallTree;

@RunWith(DataProviderRunner.class)
public class CallTreeTest {

  static void buildTree(CallTree.Node node, StringTokenizer tokenizer) {
    while (tokenizer.hasMoreElements()) {
      String token = tokenizer.nextToken();
      if (token.equals("(")) {
        CallTree.Node child = node.getChildren().get(node.getChildren().size() - 1);
        buildTree(child, tokenizer);
      } else if (token.equals(")")) {
        return;
      } else if (token.equals(",")) {
        continue;
      } else {
        node.addChild(new CallTree.Node(new StackTraceElement(token, "", "", -1), -1, -1, -1, -1));
      }
    }
  }

  static CallTree tree(String expr) {
    CallTree tree = new CallTree();
    StringTokenizer tokenizer = new StringTokenizer(expr, "(),", true);
    if (!tokenizer.nextToken().equals("(")) {
      throw new IllegalStateException();
    }

    buildTree(tree.getRoot(), tokenizer);
    return tree;
  }

  static void expr(CallTree.Node node, StringBuilder builder) {
    if (node.getElement() != null) {
      builder.append(node.getElement().getClassName());
    }
    if (!node.getChildren().isEmpty()) {
      builder.append("(");
    }
    Iterator<CallTree.Node> iter = node.getChildren().iterator();
    while (iter.hasNext()) {
      expr(iter.next(), builder);
      if (iter.hasNext()) {
        builder.append(",");
      }
    }
    if (!node.getChildren().isEmpty()) {
      builder.append(")");
    }
  }

  static String expr(CallTree tree) {
    StringBuilder builder = new StringBuilder();
    expr(tree.getRoot(), builder);
    return builder.toString();
  }

  @DataProvider
  public static Object[][] mergeDataProvider() {
    return new Object[][] {
        new Object[] { "(a1(a2,a3))", "(a1(a3))", "(a1(a2,a3))" },
        new Object[] { "(a1(a2(a3)))", "(a1(a3))", "(a1(a2(a3),a3))" },
    };
  }

  @Test
  @UseDataProvider("mergeDataProvider")
  public void testMerge(String tree1, String tree2, String expected) {
    CallTree merged = CallTree.merge(Arrays.asList(tree(tree1), tree(tree2)));
    assertEquals(expr(merged), expected);
  }

  @DataProvider
  public static Object[][] serDeInvariantDataProvider() {
    return new Object[][] {
        new Object[] { "(a1(a2(a3,a4,a5),a6))" }
    };
  }

  @Test
  @UseDataProvider("serDeInvariantDataProvider")
  public void testThriftSerDeInvariant(String expr) {
    assertEquals(expr(CallTree.fromThrift(ProfileSnapshotThriftSerializer.toThrift(tree(expr)))), expr);
  }

  @Test
  public void testSerDe() {
    CallTree orig = new CallTree();
    CallTree.Node node1 = new CallTree.Node(new StackTraceElement("a", "", "", -1), 1, 1, 1, 1);
    CallTree.Node node2 = new CallTree.Node(new StackTraceElement("b", "", "", -1), 2, 2, 2, 2);
    orig.getRoot().addChild(node1);
    node1.addChild(node2);

    TCallTree thrift = ProfileSnapshotThriftSerializer.toThrift(orig);
    for (TCallNode thriftNode : thrift.getNodes()) {
      if (thriftNode.getDeclaringClass() != null && thriftNode.getDeclaringClass().equals("a")) {
        assertEquals(thriftNode.getCpuTime(), 1);
        assertEquals(thriftNode.getUserCPUTime(), 1);
        assertEquals(thriftNode.getTime(), 1);
        assertEquals(thriftNode.getSamples(), 1);
      } else if (thriftNode.getDeclaringClass() != null && thriftNode.getDeclaringClass().equals("b")) {
        assertEquals(thriftNode.getCpuTime(), 2);
        assertEquals(thriftNode.getUserCPUTime(), 2);
        assertEquals(thriftNode.getTime(), 2);
        assertEquals(thriftNode.getSamples(), 2);
      }
    }

    CallTree deser = CallTree.fromThrift(thrift);

    CallTree.Node node1Deser = deser.getRoot().getChildren().get(0);
    assertEquals(node1Deser.getElement().getClassName(), "a");
    assertEquals(node1Deser.getCpuTime(), 1);
    assertEquals(node1Deser.getUserCpuTime(), 1);
    assertEquals(node1Deser.getTime(), 1);
    assertEquals(node1Deser.getSamples(), 1);

    CallTree.Node node2Deser = deser.getRoot().getChildren().get(0).getChildren().get(0);
    assertEquals(node2Deser.getElement().getClassName(), "b");
    assertEquals(node2Deser.getCpuTime(), 2);
    assertEquals(node2Deser.getUserCpuTime(), 2);
    assertEquals(node2Deser.getTime(), 2);
    assertEquals(node2Deser.getSamples(), 2);
  }
}
