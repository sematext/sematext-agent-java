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
package com.sematext.spm.client.unlogger.weaver;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.unlogger.Pointcut;
import com.sematext.spm.client.unlogger.UnloggerTransformerFilter;
import com.sematext.spm.client.unlogger.WeaverUtils;
import com.sematext.spm.client.util.ClassPools;
import com.sematext.spm.client.util.StringInterpolator;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Weaver that respects priority for same pointcuts.
 */
public final class PriorityWeaver {

  private static final Log LOG = LogFactory.getLog(PriorityWeaver.class);
  private static final String RESULT_CONVERSION = WeaverUtils.class.getName() + ".toObject";

  private final Map<Pointcut, Integer> firstPriorityPointcutToDispatchId;
  private final Map<Pointcut, Integer> secondPriorityPointcutToDispatchId;
  private final String thunkClassName;

  public PriorityWeaver(Map<Pointcut, Integer> firstPriorityPointcutToDispatchId,
                        Map<Pointcut, Integer> secondPriorityPointcutToDispatchId, String thunkClassName) {
    this.firstPriorityPointcutToDispatchId = firstPriorityPointcutToDispatchId;
    this.secondPriorityPointcutToDispatchId = secondPriorityPointcutToDispatchId;
    this.thunkClassName = thunkClassName;
  }

  public byte[] processClassfileBuffer(ClassLoader loader, byte[] classFileBuffer, UnloggerTransformerFilter filter)
      throws Exception {
    final ClassPool classPool = getClassPool(loader);
    final CtClass klass = classPool.makeClass(new ByteArrayInputStream(classFileBuffer));

    CtClass result = processPointcut(klass, filter);
    if (result != null) {
      return result.toBytecode();
    }
    return null;
  }

  private static Set<Integer> thunkIds(Map<Pointcut, Integer> pointcutToDispatchId, Set<Pointcut> pointcuts) {
    Set<Integer> thunkIds = new TreeSet<Integer>();
    for (Pointcut pointcut : pointcuts) {
      final Integer id = pointcutToDispatchId.get(pointcut);
      if (id != null) {
        thunkIds.add(id);
      }
    }
    return thunkIds;
  }

  private Set<Integer> thunkIds(Set<Pointcut> pointcuts) {
    final Set<Integer> union = new TreeSet<Integer>();
    union.addAll(thunkIds(firstPriorityPointcutToDispatchId, pointcuts));
    union.addAll(thunkIds(secondPriorityPointcutToDispatchId, pointcuts));
    return union;
  }

  private static Map<CtBehavior, Set<Pointcut>> getAffectedBehaviors(Map<Pointcut, Integer> pointcutToDispatchId,
                                                                     CtClass ctClass, UnloggerTransformerFilter filter)
      throws Exception {
    if (ctClass.isInterface() || ctClass.isArray() || ctClass.isPrimitive()) {
      return Collections.emptyMap();
    }

    return WeaverUtils.computeAffectedBehaviours(pointcutToDispatchId.keySet(), ctClass, filter);
  }

  private void weave(Map<CtBehavior, Set<Pointcut>> affectedBehaviours) throws Exception {
    for (Map.Entry<CtBehavior, Set<Pointcut>> weaverEntry : affectedBehaviours.entrySet()) {
      CtBehavior weavingPoint = weaverEntry.getKey();
      Set<Integer> dispatchIds = thunkIds(weaverEntry.getValue());
      weave(dispatchIds, weavingPoint);
    }
  }

  private CtClass processPointcut(CtClass ctClass, UnloggerTransformerFilter filter) throws Exception {
    //
    // compute affected behaviors for first priority pointcuts and for second priority poincuts
    // we need to have all first priority pointcuts, and second priority poincuts without those that affected by
    // first priority pointcuts
    //
    // ex:
    // first priority pointcut List#add(Object el)
    // second priority pointcut ArrayList#add(Object el)
    //
    // we need to weave methods that affected by first priority pointcut in this case
    // (to avoid twice weaving because ArrayList#add(Object el) matched by both first and second priority pointcut
    //
    // P1 - first priority pointcuts
    // P2 - second priority pointcuts
    // PR - result pointcuts
    //
    // PR = P1 + (P2 - P1)
    //
    final Map<CtBehavior, Set<Pointcut>> affectedBehaviours = new HashMap<CtBehavior, Set<Pointcut>>();
    affectedBehaviours.putAll(getAffectedBehaviors(secondPriorityPointcutToDispatchId, ctClass, filter));

    final Map<CtBehavior, Set<Pointcut>> firstPriorityAffectedBehaviours = getAffectedBehaviors(firstPriorityPointcutToDispatchId, ctClass, filter);

    for (final CtBehavior firstPriorityBehavior : firstPriorityAffectedBehaviours.keySet()) {
      affectedBehaviours.remove(firstPriorityBehavior);
    }
    affectedBehaviours.putAll(firstPriorityAffectedBehaviours);

    if (affectedBehaviours.isEmpty()) {
      return null;
    }

    weave(affectedBehaviours);
    return ctClass;
  }

  private void weave(Set<Integer> adviceIds, CtBehavior to) throws CannotCompileException, NotFoundException {
    try {
      for (Integer adviceId : adviceIds) {
        weave(adviceId, to);
      }
    } catch (Exception e) {
      if (MonitorUtil.JAVA_MAJOR_VERSION >= 9) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Could not weave method -> " + to + ", error was: " + e.getMessage());
        }
      } else {
        LOG.error("Could not weave method -> " + to, e);
      }
    }
  }

  protected void weave(int adviceId, CtBehavior to) throws CannotCompileException, NotFoundException {
    // AdviceDispatcher.logBefore(adviceId, that, params);
    StringInterpolator interpolator = StringInterpolator.interpolator("#")
        //
        .addParam("thunkClassName", thunkClassName)
        //
        .addParam("conversionFunctor", RESULT_CONVERSION)
        //
        .addParam("adviceId", adviceId)
        //
        .addParam("class", "$0.getClass().getName()")
        //
        .addParam("methodName", to.getName())
        //
        .addParam("sig", serializeParams(to.getParameterTypes()));

    String beforeThunkTemplate = "{#thunkClassName#.logBefore(#adviceId#, new com.sematext.spm.client.unlogger.JoinPoint(#class#, \"#methodName#\", \"#sig#\"), $0, $args);}";
    if (LOG.isDebugEnabled()) {
      LOG.debug("Start weaving: " + to.getLongName());
    }

    if (to instanceof CtConstructor) {
      // if we invoke insertBefore for constructor,
      // we have not access to this variable
      ((CtConstructor) to).insertBeforeBody(interpolator.interpolate(beforeThunkTemplate));
    } else {
      to.insertBefore(interpolator.interpolate(beforeThunkTemplate));
    }
    String afterThunkTemplate = isHasReturnValue(to) ?
        "{#thunkClassName#.logAfter(#adviceId#, new com.sematext.spm.client.unlogger.JoinPoint(#class#, \"#methodName#\", \"#sig#\"), $0, #conversionFunctor#($_));}" :
        "{#thunkClassName#.logAfter(#adviceId#, new com.sematext.spm.client.unlogger.JoinPoint(#class#, \"#methodName#\", \"#sig#\"), $0, null);}";
    to.insertAfter(interpolator.interpolate(afterThunkTemplate), false);
    to.addCatch(
        interpolator
            .interpolate("{#thunkClassName#.logThrow(#adviceId#, new com.sematext.spm.client.unlogger.JoinPoint(\"#class#\", \"#methodName#\", \"#sig#\"), $0, $e); throw $e; }"),
        to.getDeclaringClass().getClassPool().get(Throwable.class.getCanonicalName()));

    if (LOG.isDebugEnabled()) {
      LOG.debug("Finish weaving: " + to.getLongName());
    }
  }

  private static boolean isHasReturnValue(CtBehavior behavior) throws NotFoundException {
    return (behavior instanceof CtMethod && !((CtMethod) behavior).getReturnType().getName().equals("void"));
  }

  private static String serializeParams(CtClass[] ctClasses) {
    StringBuilder res = new StringBuilder();
    String delim = "";
    for (CtClass ctClass : ctClasses) {
      res.append(delim);
      res.append(ctClass.getName());
      delim = ",";
    }
    return res.toString();
  }

  private ClassPool getClassPool(ClassLoader loader) {
    return new ClassPools().getClassPool(loader);
  }
}
