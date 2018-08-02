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
package com.sematext.spm.client.unlogger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.unlogger.pointcuts.UPointcutContext;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public final class WeaverUtils {

  private static final Log LOG = LogFactory.getLog(WeaverUtils.class);

  private WeaverUtils() {
    // It's utility class
  }

  public static Map<CtBehavior, Set<Pointcut>> computeAffectedBehaviours(Set<Pointcut> pointCuts, CtClass ctClass)
      throws NotFoundException {
    return computeAffectedBehaviours(pointCuts, ctClass, UnloggerTransformerFilter.ACCEPT_ALL);
  }

  public static Map<CtBehavior, Set<Pointcut>> computeAffectedBehaviours(Set<Pointcut> pointCuts, CtClass ctClass,
                                                                         UnloggerTransformerFilter filter)
      throws NotFoundException {
    Set<String> hierarchy = getClassesInHierarchy(ctClass);
    if (LOG.isTraceEnabled()) {
      LOG.trace("Computing behaviours for: " + ctClass + ". Hierarchy: " + hierarchy);
    }

    UPointcutContext ctx = new UPointcutContext(hierarchy);

    final Map<CtBehavior, Set<Pointcut>> affectedBehaviours = new HashMap<CtBehavior, Set<Pointcut>>();
    for (Pointcut pointcut : pointCuts) {
      final Set<CtBehavior> behaviours = new HashSet<CtBehavior>();
      pointcut.match(ctClass, ctx, behaviours);

      for (CtBehavior b : behaviours) {
        if (filter == null || filter.shouldBeTransformed(b)) {
          Set<Pointcut> pointcutForBeh = affectedBehaviours.get(b);
          if (pointcutForBeh == null) {
            pointcutForBeh = new LinkedHashSet<Pointcut>();
            affectedBehaviours.put(b, pointcutForBeh);
          }
          pointcutForBeh.add(pointcut);
        }
      }
    }
    return affectedBehaviours;
  }

  private static Set<Pointcut> narrowPointcutsByHierarchy(Set<Pointcut> pointCuts, Set<String> hierarchy) {
    Set<Pointcut> applicablePointcuts = new LinkedHashSet<Pointcut>();
    for (Pointcut pointCut : pointCuts) {
      if (hierarchy.contains(pointCut.getTypeName())) {
        applicablePointcuts.add(pointCut);
      }
    }

    return applicablePointcuts;
  }

  protected static Set<String> getClassesInHierarchy(CtClass ctClass) {
    Set<String> visitedClasses = new HashSet<String>();
    try {
      getClassesInHierarchy(ctClass, visitedClasses);
      return visitedClasses;
    } catch (Exception e) {
      throw new RuntimeException("Could't not compute hierarchy for -> " + ctClass, e);
    }
  }

  private static void getClassesInHierarchy(CtClass ctClass, Set<String> visitedClasses) throws NotFoundException {
    if (ctClass == null) {
      return;
    }
    if (visitedClasses.contains(ctClass)) {
      return;
    }
    visitedClasses.add(ctClass.getName());

    getClassesInHierarchy(ctClass.getSuperclass(), visitedClasses);

    for (CtClass implementedInterface : ctClass.getInterfaces()) {
      getClassesInHierarchy(implementedInterface, visitedClasses);
    }
  }

  public static boolean isPublic(CtMethod ctMethod) {
    return (ctMethod.getModifiers() & javassist.Modifier.PUBLIC) != 0;
  }

  public static boolean isStatic(CtMethod ctMethod) {
    return (ctMethod.getModifiers() & javassist.Modifier.STATIC) != 0;
  }

  public static boolean isAbstract(CtBehavior ctMethod) {
    return (ctMethod.getModifiers() & javassist.Modifier.ABSTRACT) != 0;
  }

  public static Object toObject(Object object) {
    return object;
  }

  public static Object toObject(boolean object) {
    return object;
  }

  public static Object toObject(int object) {
    return object;
  }

  public static Object toObject(long object) {
    return object;
  }

  public static Object toObject(byte object) {
    return object;
  }

  public static Object toObject(short object) {
    return object;
  }

  public static Object toObject(float object) {
    return object;
  }

  public static Object toObject(double object) {
    return object;
  }

  public static Object toObject(char object) {
    return object;
  }

}
