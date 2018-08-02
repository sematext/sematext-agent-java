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

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ConcretePointcut extends Pointcut {

  private final LinkedHashMap<String, String> paramsToTypes;
  private final String[] paramNames;
  private final String[] paramTypes;

  protected ConcretePointcut(String typeName, LinkedHashMap<String, String> params) {
    super(typeName);

    String[] paramNames = new String[params.size()];
    String[] paramTypes = new String[params.size()];

    int i = 0;
    for (Map.Entry<String, String> entry : params.entrySet()) {
      paramNames[i] = entry.getKey();
      paramTypes[i] = entry.getValue();
      i++;
    }

    this.paramNames = paramNames;
    this.paramTypes = paramTypes;
    this.paramsToTypes = params;

  }

  public String[] getParamNames() {
    return paramNames;
  }

  public String[] getParamTypes() {
    return paramTypes;
  }

  protected final LinkedHashMap<String, String> getParams() {
    return paramsToTypes;
  }

  public <T> T getWellknownParam(String paramName, Object[] params) {

    // paramNames is very short, so it will be faster to iterate
    // through it instead of making a map
    for (int i = 0; i < paramNames.length; i++) {
      if (paramNames[i].equals(paramName)) {
        return i < params.length ? (T) params[i] : null;
      }
    }
    return null;
  }
}
