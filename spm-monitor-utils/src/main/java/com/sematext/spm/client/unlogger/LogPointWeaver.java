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

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.util.ClassPools;
import com.sematext.spm.client.util.StringInterpolator;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.Loader;
import javassist.NotFoundException;

public class LogPointWeaver {
  private final Map<Pointcut, Integer> pointCutsToDispatchId;
  private final String thunkClassName;
  private final UnloggerTransformerFilter filter;
  private static final String RESULT_CONVERSION = WeaverUtils.class.getName() + ".toObject";
  private static final Log LOG = LogFactory.getLog(LogPointWeaver.class);

  public LogPointWeaver(Map<Pointcut, Integer> pointCutsToDispatchId, String thunkClassName) {
    this.pointCutsToDispatchId = pointCutsToDispatchId;
    this.thunkClassName = thunkClassName;
    this.filter = UnloggerTransformerFilter.ACCEPT_ALL;
  }

  public LogPointWeaver(Map<Pointcut, Integer> pointCutsToDispatchId, String thunkClassName,
                        UnloggerTransformerFilter filter) {
    this.pointCutsToDispatchId = pointCutsToDispatchId;
    this.thunkClassName = thunkClassName;
    this.filter = filter;
  }

  /**
   * @param loader
   * @param className
   * @param classBinary
   * @return
   * @throws Exception
   */
  protected CtClass processPointcut(ClassLoader loader, String className, byte[] classBinary) throws Exception {
    return processPointcut(buildClassPool(loader), className, classBinary);
  }

  protected CtClass processPointcut(ClassLoader loader, String className, byte[] classBinary,
                                    UnloggerTransformerFilter filter) throws Exception {
    return processPointcut(buildClassPool(loader), className, classBinary, filter);
  }

  protected CtClass processPointcut(ClassPool classPool, String className, byte[] classBinary) throws Exception {
    CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classBinary));
//    ctClass.detach();
    return processPointcut(ctClass, filter);
  }

  protected CtClass processPointcut(ClassPool classPool, String className, byte[] classBinary,
                                    UnloggerTransformerFilter filter) throws Exception {
    CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classBinary));
//    ctClass.detach();
    return processPointcut(ctClass, filter);
  }

  protected CtClass processPointcut(CtClass ctClass, UnloggerTransformerFilter filter) throws Exception {
    if (ctClass.isInterface() || ctClass.isArray() || ctClass.isPrimitive()) {
      return null;
    }

    Map<CtBehavior, Set<Pointcut>> affectedBehaviours = null;

    try {
      affectedBehaviours = WeaverUtils.computeAffectedBehaviours(
          pointCutsToDispatchId.keySet(), ctClass, filter);
    } catch (Throwable th) {
      LOG.error("Can't process pointcut for class:" + ctClass, th);
      return null;
    }

    if (affectedBehaviours.isEmpty()) {
      // Noting to change
      return null;
    }

    for (Map.Entry<CtBehavior, Set<Pointcut>> weaverEntry : affectedBehaviours.entrySet()) {
      CtBehavior weavingPoint = weaverEntry.getKey();
      Set<Integer> dispatchIds = thunkIds(weaverEntry.getValue());
      weave(dispatchIds, weavingPoint);
    }

    return ctClass;
  }

  /**
   * @param pointcuts
   * @return
   */
  private Set<Integer> thunkIds(Set<Pointcut> pointcuts) {
    Set<Integer> thunkIds = new TreeSet<Integer>();
    for (Pointcut pointcut : pointcuts) {
      thunkIds.add(pointCutsToDispatchId.get(pointcut));
    }
    return thunkIds;
  }

  public byte[] processPointcutAsBytes(ClassLoader loader, String className, byte[] classBinary,
                                       UnloggerTransformerFilter filter) throws Exception {
    return processPointcut(loader, className, classBinary, filter).toBytecode();
  }

  public byte[] processPointcutAsBytes(ClassLoader loader, String className, byte[] classBinary) throws Exception {
    return processPointcut(loader, className, classBinary).toBytecode();
  }

  public Class<?> processPointcutAsClass(ClassLoader loader, String className, byte[] classBinary) throws Exception {
    Loader ctLoader = new Loader(buildClassPool(loader));
    processPointcut(loader, className, classBinary);
    return ctLoader.loadClass(className);
  }

  public void loadPointcutAsClass(ClassPool classPool, String className) throws Exception {
    processPointcut(classPool.get(className), filter);
  }

  private void weave(Set<Integer> adviceIds, CtBehavior to) throws CannotCompileException, NotFoundException {
    try {
      for (Integer adviceId : adviceIds) {
        weave(adviceId, to);
      }
    } catch (Exception e) {
      LOG.error("Could not weave method -> " + to, e);
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

    if (LOG.isInfoEnabled()) {
      LOG.info("Start weaving: " + to.getLongName());
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

    if (LOG.isInfoEnabled()) {
      LOG.info("Finish weaving: " + to.getLongName());
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

  private static ClassPool buildClassPool(ClassLoader loader) {
    return new ClassPools().getClassPool(loader);
  }

}
