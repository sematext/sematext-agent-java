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
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.unlogger.Pointcut;
import com.sematext.spm.client.unlogger.dispatch.LastInchOfTrampoline;
import com.sematext.spm.client.unlogger.weaver.PriorityWeaver;

public final class UnloggerDynamicTransformer implements DynamicTransformer {

  private final Log log = LogFactory.getLog(UnloggerDynamicTransformer.class);

  private volatile PriorityWeaver weaver;
  private final Set<String> transformedClasses = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
  private final Map<Pointcut, Integer> predefinedPointcuts;

  public UnloggerDynamicTransformer(final Map<Pointcut, Integer> predefinedPointcuts) {
    this.predefinedPointcuts = predefinedPointcuts;
    this.weaver = new PriorityWeaver(predefinedPointcuts, Collections.<Pointcut, Integer>emptyMap(), LastInchOfTrampoline.class
        .getName());
  }

  @Override
  public void reload(Map<Pointcut, Integer> additionalPointcuts) {
    this.weaver = new PriorityWeaver(predefinedPointcuts, additionalPointcuts, LastInchOfTrampoline.class.getName());
  }

  @Override
  public Set<String> getWeavedClasses() {
    return transformedClasses;
  }

  @Override
  public byte[] transform(Parameters p) throws IllegalClassFormatException {
    if (p.isEnabled()) {
      final long time = System.currentTimeMillis();
      try {
        byte[] bytes = weaver.processClassfileBuffer(p.getLoader(), p.getClassfileBuffer(), p.getSettings()
            .getUnloggerTransformerFilter());
        if (bytes != null) {
          transformedClasses.add(p.getClassName().replace('/', '.'));
          if (log.isDebugEnabled()) {
            log.debug("Transformed class " + p.getClassName());
          }
          p.getStatistics().getTotalClassesLoaded().incrementAndGet();
        }
        return bytes;
      } catch (Exception e) {
        if (log.isDebugEnabled()) {
          log.error("Can't retransform class.", e);
        }
        return null;
      } finally {
        p.getStatistics().getTransformationTimeSpentMs().addAndGet(System.currentTimeMillis() - time);
      }
    } else {
      return null;
    }
  }

}
