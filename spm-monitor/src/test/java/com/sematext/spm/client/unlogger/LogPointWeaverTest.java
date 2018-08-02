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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sematext.spm.client.util.ReflectionUtils;

/**
 * We can't use test frameworks based on bytecode manipulation(like mockito) because it can interfere with javassist.
 */
public class LogPointWeaverTest {
  // TODO we can weave class only
  // one time, so all checks we place in one test method.
  @Test
  public void testSimpleInvoke() {
    Set<MethodPointcut> simpleToLogPointcuts = methodPointcuts(
        "java.lang.String com.sematext.spm.client.unlogger.LogPointWeaverTest$SimpleToLog#toLog()",
        "void com.sematext.spm.client.unlogger.LogPointWeaverTest$SimpleToLog#toLogThrow()");

    Class<SimpleToLog> weavedClass = weave(simpleToLogPointcuts, SimpleToLog.class);

    Object simpleToLog = ReflectionUtils.instance(weavedClass);
    Assert.assertEquals("Logged", VvsEL.eval(simpleToLog, "toLog()"));
    // Assert.assertEquals(Arrays.asList(ExecutionFlowPoints.BEFORE,
    // ExecutionFlowPoints.AFTER), TestLogger.getAndCleanPrevious());

    try {
      VvsEL.eval(simpleToLog, "toLogThrow()");
    } catch (Exception e) {

    }
  }

  @Test
  public void testSimpleInvokeViaInterface() {
    Set<MethodPointcut> simpleToLogPointcuts = methodPointcuts(
        "java.lang.String com.sematext.spm.client.unlogger.LogPointWeaverTest$SimpleToLogInterface#toLog()",
        "void com.sematext.spm.client.unlogger.LogPointWeaverTest$SimpleToLogInterface#toLogThrow()");

    Class<SimpleToLogInterfaceImpl> weavedClass = weave(simpleToLogPointcuts, SimpleToLogInterfaceImpl.class);

    Object simpleToLog = ReflectionUtils.instance(weavedClass);
    Assert.assertEquals("Logged", VvsEL.eval(simpleToLog, "toLog()"));

    try {
      VvsEL.eval(simpleToLog, "toLogThrow()");
    } catch (Exception e) {

    }
  }

  @Test
  public void testSimpleInvokeWithPrimitiveParams() {
    Set<MethodPointcut> simpleToLogPointcuts = methodPointcuts("java.lang.String com.sematext.spm.client.unlogger.LogPointWeaverTest$SimpleToLogWithApplicativeParams#toLog(boolean paramX, int patamY)");

    Class<SimpleToLogWithApplicativeParams> weavedClass = weave(simpleToLogPointcuts,
                                                                SimpleToLogWithApplicativeParams.class);

    Method toLogMethod = ReflectionUtils.getMethod(weavedClass, "toLog", Boolean.TYPE, Integer.TYPE);
    Assert.assertEquals("Logged",
                        ReflectionUtils.silentInvoke(toLogMethod, ReflectionUtils.instance(weavedClass), true, 1));

    // try {
    // ReflectionUtils.eval(simpleToLog, "toLogThrow()");
    // } catch (Exception e) {
    //
    // }
  }

  @Test
  @Ignore
  // TODO fix, with high priority
  // All OK with that test if we run it separatelly.
  // But if we run it with other,
  // for that moment SimpleToLog class
  // already loaded, and javassist marked
  // it as 'frozen', so we can't add new pointcuts here.
  public void testSimpleConstructor() {
    Class<SimpleToLog> weavedClass = weave(
        constructorPointcuts("com.sematext.spm.client.unlogger.LogPointWeaverTest$SimpleToLog(java.lang.Object object, java.lang.String string)"),
        SimpleToLog.class);

    Object simpleToLog = ReflectionUtils.instance(weavedClass, new Class[] { Object.class, String.class },
                                                  new Object[] { "ix", "iy" });
    Assert.assertNotNull(simpleToLog);

    try {
      VvsEL.eval(simpleToLog, "toLogThrow()");
    } catch (Exception e) {

    }
  }

  @Test
  public void testSimpleInvokeAndThrow() {
    // Class<SimpleToLog> weavedClass =
    // weave(Collections.singleton(MethodPointcut
    // .make("void com.sematext.spm.client.unlogger.LogPointWeaverTest$SimpleToLog#toLog()")),
    // SimpleToLog.class);
    //
    // Object simpleToLog = ReflectionUtils.instance(weavedClass);
    // ReflectionUtils.eval(simpleToLog, "toLogAndThrow()");
  }

  private static <T> Class<T> weave(Set<? extends Pointcut> pointCuts, Class<T> type) {
    try {
      LogPointWeaver weaver = new LogPointWeaver(generatePointcutsIds(pointCuts), TestLogger.class.getName());
      return (Class<T>) weaver.processPointcutAsClass(type.getClassLoader(), type.getName(), getAsBinary(type));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Map<Pointcut, Integer> generatePointcutsIds(Collection<? extends Pointcut> pointCuts) {
    int id = 0;
    Map<Pointcut, Integer> pointCutWithIds = new HashMap<Pointcut, Integer>();
    for (Pointcut pointcut : pointCuts) {
      pointCutWithIds.put(pointcut, id++);
    }
    return pointCutWithIds;
  }

  private static byte[] getAsBinary(Class<?> type) {
    InputStream inputStream = type.getClassLoader().getResourceAsStream(type.getName().replace(".", "/") + ".class");
    return toByteArray(inputStream);
  }

  private static byte[] toByteArray(InputStream is) {
    try {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      int nRead;
      byte[] data = new byte[16384];

      while ((nRead = is.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }

      buffer.flush();

      return buffer.toByteArray();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static class SimpleToLog {

    public SimpleToLog() {
    }

    public SimpleToLog(Object object, String string) {
    }

    public String toLog() {
      return "Logged";
    }

    public void toLogThrow() {
      throw new RuntimeException("Throw");
    }

  }

  public static class SimpleToLogExt extends SimpleToLog {
    public SimpleToLogExt() {
      super();
    }
  }

  public interface SimpleToLogInterface {

    String toLog();

    void toLogThrow();
  }

  public static class SimpleToLogInterfaceImpl implements SimpleToLogInterface {

    public String toLog() {
      return "Logged";
    }

    public void toLogThrow() {
      throw new RuntimeException("Throw");
    }

  }

  public static class SimpleToLogWithApplicativeParams {

    public String toLog(boolean paramX, int patamY) {
      return "Logged";
    }
  }

  // TODO now TestLogger loaded by classloader of weaved class,
  // not by a test classloader, so we can't use flow
  // information in a tests.
  public static class TestLogger {

    public enum ExecutionFlowPoints {
      BEFORE, AFTER, THROW;
    }

    private static final List<ExecutionFlowPoints> FLOW = new ArrayList<ExecutionFlowPoints>();

    public static void logBefore(int adviceId, JoinPoint methodJoinPoint, Object that, Object[] params) {
      FLOW.add(ExecutionFlowPoints.BEFORE);
    }

    public static void logAfter(int adviceId, JoinPoint methodJoinPoint, Object that, Object returnValue) {
      FLOW.add(ExecutionFlowPoints.AFTER);
    }

    public static void logThrow(int adviceId, JoinPoint methodJoinPoint, Object that, Throwable throwable) {
      FLOW.add(ExecutionFlowPoints.THROW);
    }

    public static List<ExecutionFlowPoints> getAndCleanPrevious() {
      List<ExecutionFlowPoints> ret = new ArrayList<ExecutionFlowPoints>(FLOW);
      FLOW.clear();
      return ret;
    }

  }

  private static Set<MethodPointcut> methodPointcuts(String... descriptors) {
    return MethodPointcut.FACTORY.make(descriptors, new HashSet<MethodPointcut>());
  }

  private static Set<ConstructorPointcut> constructorPointcuts(String... descriptors) {
    return ConstructorPointcut.FACTORY.make(descriptors, new HashSet<ConstructorPointcut>());
  }

}
