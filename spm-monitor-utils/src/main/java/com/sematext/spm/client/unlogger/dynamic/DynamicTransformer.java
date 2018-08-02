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

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;

import com.sematext.spm.client.unlogger.Pointcut;

public interface DynamicTransformer {

  public static class Parameters {
    private final ClassLoader loader;
    private final String className;
    private final Class<?> classBeingRedefined;
    private final ProtectionDomain protectionDomain;
    private final byte[] classfileBuffer;
    private final AgentStatistics statistics;
    private final boolean enabled;
    private final InstrumentationSettings settings;
    private final AdditionalPointcuts pointcuts = new AdditionalPointcuts();

    public Parameters(ClassLoader loader, String className, Class<?> classBeingRedefined,
                      ProtectionDomain protectionDomain, byte[] classfileBuffer, AgentStatistics statistics,
                      boolean enabled, InstrumentationSettings settings) {
      this.loader = loader;
      this.className = className;
      this.classBeingRedefined = classBeingRedefined;
      this.protectionDomain = protectionDomain;
      this.classfileBuffer = classfileBuffer;
      this.statistics = statistics;
      this.enabled = enabled;
      this.settings = settings;
    }

    public ClassLoader getLoader() {
      return loader;
    }

    public String getClassName() {
      return className;
    }

    public Class<?> getClassBeingRedefined() {
      return classBeingRedefined;
    }

    public ProtectionDomain getProtectionDomain() {
      return protectionDomain;
    }

    public byte[] getClassfileBuffer() {
      return classfileBuffer;
    }

    public AgentStatistics getStatistics() {
      return statistics;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public InstrumentationSettings getSettings() {
      return settings;
    }
  }

  void reload(Map<Pointcut, Integer> additionalPointcuts);

  Set<String> getWeavedClasses();

  byte[] transform(Parameters p) throws IllegalClassFormatException;
}
