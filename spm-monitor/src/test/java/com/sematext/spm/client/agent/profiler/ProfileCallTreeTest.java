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

import static com.sematext.spm.client.util.StringUtils.join;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ProfileCallTreeTest {

  private static StackTraceElement[] stackTrace(String... names) {
    List<StackTraceElement> elements = new ArrayList<StackTraceElement>();
    for (int i = names.length - 1; i >= 0; i--) {
      final String n = names[i];
      elements.add(new StackTraceElement(n, n, n + ".java", 100));
    }
    return elements.toArray(new StackTraceElement[names.length]);
  }

  private static String toMatchString(ProfileCallTree tree) {
    return toMatchString(tree.root);
  }

  private static String toMatchString(ProfileCallTree.Node node) {
    final List<String> childrenMatches = new ArrayList<String>();
    for (ProfileCallTree.Node child : node.children) {
      childrenMatches.add(toMatchString(child));
    }
    return (node.element == null ? "" : node.element.getClassName()) + "(" + join(childrenMatches, ",") + ")";
  }

  @Test
  public void testInitialMerge() {
    ProfileCallTree tree = new ProfileCallTree();
    tree.merge(stackTrace("A", "B", "C"));

    assertEquals(toMatchString(tree), "(A(B(C())))");
  }

  @Test
  public void testMergeNonEmpty() {
    ProfileCallTree tree = new ProfileCallTree();
    tree.merge(stackTrace("A", "B", "C"));
    tree.merge(stackTrace("B", "C"));

    assertEquals(toMatchString(tree), "(A(B(C())),B(C()))");

    tree.merge(stackTrace("A", "B", "B"));

    assertEquals(toMatchString(tree), "(A(B(C(),B())),B(C()))");
  }
}
