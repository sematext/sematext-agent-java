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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;

public class SignatureMatcher {
  private final String declaredType;
  private final String returnType;
  private final String methodName;
  private final Collection<String> paramTypes;

  public SignatureMatcher(String declaredType, String methodName, String returnType, Collection<String> paramTypes) {
    this.declaredType = declaredType;
    this.returnType = returnType;
    this.methodName = methodName;
    this.paramTypes = new ArrayList<String>(paramTypes);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((declaredType == null) ? 0 : declaredType.hashCode());
    result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
    result = prime * result + ((paramTypes == null) ? 0 : paramTypes.hashCode());
    result = prime * result + ((returnType == null) ? 0 : returnType.hashCode());
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
    SignatureMatcher other = (SignatureMatcher) obj;
    if (declaredType == null) {
      if (other.declaredType != null) {
        return false;
      }
    } else if (!declaredType.equals(other.declaredType)) {
      return false;
    }
    if (methodName == null) {
      if (other.methodName != null) {
        return false;
      }
    } else if (!methodName.equals(other.methodName)) {
      return false;
    }
    if (paramTypes == null) {
      if (other.paramTypes != null) {
        return false;
      }
    } else if (!paramTypes.equals(other.paramTypes)) {
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

  public static SignatureMatcher make(CtMethod ctMethod) throws NotFoundException {
    CtClass[] paramTypes = ctMethod.getParameterTypes();
    List<String> paramTypesNames = new ArrayList<String>(paramTypes.length);
    for (CtClass ctClass : paramTypes) {
      paramTypesNames.add(ctClass.getName());
    }
    //We use null's for methods,
    //due to the method we hijack all
    //methods with te same name in hierarchy.
    //But for constrcutors we use concrete types.
    return new SignatureMatcher(null, ctMethod.getName(), ctMethod.getReturnType().getName(), paramTypesNames);
  }

  public static SignatureMatcher make(MethodPointcut pointcut) {
    return new SignatureMatcher(null, pointcut.getMethodName(), pointcut.getReturnType(), Arrays.asList(pointcut
                                                                                                            .getParamTypes()));
  }

  public static SignatureMatcher make(CtConstructor ctConstructor) throws NotFoundException {
    CtClass[] paramTypes = ctConstructor.getParameterTypes();
    List<String> paramTypesNames = new ArrayList<String>(paramTypes.length);
    for (CtClass ctClass : paramTypes) {
      paramTypesNames.add(ctClass.getName());
    }
    return new SignatureMatcher(ctConstructor.getDeclaringClass().getName(), "<init>", null, paramTypesNames);
  }

  public static SignatureMatcher make(ConstructorPointcut pointcut) {
    return new SignatureMatcher(pointcut.getTypeName(), "<init>", null, Arrays.asList(pointcut.getParamTypes()));
  }

}
