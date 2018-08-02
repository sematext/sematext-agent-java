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

import java.io.ByteArrayInputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.sematext.spm.client.unlogger.Pointcut;
import com.sematext.spm.client.unlogger.WeaverUtils;
import com.sematext.spm.client.util.ClassPools;

import javassist.CtClass;

public final class WeavingClassTrackerDynamicTransformer implements DynamicTransformer {
  private final Set<String> transformedClasses = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
  private final Set<Pointcut> pointcuts;
  private final ReentrantLock lock = new ReentrantLock();

  public WeavingClassTrackerDynamicTransformer(Set<Pointcut> pointcuts) {
    this.pointcuts = pointcuts;
  }

  @Override
  public void reload(Map<Pointcut, Integer> additionalPointcuts) {

  }

  @Override
  public Set<String> getWeavedClasses() {
    return transformedClasses;
  }

  @Override
  public byte[] transform(Parameters p) throws IllegalClassFormatException {
    try {
      final CtClass klass = new ClassPools().getClassPool(p.getLoader())
          .makeClass(new ByteArrayInputStream(p.getClassfileBuffer()));
      if (!WeaverUtils.computeAffectedBehaviours(pointcuts, klass, null).isEmpty()) {
        transformedClasses.add(p.getClassName().replace('/', '.'));
      }
    } catch (Exception e) {
      // pass
    } finally {
    }
    return null;
  }
}
