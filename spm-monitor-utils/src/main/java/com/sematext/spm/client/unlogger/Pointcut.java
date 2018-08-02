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

import java.util.Collection;
import java.util.Set;

import com.sematext.spm.client.unlogger.pointcuts.UPointcutContext;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.MethodInfo;

public abstract class Pointcut {

  public abstract static class Factory<POINTCUT, FROM> {
    public abstract POINTCUT make(FROM from);

    public final <T extends Collection<? super POINTCUT>> T make(FROM[] values, T res) {
      if (values == null) {
        return res;
      }

      for (FROM from : values) {
        POINTCUT pointcut = make(from);
        if (pointcut != null) {
          res.add(pointcut);
        }
      }
      return res;
    }
  }

  private final String typeName;

  protected Pointcut(String typeName) {
    this.typeName = typeName;
  }

  public String getTypeName() {
    return typeName;
  }

  // /**
  // *
  // * @return
  // */
  // protected abstract String[] getParamNames();

  public abstract <T> T getWellknownParam(String paramName, Object[] params);

  /**
   * @param ctBehavior
   * @return
   */
  public boolean isApplied(CtBehavior ctBehavior) throws NotFoundException {
    MethodInfo methodInfo = ctBehavior.getMethodInfo();
    if (methodInfo.isMethod()) {
      return isApplied((CtMethod) ctBehavior);
    }
    if (methodInfo.isConstructor()) {
      return isApplied((CtConstructor) ctBehavior);
    }
    return false;
  }

  protected abstract boolean isApplied(CtMethod ctMethod) throws NotFoundException;

  protected abstract boolean isApplied(CtConstructor ctConstructor) throws NotFoundException;

  protected abstract void match(CtClass clazz, UPointcutContext ctx, Set<CtBehavior> result) throws NotFoundException;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Pointcut other = (Pointcut) obj;
    if (typeName == null) {
      if (other.typeName != null) {
        return false;
      }
    } else if (!typeName.equals(other.typeName)) {
      return false;
    }
    return true;
  }

  /**
   * For test purposes
   *
   * @return
   */
  public abstract String getAsString();
}
