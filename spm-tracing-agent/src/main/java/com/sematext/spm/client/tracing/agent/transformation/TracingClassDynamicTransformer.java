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
package com.sematext.spm.client.tracing.agent.transformation;

import java.io.ByteArrayInputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.unlogger.Pointcut;
import com.sematext.spm.client.unlogger.dynamic.DynamicTransformer;
import com.sematext.spm.client.util.ClassPools;
import com.sematext.spm.client.util.InstrumentationUtils;

import javassist.CtClass;

public final class TracingClassDynamicTransformer implements DynamicTransformer {

  private static final Log LOG = LogFactory.getLog(TracingClassDynamicTransformer.class);

  private final List<TracingTransform> transforms;

  public TracingClassDynamicTransformer(List<TracingTransform> transforms) {
    this.transforms = transforms;
  }

  @Override
  public void reload(Map<Pointcut, Integer> additionalPointcuts) {

  }

  @Override
  public Set<String> getWeavedClasses() {
    return Collections.emptySet();
  }

  @Override
  public byte[] transform(Parameters p) throws IllegalClassFormatException {
    final long time = System.currentTimeMillis();
    try {
      final CtClass ctClass = new ClassPools().getClassPool(p.getLoader())
          .makeClass(new ByteArrayInputStream(p.getClassfileBuffer()));
      if (LOG.isTraceEnabled()) {
        LOG.trace("Attempt to transform: " + ctClass.getName());
      }
      final Set<String> hierarchy = InstrumentationUtils.getHierarchy(ctClass);
      boolean transformed = false;
      for (final TracingTransform transform : transforms) {
        if (transform.transform(ctClass, hierarchy)) {
          transformed = true;
        }
      }
      if (transformed) {
        p.getStatistics().getTotalClassesLoaded().incrementAndGet();
        return ctClass.toBytecode();
      }
      return null;
    } catch (Throwable e) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Error while retransforming " + p.getClassName() + ".", e);
      }
      return null;
    } finally {
      p.getStatistics().getTransformationTimeSpentMs().addAndGet(System.currentTimeMillis() - time);
    }

  }
}
