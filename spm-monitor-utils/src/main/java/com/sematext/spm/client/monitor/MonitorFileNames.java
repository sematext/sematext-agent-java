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
package com.sematext.spm.client.monitor;

import java.util.ArrayList;
import java.util.List;

import com.sematext.spm.client.util.Preconditions;
import com.sematext.spm.client.util.StringUtils;

public final class MonitorFileNames {
  private MonitorFileNames() {
  }

  public static RuntimeNameBuilder runtimeConfig() {
    return new RuntimeNameBuilder();
  }

  public static class RuntimeNameBuilder {
    private String subtype;
    private String monitorType;
    private String token;
    private String jvm;

    public RuntimeNameBuilder subtype(String subtype) {
      this.subtype = subtype;
      return this;
    }

    public RuntimeNameBuilder monitorType(String monitorType) {
      this.monitorType = monitorType;
      return this;
    }

    public RuntimeNameBuilder token(String token) {
      this.token = token;
      return this;
    }

    public RuntimeNameBuilder jvm(String jvm) {
      this.jvm = jvm;
      return this;
    }
  }

  public static String config(String subtype, String token, String jvm) {
    Preconditions.checkNotNull(token, "token should be defined");
    Preconditions.checkNotNull(jvm, "jvm name should be defined");

    final List<String> tokens = new ArrayList<String>();
    tokens.add("spm-monitor");
    if (subtype != null) {
      tokens.add(subtype);
    }
    tokens.add("config");
    tokens.add(token);
    tokens.add(jvm);
    return StringUtils.join(tokens, "-") + ".properties";
  }
}
