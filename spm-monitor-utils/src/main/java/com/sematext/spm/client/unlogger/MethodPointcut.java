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

import static com.sematext.spm.client.unlogger.VvsEL.METHOD_SIGNATURE_MATCHER;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.regex.Pattern;

import com.sematext.spm.client.unlogger.pointcuts.UPointcutContext;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;

public final class MethodPointcut extends ConcretePointcut {

  public static final Pointcut.Factory<MethodPointcut, String> FACTORY = new Pointcut.Factory<MethodPointcut, String>() {
    @Override
    public MethodPointcut make(String stringRepresentation) {
      return MethodPointcut.make(stringRepresentation, new String[] {});
    }
  };
  private final String returnType;
  private final String methodName;
  private final Pattern[] ignorePatterns;
  private final SignatureMatcher matcher;

  private MethodPointcut(String returnType, String typeName, String methodName, LinkedHashMap<String, String> params,
                         Pattern[] ignorePatterns) {
    super(typeName, params);
    this.returnType = returnType;
    this.methodName = methodName;
    this.ignorePatterns = ignorePatterns;
    this.matcher = SignatureMatcher.make(this);
  }

  public static <T extends Collection<? super MethodPointcut>> T make(String[] methods, String[] ignorePatterns,
                                                                      T result) {
    for (final String method : methods) {
      result.add(make(method, ignorePatterns));
    }
    return result;
  }

  /**
   * Pointcut is simple set of the methods names. Method name is very similar to canonical name that returned by
   * toString() ot Method class with the following differences: 1. All access modifiers, exception types is ignored. 2.
   * Method name delimited from a class name by a “#” sign, instead of period. 3. Parameter name should be followed by
   * the any parameter type.
   * <p/>
   * If pointcut describes the private method the advice handles only it. For other access modifiers handled that method
   * and all methods that override it. In case of interfaces handled in all implemented classes (except the
   * bridged/synthetic methods).
   *
   * @param stringRepresentation
   * @return
   */
  private static MethodPointcut make(String stringRepresentation, String[] ignoreRegexp) {
    // We should not use anything more complicated than regexp
    // because we should eliminate dependencies for code that can be run
    // in agent as much as possible.
    StrictMatcher.Result methodMatcher = METHOD_SIGNATURE_MATCHER.match(stringRepresentation);
    if (methodMatcher == null) {
      throw new IllegalArgumentException("Can't recognize pointcut signature -> " + stringRepresentation);
    }

    String returnType = methodMatcher.get(1);
    String typeName = methodMatcher.get(2);
    String methodName = methodMatcher.get(3);
    LinkedHashMap<String, String> params = methodMatcher.get(4, LinkedHashMap.class);

    Pattern[] ignorePatterns = new Pattern[ignoreRegexp.length];

    for (int i = 0; i < ignoreRegexp.length; i++) {
      ignorePatterns[i] = Pattern.compile(ignoreRegexp[i]);
    }

    return new MethodPointcut(returnType, typeName, methodName, params, ignorePatterns);
  }

  public String getReturnType() {
    return returnType;
  }

  public String getMethodName() {
    return methodName;
  }

  @Override
  public String toString() {
    return "MethodPointcut[ methodName='" + methodName + "', returnType='" + returnType + "', type = '" + getTypeName()
        + "']";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
    result = prime * result + ((getParams() == null) ? 0 : getParams().hashCode());
    result = prime * result + ((returnType == null) ? 0 : returnType.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    MethodPointcut other = (MethodPointcut) obj;
    if (methodName == null) {
      if (other.methodName != null) {
        return false;
      }
    } else if (!methodName.equals(other.methodName)) {
      return false;
    }
    if (getParams() == null) {
      if (other.getParams() != null) {
        return false;
      }
    } else if (!getParams().equals(other.getParams())) {
      return false;
    }
    if (returnType == null) {
      if (other.returnType != null) {
        return false;
      }
    } else if (!returnType.equals(other.returnType)) {
      return false;
    }
    return true;
  }

  @Override
  protected boolean isApplied(CtMethod ctMethod) throws NotFoundException {
    return matcher.equals(SignatureMatcher.make(ctMethod));
  }

  @Override
  protected void match(CtClass clazz, UPointcutContext ctx, Set<CtBehavior> result) throws NotFoundException {
    if (ctx.getHierarchy().contains(getTypeName())) {
      for (final Pattern ignorePattern : ignorePatterns) {
        if (ignorePattern.matcher(clazz.getName()).matches()) {
          return;
        }
      }

      for (CtBehavior beh : clazz.getDeclaredBehaviors()) {
        if (beh.getMethodInfo().isMethod() && !WeaverUtils.isAbstract(beh) && !WeaverUtils.isStatic((CtMethod) beh)) {
          if (matcher.equals(SignatureMatcher.make((CtMethod) beh))) {
            result.add(beh);
          }
        }
      }
    }
  }

  @Override
  protected boolean isApplied(CtConstructor ctConstructor) throws NotFoundException {
    return false;
  }

  @Override
  public String getAsString() {
    return returnType + " " + getTypeName() + "#" + methodName + "(" + getParams() + ")";
  }

}
