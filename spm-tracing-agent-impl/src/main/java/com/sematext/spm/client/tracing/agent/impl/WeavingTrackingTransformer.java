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
package com.sematext.spm.client.tracing.agent.impl;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.unlogger.Pointcut;
import com.sematext.spm.client.unlogger.WeaverUtils;
import com.sematext.spm.client.util.ClassPools;

import javassist.CtClass;

public final class WeavingTrackingTransformer implements ClassFileTransformer, HasTransfomedClasses {

  private final Log log = LogFactory.getLog(WeavingTrackingTransformer.class);

  private final Set<Pointcut> pointcuts;
  private final Set<String> transformedClasses = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
  private volatile boolean enabled = false;

  public WeavingTrackingTransformer(Set<Pointcut> pointcuts) {
    this.pointcuts = pointcuts;
  }

  @Override
  public Set<String> getTransformedClasses() {
    return transformedClasses;
  }

  public void enable() {
    enabled = true;
  }

  public void disable() {
    enabled = false;
  }

  @Override
  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain, byte[] classfileBuffer)
      throws IllegalClassFormatException {
    if (enabled) {
      try {
        final CtClass klass = new ClassPools().getClassPool(loader)
            .makeClass(new ByteArrayInputStream(classfileBuffer));
        if (!WeaverUtils.computeAffectedBehaviours(pointcuts, klass).isEmpty()) {
          transformedClasses.add(className.replace('/', '.'));
        }
      } catch (Exception e) {
        log.error("Can't compute affected behaviours.", e);
      }
    }
    return null;
  }
}
