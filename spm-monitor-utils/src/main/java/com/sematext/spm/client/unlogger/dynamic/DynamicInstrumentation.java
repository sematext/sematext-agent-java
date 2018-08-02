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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.unlogger.Pointcut;

/**
 * Orchestrate by dynamic transformers:
 * - enable all
 * - disable all
 * - redefine processed classes
 */
public final class DynamicInstrumentation {

  private final Log log = LogFactory.getLog(DynamicInstrumentation.class);

  private final InstrumentationSettings settings;
  private final AgentStatistics statistics;
  private final List<DynamicTransformer> dynamicTransformers;
  private final Instrumentation instrumentation;
  private final AtomicBoolean enabled;
  private final List<Class<?>> predefinedClassesToBeInstrumented;
  private final int userTracedMethodEntryPointDispatchId;
  private final int userTracedMethodNonEntryPointDispatchId;

  public DynamicInstrumentation(InstrumentationSettings settings, AgentStatistics statistics,
                                List<DynamicTransformer> dynamicTransformers,
                                List<Class<?>> predefinedClassesToBeInstrumented, Instrumentation instrumentation,
                                boolean enabled,
                                int userTracedMethodEntryPointDispatchId, int userTracedMethodNonEntryPointDispatchId) {
    this.settings = settings;
    this.statistics = statistics;
    this.dynamicTransformers = dynamicTransformers;
    this.instrumentation = instrumentation;
    this.predefinedClassesToBeInstrumented = predefinedClassesToBeInstrumented;
    this.enabled = new AtomicBoolean(enabled);
    this.userTracedMethodEntryPointDispatchId = userTracedMethodEntryPointDispatchId;
    this.userTracedMethodNonEntryPointDispatchId = userTracedMethodNonEntryPointDispatchId;
  }

  private void reTransformPredefinedClasses() {
    for (final Class<?> klass : predefinedClassesToBeInstrumented) {
      if (instrumentation.isModifiableClass(klass)) {
        try {
          instrumentation.retransformClasses(klass);
        } catch (Exception e) {
          if (log.isDebugEnabled()) {
            log.debug("Can't retransform class " + klass + ".", e);
          }
        }
      } else if (!instrumentation.isModifiableClass(klass)) {
        if (log.isDebugEnabled()) {
          log.debug("Can't retransform class " + klass + " - not modifiable");
        }
      }
    }
  }

  private void reTransformClasses(Set<String> classes) {
    final Set<String> weavedClasses = new HashSet<String>(classes);

    for (final Class<?> klass : instrumentation.getAllLoadedClasses()) {
      if (instrumentation.isModifiableClass(klass) && weavedClasses.contains(klass.getName())) {
        try {
          instrumentation.retransformClasses(klass);
        } catch (Exception e) {
          if (log.isDebugEnabled()) {
            log.debug("Can't retransform class " + klass + ".", e);
          }
        }
      } else if (!instrumentation.isModifiableClass(klass)) {
        if (log.isDebugEnabled()) {
          log.debug("Can't retransform class " + klass + " - not modifiable");
        }
      }
    }
  }

  private void reloadDynamicTransformers() {
    final Map<BehaviorDescription, BehaviorState> behaviorsToBeWeaved = settings.getBehaviorsToBeWeaved();

    final Map<Pointcut, Integer> additionalPointcuts = new HashMap<Pointcut, Integer>();
    for (final Map.Entry<BehaviorDescription, BehaviorState> entry : behaviorsToBeWeaved.entrySet()) {
      final BehaviorDescription descr = entry.getKey();
      try {
        final BehaviorState state = entry.getValue();
        // assuming that state.enabled == true
        final int dispatchId;
        if (state.isEntryPoint()) {
          dispatchId = userTracedMethodEntryPointDispatchId;
        } else {
          dispatchId = userTracedMethodNonEntryPointDispatchId;
        }
        additionalPointcuts.put(descr.toPointcut(), dispatchId);
        log.info("Performing dynamic transform for " + descr + ".");
      } catch (Exception e) {
        log.info("Can't perform dynamic transform for " + descr + ".", e);
      }
    }

    for (DynamicTransformer transformer : dynamicTransformers) {
      transformer.reload(additionalPointcuts);
    }
  }

  private void reTransformClasses() {
    log.info("Start retransform classes");

    reloadDynamicTransformers();

    final Set<String> classes = new HashSet<String>();
    classes.addAll(getWeavedClasses());
    classes.addAll(settings.getClassNamesToBeRetransformed());

    log.info("Preparing to retransform " + classes.size() + " classes.");
    reTransformClasses(classes);
    reTransformPredefinedClasses();
    log.info("Classes retransformed");
  }

  public boolean enable() {
    if (!enabled.compareAndSet(false, true)) {
      return false;
    }

    reTransformClasses();

    return true;
  }

  public boolean disable() {
    if (!enabled.compareAndSet(true, false)) {
      return false;
    }

    reTransformClasses();

    return true;
  }

  public boolean isEnabled() {
    return enabled.get();
  }

  public boolean applyTransformationSettings() {
    if (!enabled.get()) {
      return false;
    }

    reTransformClasses();

    return true;
  }

  public void init() {
    boolean canRetransform = instrumentation.isRetransformClassesSupported();

    if (!canRetransform) {
      log.warn("Retransform classes is disabled. No dynamic transformation will be available. Please look at your appserver settings in order to enable class retransform.");
    }

    for (final DynamicTransformer dynTransformer : dynamicTransformers) {
      final ClassFileTransformer classFileTransformer = new ClassFileTransformer() {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {

          final DynamicTransformer.Parameters params = new DynamicTransformer.Parameters(loader, className, classBeingRedefined,
                                                                                         protectionDomain, classfileBuffer, statistics, enabled
                                                                                             .get(), settings);

          byte[] bytes = dynTransformer.transform(params);

          if (bytes != null) {
            if (log.isDebugEnabled()) {
              log.debug("Transformed class " + className + " [" + dynTransformer.getClass().getName() + "]");
            }
          }

          return bytes;
        }
      };

      instrumentation.addTransformer(classFileTransformer, canRetransform);
    }
  }

  public Set<String> getWeavedClasses() {
    final Set<String> classes = new HashSet<String>();
    for (final DynamicTransformer transformer : dynamicTransformers) {
      classes.addAll(transformer.getWeavedClasses());
    }
    return classes;
  }

}
