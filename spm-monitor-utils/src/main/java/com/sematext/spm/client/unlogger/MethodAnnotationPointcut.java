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

import java.util.Set;

import com.sematext.spm.client.unlogger.pointcuts.UPointcutContext;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;

public class MethodAnnotationPointcut extends Pointcut {

  public static final Factory<Pointcut, String> FACTORY = new Factory<Pointcut, String>() {
    @Override
    public Pointcut make(String s) {
      return new MethodAnnotationPointcut(null, s);
    }
  };

  private final String annotation;

  public MethodAnnotationPointcut(String typeName, String annotation) {
    super(typeName);
    this.annotation = annotation;
  }

  @Override
  public <T> T getWellknownParam(String paramName, Object[] params) {
    return null;
  }

  @Override
  protected boolean isApplied(CtMethod ctMethod) throws NotFoundException {
    return false;
  }

  @Override
  protected boolean isApplied(CtConstructor ctConstructor) throws NotFoundException {
    return false;
  }

  @Override
  protected void match(CtClass clazz, UPointcutContext ctx, Set<CtBehavior> result) throws NotFoundException {
    for (final CtBehavior behavior : clazz.getDeclaredBehaviors()) {
      final AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute) behavior.getMethodInfo()
          .getAttribute(AnnotationsAttribute.visibleTag);
      if (annotationsAttribute != null) {
        for (Annotation annotationAttribute : annotationsAttribute.getAnnotations()) {
          if (annotationAttribute.getTypeName().equals(this.annotation)) {
            result.add(behavior);
          }
        }
      }
    }
  }

  /*CHECKSTYLE:OFF*/
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    MethodAnnotationPointcut that = (MethodAnnotationPointcut) o;

    if (annotation != null ? !annotation.equals(that.annotation) : that.annotation != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (annotation != null ? annotation.hashCode() : 0);
    return result;
  }
  /*CHECKSTYLE:ON*/

  @Override
  public String getAsString() {
    return "Annotation [ annotation = '" + annotation + "]";
  }
}
