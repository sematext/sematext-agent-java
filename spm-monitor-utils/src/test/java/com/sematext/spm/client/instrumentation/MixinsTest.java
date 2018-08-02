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

package com.sematext.spm.client.instrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.sematext.spm.client.unlogger.MethodPointcut;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.Loader;

public class MixinsTest {

  public static interface NameMixin {
    @Getter("name")
    String getName();

    @Setter("name")
    void setName(String value);
  }

  public static class Person {
  }

  @Test
  public void testMixinGetterSetter() throws Exception {
    ClassPool cp = ClassPool.getDefault();
    Mixins.mixin(cp.get(Person.class.getName()), NameMixin.class);

    final Class<?> personClass = Class.forName(Person.class.getName(), true, new Loader(cp));

    final Method getName = personClass.getMethod("getName");
    assertNotNull(getName);

    final Method setName = personClass.getMethod("setName", String.class);
    assertNotNull(setName);

    Object person = personClass.newInstance();
    setName.invoke(person, "Jim");

    assertEquals("Jim", getName.invoke(person));
  }

  public static class Servlet {
    public void setStatus(int status) {
    }

    public void setStatusWithReason(int status, String reason) {
    }
  }

  public interface ServletMixin {
    @ParameterCapture(methods = { "setStatus", "setStatusWithReason" }, initial = "404")
    int getStatus();
  }

  @Test
  public void testMixinParameterCapture() throws Exception {
    ClassPool cp = ClassPool.getDefault();
    Mixins.mixin(cp.get(Servlet.class.getName()), ServletMixin.class);

    final Class<?> servletClass = Class.forName(Servlet.class.getName(), true, new Loader(cp));

    final Method setStatus = servletClass.getMethod("setStatus", int.class);
    final Method setStatusWithReason = servletClass.getMethod("setStatusWithReason", int.class, String.class);
    final Method getStatus = servletClass.getMethod("getStatus");
    assertNotNull(getStatus);

    Object servlet = servletClass.newInstance();

    assertEquals(404, getStatus.invoke(servlet));

    setStatus.invoke(servlet, 201);

    setStatusWithReason.invoke(servlet, 201, "Created");

    assertEquals(201, getStatus.invoke(servlet));
  }

  public static class Response {
    public String name;

    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  public static interface ResponseMixin {
    @Delegate(method = "setName")
    void updateName(String name);

    @Delegate(method = "getName")
    String getPersonName();
  }

  @Test
  public void testMixinDelegate() throws Exception {
    ClassPool cp = ClassPool.getDefault();
    Mixins.mixin(cp.get(Response.class.getName()), ResponseMixin.class);

    final Class<?> responseClass = Class.forName(Response.class.getName(), true, new Loader(cp));

    final Field name = responseClass.getField("name");
    final Method updateName = responseClass.getMethod("updateName", String.class);
    final Method getPersonName = responseClass.getMethod("getPersonName");

    Object response = responseClass.newInstance();
    updateName.invoke(response, "jack");

    assertEquals("jack", name.get(response));
    assertEquals("jack", getPersonName.invoke(response));
  }

  public static class Empty {
  }

  public static interface EmptyMixin {
    @Delegate(method = "_incrementCounter")
    void incrementCounter();

    @Delegate(method = "_getAge")
    int getAge();

    @Delegate(method = "_isMarried")
    boolean isMarried();

    @Delegate(method = "_weight")
    double getWeight();

    @Delegate(method = "_copyInstance")
    Object copyInstance();

    @Delegate(method = "_getLastName")
    String getLastName();
  }

  @Test
  public void testMixinTolerateMissingMethods() throws Exception {
    ClassPool cp = ClassPool.getDefault();
    Mixins.mixin(cp.get(Empty.class.getName()), EmptyMixin.class);

    final Class<?> emptyClass = Class.forName(Empty.class.getName(), true, new Loader(cp));

    final Method incrementCounter = emptyClass.getMethod("incrementCounter");
    final Method getAge = emptyClass.getMethod("getAge");
    final Method copy = emptyClass.getMethod("copyInstance");
    final Method isMarried = emptyClass.getMethod("isMarried");
    final Method getWeight = emptyClass.getMethod("getWeight");
    final Method getLastName = emptyClass.getMethod("getLastName");

    Object empty = emptyClass.newInstance();

    incrementCounter.invoke(empty);

    assertEquals(0, getAge.invoke(empty));
    assertNull(copy.invoke(empty));
    assertFalse((Boolean) isMarried.invoke(empty));
    assertEquals(0, (Double) getWeight.invoke(empty), 10);
    assertNull(getLastName.invoke(empty));
  }

  public static class Employee {
    public void setAge(int age) {
    }

    public void updateAge(int updateAge) {
    }

    public String greeting() {
      return "Alloha!";
    }
  }

  public static class CTO extends Employee {
    public String getName() {
      return "Steve";
    }
  }

  public static interface EmployeeAccess {
    @Delegate(method = "getName")
    String print();

    @Delegate(method = "greeting")
    String greet();

    @ParameterCapture(methods = { "setAge", "updateAge" })
    int getAge();
  }

  @Test
  public void testMixinProcessInheritedMethods() throws Exception {
    ClassPool cp = ClassPool.getDefault();
    Mixins.mixin(cp.get(CTO.class.getName()), EmployeeAccess.class);

    Class<?> klass = Class.forName(CTO.class.getName(), true, new Loader(cp));
    Method setAge = klass.getMethod("setAge", int.class);
    Method getName = klass.getMethod("getName");
    Method print = klass.getMethod("print");
    Method getAge = klass.getMethod("getAge");
    Method greet = klass.getMethod("greet");

    Object cto = klass.newInstance();

    setAge.invoke(cto, 42);
    assertEquals(42, getAge.invoke(cto));
    assertEquals("Steve", print.invoke(cto));
    assertEquals("Alloha!", greet.invoke(cto));
  }

  public static interface MessageAccess {
    @Delegate(method = "getProtocol")
    String _getProtocol();
  }

  public static interface RequestAccess extends MessageAccess {
    @Delegate(method = "getRequestLine")
    String _getRequestLine();
  }

  public static class HttpRequest {
    public String getProtocol() {
      return "http";
    }

    public String getRequestLine() {
      return "GET /";
    }
  }

  @Test
  public void testMixinInterfaceInheritance() throws Exception {
    ClassPool cp = ClassPool.getDefault();
    Mixins.mixin(cp.get(HttpRequest.class.getName()), RequestAccess.class);

    Class<?> klass = Class.forName(HttpRequest.class.getName(), true, new Loader(cp));
    Method getProtocolDelegate = klass.getMethod("_getProtocol");
    Method getRequestLineDelegate = klass.getMethod("_getRequestLine");

    Object httpRequest = klass.newInstance();

    assertEquals("http", getProtocolDelegate.invoke(httpRequest));
    assertEquals("GET /", getRequestLineDelegate.invoke(httpRequest));
  }

  @Test
  public void testSequentiallyMixinInheritedTraits() throws Exception {
    ClassPool cp = ClassPool.getDefault();

    Mixins.mixin(cp.get(HttpRequest.class.getName()), MessageAccess.class);
    Mixins.mixin(cp.get(HttpRequest.class.getName()), RequestAccess.class);

    Class<?> klass = Class.forName(HttpRequest.class.getName(), true, new Loader(cp));
    Method getProtocolDelegate = klass.getMethod("_getProtocol");
    Method getRequestLineDelegate = klass.getMethod("_getRequestLine");

    Object httpRequest = klass.newInstance();

    assertEquals("http", getProtocolDelegate.invoke(httpRequest));
    assertEquals("GET /", getRequestLineDelegate.invoke(httpRequest));
  }

  @Test
  public void testTolerateTwiceMixin() throws Exception {
    ClassPool cp = ClassPool.getDefault();
    Mixins.mixin(cp.get(Person.class.getName()), NameMixin.class);

    CtClass klass = cp.get(Person.class.getName());
    klass.toBytecode();

    Mixins.mixin(cp.get(Person.class.getName()), NameMixin.class);
  }

  @Test
  public void testCtBehaviourEquals() throws Exception {
    MethodPointcut method = MethodPointcut.FACTORY.make("void java.util.ArrayList#add(int p1)");
    System.out.println(method);
  }

}
