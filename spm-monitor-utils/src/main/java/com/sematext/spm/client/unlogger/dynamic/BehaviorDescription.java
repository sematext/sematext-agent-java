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

import org.apache.commons.lang.StringUtils;

import com.sematext.spm.client.unlogger.ConstructorPointcut;
import com.sematext.spm.client.unlogger.MethodPointcut;
import com.sematext.spm.client.unlogger.Pointcut;
import com.sematext.spm.client.unlogger.StrictMatcher;
import com.sematext.spm.client.unlogger.VvsEL;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Description of behavior - either method or constructor
 */
public final class BehaviorDescription {
  private final String signature;

  /**
   * @param signature [returnType ][[package.]class]#method(parameterType,...)
   */
  public BehaviorDescription(String signature) {
    this.signature = signature;
  }

  public String getSignature() {
    return signature;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BehaviorDescription that = (BehaviorDescription) o;

    return signature != null ? signature.equals(that.signature) : that.signature == null;

  }

  private String normalizeParamsForPointcut(String unparsedParameters) {
    final String[] params = unparsedParameters.trim().split(",");
    if (params.length == 1 && params[0].isEmpty()) {
      return StringUtils.EMPTY;
    }
    final StringBuilder normalizedParams = new StringBuilder();
    for (int i = 0; i < params.length; i++) {
      normalizedParams.append(params[i].trim()).append(" p").append(i);
      if (i < params.length - 1) {
        normalizedParams.append(",");
      }
    }
    return normalizedParams.toString();
  }

  private Pointcut toMethodPointcut() {
    final StrictMatcher.Result result = VvsEL.METHOD_SIGNATURE_MATCHER.match(signature);
    if (result == null) {
      return null;
    }

    final String returnType = result.get(1);
    final String type = result.get(2);
    final String methodName = result.get(3);
    final String unparsedParams = result.get(4);

    return MethodPointcut.FACTORY
        .make(String.format("%s %s#%s(%s)", returnType, type, methodName, normalizeParamsForPointcut(unparsedParams)));
  }

  private Pointcut toCtorPointcut() {
    final StrictMatcher.Result result = VvsEL.CONSTRUCTOR_SIGNATURE_MATCHER.match(signature);
    if (result == null) {
      return null;
    }

    final String type = result.get(1);
    final String unparsedParams = result.get(2);

    return ConstructorPointcut.FACTORY.make(String.format("%s(%s)", type, normalizeParamsForPointcut(unparsedParams)));
  }

  /**
   * @return pointcut
   * @throws IllegalStateException if pointcut can't be created
   */
  public Pointcut toPointcut() {
    try {
      Pointcut methodPointcut = toMethodPointcut();
      if (methodPointcut != null) {
        return methodPointcut;
      }
    } catch (Exception e) {
      //pass
    }
    try {
      Pointcut ctorPointcut = toCtorPointcut();
      if (ctorPointcut != null) {
        return ctorPointcut;
      }
    } catch (Exception e) {
      throw new IllegalStateException("Can't create pointcut for signature (" + signature + ")", e);
    }

    throw new IllegalStateException("Can't create pointcut for signature (" + signature + ")");
  }

  @Override
  public int hashCode() {
    return signature != null ? signature.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "MethodDescription{" +
        "signature='" + signature + '\'' +
        '}';
  }

  public static BehaviorDescription fromBehaviour(CtBehavior beh) {
    final StringBuffer parameters = new StringBuffer();
    try {
      final CtClass[] types = beh.getParameterTypes();
      for (int i = 0; i < types.length; i++) {
        parameters.append(types[i].getName());
        if (i < types.length - 1) {
          parameters.append(",");
        }
      }

      final String signature;
      if (beh.getMethodInfo().isConstructor()) {
        signature = String.format("%s(%s)", beh.getDeclaringClass().getName(), parameters);
      } else {
        final String returnType = ((CtMethod) beh).getReturnType().getName();
        signature = String
            .format("%s %s#%s(%s)", returnType, beh.getDeclaringClass().getName(), beh.getName(), parameters);
      }

      return new BehaviorDescription(signature);
    } catch (NotFoundException e) {
      throw new IllegalStateException("Can't construct method description.", e);
    }
  }
}
