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
package com.sematext.spm.client.unlogger.dynamic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sematext.spm.client.unlogger.ConstructorPointcut;
import com.sematext.spm.client.unlogger.MethodPointcut;

import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;

public class BehaviorDescriptionTest {
  @Test
  public void fromBehaviour() throws Exception {
    ClassPool cp = ClassPool.getDefault();
    CtClass arrayListClass = cp.get("java.util.ArrayList");

    CtBehavior ctor0 = arrayListClass.getDeclaredConstructor(new CtClass[0]);
    assertEquals(new BehaviorDescription("java.util.ArrayList()"), BehaviorDescription.fromBehaviour(ctor0));

    CtBehavior ctor1 = arrayListClass.getDeclaredConstructor(new CtClass[] { cp.get("int") });
    assertEquals(new BehaviorDescription("java.util.ArrayList(int)"), BehaviorDescription.fromBehaviour(ctor1));

    CtBehavior ctor2 = arrayListClass.getDeclaredConstructor(new CtClass[] { cp.get("java.util.Collection") });
    assertEquals(new BehaviorDescription("java.util.ArrayList(java.util.Collection)"), BehaviorDescription
        .fromBehaviour(ctor2));

    CtBehavior add1 = arrayListClass.getDeclaredMethod("add", new CtClass[] { cp.get("java.lang.Object") });
    assertEquals(new BehaviorDescription("boolean java.util.ArrayList#add(java.lang.Object)"), BehaviorDescription
        .fromBehaviour(add1));

    CtBehavior add2 = arrayListClass
        .getDeclaredMethod("add", new CtClass[] { cp.get("int"), cp.get("java.lang.Object") });
    assertEquals(new BehaviorDescription("void java.util.ArrayList#add(int,java.lang.Object)"), BehaviorDescription
        .fromBehaviour(add2));

    CtBehavior toArray = arrayListClass.getDeclaredMethod("toArray", new CtClass[0]);
    assertEquals(new BehaviorDescription("java.lang.Object[] java.util.ArrayList#toArray()"), BehaviorDescription
        .fromBehaviour(toArray));
  }

  @Test
  public void toPointcut() throws Exception {
    assertEquals(MethodPointcut.FACTORY.make("void java.util.ArrayList#add(int p0,java.lang.Object p1)"),
                 new BehaviorDescription("void java.util.ArrayList#add(int,java.lang.Object)").toPointcut());

    assertEquals(MethodPointcut.FACTORY.make("java.lang.Object[] java.util.ArrayList#toArray()"),
                 new BehaviorDescription("java.lang.Object[] java.util.ArrayList#toArray()").toPointcut());

    assertEquals(ConstructorPointcut.FACTORY.make("java.util.ArrayList(int p0)"),
                 new BehaviorDescription("java.util.ArrayList(int)").toPointcut());
  }

}
