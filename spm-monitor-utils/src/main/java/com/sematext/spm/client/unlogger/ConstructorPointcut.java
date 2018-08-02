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

import static com.sematext.spm.client.unlogger.VvsEL.CONSTRUCTOR_SIGNATURE_MATCHER;

import java.util.LinkedHashMap;
import java.util.Set;

import com.sematext.spm.client.unlogger.pointcuts.UPointcutContext;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;

public final class ConstructorPointcut extends ConcretePointcut {

  public static final Factory<ConstructorPointcut, String> FACTORY = new Factory<ConstructorPointcut, String>() {
    @Override
    public ConstructorPointcut make(String stringRepresentation) {
      return ConstructorPointcut.make(stringRepresentation);
    }
  };

  private ConstructorPointcut(String typeName, LinkedHashMap<String, String> params) {
    super(typeName, params);
  }

  @Override
  protected boolean isApplied(CtMethod ctMethod) throws NotFoundException {
    return false;
  }

  @Override
  protected boolean isApplied(CtConstructor ctConstructor) throws NotFoundException {
    return SignatureMatcher.make(this).equals(SignatureMatcher.make(ctConstructor));
  }

  @Override
  protected void match(CtClass clazz, UPointcutContext ctx, Set<CtBehavior> result) throws NotFoundException {
    if (ctx.getHierarchy().contains(getTypeName())) {
      for (CtBehavior beh : clazz.getDeclaredBehaviors()) {
        if (beh.getMethodInfo().isConstructor()) {
          if (SignatureMatcher.make(this).equals(SignatureMatcher.make((CtConstructor) beh))) {
            result.add(beh);
          }
        }
      }
    }
  }

  @Override
  public String getAsString() {
    return getTypeName() + "#<init>(" + getParamNames() + ")";
  }

  @Override
  public String toString() {
    return "ConstructorPointcut[ typeName='" + getTypeName() + "']";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((getParams() == null) ? 0 : getParams().hashCode());
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
    ConstructorPointcut other = (ConstructorPointcut) obj;
    if (getParams() == null) {
      if (other.getParams() != null) {
        return false;
      }
    } else if (!getParams().equals(other.getParams())) {
      return false;
    }
    return true;
  }

  private static ConstructorPointcut make(String stringRepresentation) {
    // We should not use anything more complicated than regexp
    // because we should eliminate dependencies for code that can be run
    // in agent as much as possible.
    StrictMatcher.Result constructorMatcher = CONSTRUCTOR_SIGNATURE_MATCHER.match(stringRepresentation);
    if (constructorMatcher == null) {
      throw new IllegalArgumentException("Can't recognize pointcut signature -> " + stringRepresentation);
    }

    String typeName = constructorMatcher.get(1);
    LinkedHashMap<String, String> params = constructorMatcher.get(2, LinkedHashMap.class);

    return new ConstructorPointcut(typeName, params);
  }

}
