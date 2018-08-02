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
package com.sematext.spm.client.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FunctionInvokerConfig extends MetricConfig {
  public enum ConfigFunctionType {
    LONG_GAUGE,
    GAUGE,
    DOUBLE_GAUGE,
    COUNTER,
    DOUBLE_COUNTER,
    TEXT,
    VOID
  }

  private static final Map<String, ConfigFunctionType> FUNC_TYPES = new HashMap<String, ConfigFunctionType>(
      ConfigFunctionType.values().length);
  private static final String ALLOWED_TYPES;

  static {
    for (ConfigFunctionType mt : ConfigFunctionType.values()) {
      FUNC_TYPES.put(mt.name().toLowerCase(), mt);
    }

    String allowedFuncTypes = "(";
    int counter = 0;
    for (String allowed : FUNC_TYPES.keySet()) {
      allowedFuncTypes += ((counter > 0) ? ", " : "") + allowed;
      counter++;
    }
    allowedFuncTypes += ")";
    ALLOWED_TYPES = allowedFuncTypes;
  }

  private String name;
  private List<FunctionInvokerParamConfig> param = Collections.EMPTY_LIST;

  @Override
  public void setType(String type) {
    String tmp = type.trim().toLowerCase();
    ConfigFunctionType tmpType = FUNC_TYPES.get(tmp);
    if (tmpType == null) {
      throw new IllegalArgumentException(
          "Unrecognized function invoker type: " + type + ", allowed types are: " + ALLOWED_TYPES);
    } else {
      this.type = tmpType.name().toLowerCase();
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<FunctionInvokerParamConfig> getParam() {
    return param;
  }

  public void setParam(List<FunctionInvokerParamConfig> param) {
    this.param = param;
  }
}
