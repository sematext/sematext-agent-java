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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.util.InstrumentationUtils;
import com.sematext.spm.client.util.StringUtils;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.MethodInfo;

public final class Mixins {

  private static final Log LOG = LogFactory.getLog(Mixins.class);

  private Mixins() {
  }

  private static interface Processor {
    void process(CtClass target, Method method) throws CannotCompileException;
  }

  private static CtField getOrAddField(CtClass target, String fieldName, Class<?> type) throws CannotCompileException {
    CtField field = null;
    try {
      field = target.getDeclaredField(fieldName);
    } catch (NotFoundException e) {
      /* pass */
    }

    if (field == null) {
      field = CtField.make("private " + type.getCanonicalName() + " " + fieldName + ";", target);
      target.addField(field);
    }

    return field;
  }

  private static CtMethod createEmptyMethodBody(CtClass target, String name, Class[] parameterTypes, Class returnType)
      throws CannotCompileException {
    final StringBuilder body = new StringBuilder();
    body.append("public ").append(returnType.getCanonicalName());
    body.append(" ").append(name).append("(");

    final List<String> parameters = new ArrayList<String>();
    for (int i = 0; i < parameterTypes.length; i++) {
      parameters.add(parameterTypes[i].getCanonicalName() + " $p" + i);
    }

    body.append(StringUtils.join(parameters, ","));

    body.append(")").append("{");

    if (returnType.isPrimitive()) {
      if (returnType == Boolean.TYPE) {
        body.append("return false;");
      } else if (returnType != Void.TYPE) {
        /* return (double)0; */
        body.append("return ").append("(").append(returnType.getCanonicalName()).append(")").append("0").append(";");
      }
    } else {
      body.append("return null;");
    }

    body.append("}");

    return CtNewMethod.make(body.toString(), target);
  }

  private static List<CtClass> getHierarchy(CtClass source, List<CtClass> hierarchy) throws NotFoundException {
    if (source == null) {
      return hierarchy;
    }

    if (!hierarchy.contains(source)) {
      hierarchy.add(source);
    }

    getHierarchy(source.getSuperclass(), hierarchy);
    return hierarchy;
  }

  public static enum Namer {
    ID {
      @Override
      public String name(String of) {
        return of;
      }
    },
    PARAMETER_CAPTURE_FIELD_NAMER {
      @Override
      public String name(String of) {
        return "_$spm_mixin$_" + of;
      }
    };

    public abstract String name(String of);
  }

  private static class GetterProcessor implements Processor {
    @Override
    public void process(CtClass target, Method method) throws CannotCompileException {
      final Getter getter = method.getAnnotation(Getter.class);
      final CtField field = getOrAddField(target, getter.value(), method.getReturnType());
      final CtMethod newMethod = CtNewMethod.getter(method.getName(), field);
      target.addMethod(newMethod);
    }
  }

  private static class SetterProcessor implements Processor {
    @Override
    public void process(CtClass target, Method method) throws CannotCompileException {
      final Setter setter = method.getAnnotation(Setter.class);
      final CtField field = getOrAddField(target, setter.namer().name(setter.value()), method.getParameterTypes()[0]);
      final CtMethod newMethod = CtNewMethod.setter(method.getName(), field);
      target.addMethod(newMethod);
    }
  }

  private static class ParameterCaptureProcessor implements Processor {
    @Override
    public void process(CtClass target, Method method) throws CannotCompileException {
      final ParameterCapture capture = method.getAnnotation(ParameterCapture.class);
      final Set<String> methodNames = new HashSet<String>();
      for (String methodName : capture.methods()) {
        methodNames.add(methodName);
      }

      final String fieldTypeName = method.getReturnType().getCanonicalName();
      final String fieldName = Namer.PARAMETER_CAPTURE_FIELD_NAMER.name(method.getName());

      final StringBuilder fieldBody = new StringBuilder();
      fieldBody.append("private ").append(fieldTypeName).append(" ").append(fieldName);

      if (capture.initial() != null && !capture.initial().isEmpty()) {
        fieldBody.append("=").append(capture.initial());
      }

      fieldBody.append(";");

      final List<CtClass> hierarchy;
      try {
        hierarchy = getHierarchy(target, new ArrayList<CtClass>());
      } catch (NotFoundException e) {
        throw new RuntimeException("Can't get hierarchy.", e);
      }

      for (String methodName : methodNames) {
        for (CtClass klass : hierarchy) {
          boolean modified = false;

          for (CtBehavior beh : klass.getDeclaredBehaviors()) {
            final MethodInfo methodInfo = beh.getMethodInfo();
            if (methodName.equals(methodInfo.getName())
                && (methodInfo.getAccessFlags() & (AccessFlag.ABSTRACT | AccessFlag.STATIC)) == 0) {

              CtField field;
              try {
                field = klass.getDeclaredField(fieldName);
              } catch (NotFoundException e) {
                field = CtField.make(fieldBody.toString(), klass);
                klass.addField(field);
              }

              try {
                klass.getDeclaredMethod(method.getName());
              } catch (NotFoundException e) {
                final CtMethod getter = CtNewMethod.getter(method.getName(), field);
                klass.addMethod(getter);
              }

              beh.insertBefore(fieldName + " = $1;");

              modified = true;
              break;
            }
          }

          if (modified) {
            break;
          }
        }
      }
    }
  }

  private static class DelegateProcessor implements Processor {
    @Override
    public void process(CtClass target, Method method) throws CannotCompileException {
      final Delegate delegate = method.getAnnotation(Delegate.class);

      final StringBuilder body = new StringBuilder();
      body.append("public ").append(method.getReturnType().getCanonicalName());
      body.append(" ").append(method.getName()).append("(");

      final List<String> parameters = new ArrayList<String>();
      final Class<?>[] types = method.getParameterTypes();
      for (int i = 0; i < types.length; i++) {
        parameters.add(types[i].getCanonicalName() + " $p" + i);
      }

      body.append(StringUtils.join(parameters, ","));

      body.append(")").append("{");
      if (!method.getReturnType().getCanonicalName().equals("void")) {
        body.append("return ");
      }
      body.append("$0.").append(delegate.method()).append("(");

      final List<String> arguments = new ArrayList<String>();
      for (int i = 1; i <= types.length; i++) {
        arguments.add("$" + i);
      }

      body.append(StringUtils.join(arguments, ",")).append(");");
      body.append("}");

      try {
        final CtMethod delegateMethod = CtNewMethod.make(body.toString(), target);
        target.addMethod(delegateMethod);
      } catch (CannotCompileException cce) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(
              "Can't mixin delegate (" + method + ") to class: " + target + ". Replacing with empty method instead.");
        }
        final CtMethod emptyMethod = createEmptyMethodBody(target, method.getName(), method.getParameterTypes(),
                                                           method.getReturnType());
        target.addMethod(emptyMethod);
      }
    }
  }

  private static enum Processors {
    GETTER(Getter.class, new GetterProcessor()),
    SETTER(Setter.class, new SetterProcessor()),
    PARAMETER_CAPTURE(ParameterCapture.class, new ParameterCaptureProcessor()),
    DELEGATE(Delegate.class, new DelegateProcessor());

    final Class<? extends Annotation> annotation;
    final Processor processor;

    Processors(Class<? extends Annotation> annotation, Processor processor) {
      this.annotation = annotation;
      this.processor = processor;
    }

    static Processor getProcessor(Method method) {
      for (Processors p : Processors.values()) {
        if (method.getAnnotation(p.annotation) != null) {
          return p.processor;
        }
      }
      return null;
    }
  }

  private static void getSubInterfaces(Set<Class<?>> hierarchy, Class<?> iface) {
    hierarchy.add(iface);
    for (final Class<?> subInterface : iface.getInterfaces()) {
      getSubInterfaces(hierarchy, subInterface);
    }
  }

  private static void mixin(CtClass target, Method method) throws CannotCompileException {
    final Processor processor = Processors.getProcessor(method);
    if (processor == null) {
      throw new IllegalStateException("Can't find processor for method: " + method + ".");
    }
    processor.process(target, method);
  }

  public static boolean mixin(CtClass target, Class<?> iface) throws Exception {
    if (!iface.isInterface()) {
      throw new IllegalArgumentException("Just interfaces can be mixed");
    }

    final Set<String> targetHierarchy = InstrumentationUtils.getHierarchy(target);
    if (targetHierarchy.contains(iface.getName())) {
      return false;
    }

    final ClassPool cp = target.getClassPool();

    target.addInterface(cp.getCtClass(iface.getName()));

    final Set<Class<?>> subInterfaces = new HashSet<Class<?>>();
    getSubInterfaces(subInterfaces, iface);

    for (final Class<?> subInterface : subInterfaces) {
      if (targetHierarchy.contains(subInterface.getName())) {
        continue;
      }

      for (final Method method : subInterface.getDeclaredMethods()) {
        mixin(target, method);
      }
    }

    return true;
  }

}
