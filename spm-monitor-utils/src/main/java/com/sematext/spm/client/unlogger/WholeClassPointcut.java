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

import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts.WholeClass;
import com.sematext.spm.client.unlogger.pointcuts.UPointcutContext;

import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;

public final class WholeClassPointcut extends Pointcut {

  public static final Factory<WholeClassPointcut, WholeClass> FACTORY = new Factory<WholeClassPointcut, WholeClass>() {
    @Override
    public WholeClassPointcut make(WholeClass from) {
      return WholeClassPointcut.make(from);
    }
  };

  private final WholeClass.Inheritance inheritance;

  private WholeClassPointcut(String typeName, WholeClass.Inheritance inheritance) {
    super(typeName);
    this.inheritance = inheritance;
  }

  public <T> T getWellknownParam(String paramName, Object[] params) {
    return null;
  }

  ;

  @Override
  protected boolean isApplied(CtMethod ctMethod) throws NotFoundException {
    if (inheritance == WholeClass.Inheritance.DOWN) {
      return WeaverUtils.isPublic(ctMethod) && !WeaverUtils.isStatic(ctMethod);
    }
    return WeaverUtils.isPublic(ctMethod) && !WeaverUtils.isStatic(ctMethod) &&
        ctMethod.getDeclaringClass().getName().equals(getTypeName());
  }

  @Override
  protected void match(CtClass clazz, UPointcutContext ctx, Set<CtBehavior> result) throws NotFoundException {
    if (clazz.getName().equals(getTypeName())) {
      for (CtBehavior beh : clazz.getDeclaredBehaviors()) {
        if (!beh.getMethodInfo().isMethod()) {
          continue;
        }
        if (!WeaverUtils.isAbstract(beh) && !WeaverUtils.isStatic((CtMethod) beh)) {
          result.add(beh);
        }
      }
    }
  }

  @Override
  protected boolean isApplied(CtConstructor ctConstructor) throws NotFoundException {
    return false;
  }

  private static WholeClassPointcut make(WholeClass descriptor) {
    return new WholeClassPointcut(descriptor.className().trim(), descriptor.inheretance());
  }

  public static WholeClassPointcut make(String descriptor) {
    return new WholeClassPointcut(descriptor, WholeClass.Inheritance.NONE);
  }

  @Override
  public String getAsString() {
    return getTypeName();
  }

}
