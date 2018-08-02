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

import static com.sematext.spm.client.unlogger.dispatch.DispatchUnit.registerDispatchUnit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.tracing.agent.config.Config;
import com.sematext.spm.client.tracing.agent.pointcuts.custom.ExtensionPointcut;
import com.sematext.spm.client.tracing.agent.tracer.Tracer;
import com.sematext.spm.client.tracing.agent.tracer.Tracers;
import com.sematext.spm.client.tracing.agent.transformation.TracingClassDynamicTransformer;
import com.sematext.spm.client.tracing.agent.transformation.TracingTransform;
import com.sematext.spm.client.unlogger.Logspect;
import com.sematext.spm.client.unlogger.Pointcut;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.dispatch.AdviceDispatcher;
import com.sematext.spm.client.unlogger.dispatch.LastInchOfTrampoline;
import com.sematext.spm.client.unlogger.dynamic.DynamicTransformer;
import com.sematext.spm.client.unlogger.dynamic.UnloggerDynamicTransformer;
import com.sematext.spm.client.unlogger.dynamic.WeavingClassTrackerDynamicTransformer;
import com.sematext.spm.client.unlogger.xml.CustomPointcutOptions;
import com.sematext.spm.client.unlogger.xml.InstrumentationLoaderException;
import com.sematext.spm.client.unlogger.xml.XMLInstrumentationDescriptorLoader;
import com.sematext.spm.client.util.ReflectionUtils;

/**
 * 'Artifacts' needed for tracing:
 * - creates predefined logspects
 * - creates user defined logspects (from .xml)
 * - creates 'DynamicTransformer' instances for logspects
 */
public final class TracingArtifacts {

  private final Log log = LogFactory.getLog(TracingArtifacts.class);

  private final ClassLoader loader;
  private final Config config;
  private final List<DynamicTransformer> dynamicTransformers = new ArrayList<DynamicTransformer>();
  private int userTracedMethodEntryPointDispatchId;
  private int userTracedMethodNonEntryPointDispatchId;

  private TracingArtifacts(ClassLoader loader, Config config) {
    this.loader = loader;
    this.config = config;
  }

  private List<Logspect> loadExtensionLogspects(File file) throws IOException, InstrumentationLoaderException {
    FileInputStream is = null;
    try {
      is = new FileInputStream(file);
      final XMLInstrumentationDescriptorLoader loader = new XMLInstrumentationDescriptorLoader(ExtensionPointcut.class);
      return loader.load(is).getLogspects();
    } finally {
      if (is != null) {
        is.close();
      }
    }
  }

  private List<Logspect> loadExtensionLogspects() {
    final List<Logspect> logspects = new ArrayList<Logspect>();
    final File extensionsDir = new File(config.getExtensionsPath());
    if (!extensionsDir.exists()) {
      log.info("Extensions dir '" + extensionsDir + "' not exists skipping loading extensions.");
    } else {
      File[] files = extensionsDir.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.getName().endsWith(".xml")) {
            try {
              logspects.addAll(loadExtensionLogspects(file));
              log.info("Loaded extension '" + file + "'.");
            } catch (Exception e) {
              log.error("Can't load extension '" + file + "'.", e);
            }
          }
        }
      }
    }
    return logspects;
  }

  private Map<Pointcut, Integer> registerLoggers(Collection<? extends Logspect> loggers) {
    final Map<Pointcut, Integer> pointcutToDispatchId = new HashMap<Pointcut, Integer>();
    for (Logspect logger : loggers) {
      processLoggerClass(logger, pointcutToDispatchId);
    }

    return pointcutToDispatchId;
  }

  private static void processLoggerClass(Logspect logspect, Map<Pointcut, Integer> toWeave) {
    final UnloggableLogger logger = logspect.createUnlogger();

    for (Pointcut pointcut : logspect.getPointcuts()) {
      int dispatchId = registerDispatchUnit(logspect.getName(), pointcut, logger);
      toWeave.put(pointcut, dispatchId);
    }
  }

  private static Integer registerUserTracedMethodLogspect(boolean entryPoint) {
    final CustomPointcutOptions options = new CustomPointcutOptions(entryPoint, null);
    final Logspect logspect = new Logspect(
        "UserTracedMethodLogspectEntryPoint" + entryPoint, ExtensionPointcut.class, Collections.<Pointcut>emptyList(),
        ReflectionUtils.ClassValue.cv(CustomPointcutOptions.class, options));
    return registerDispatchUnit(logspect.getName(), null, logspect.createUnlogger());
  }

  private void initialize() {
    final List<TracingTransform> structuralTransforms = new ArrayList<TracingTransform>();
    final List<Logspect> logspects = new ArrayList<Logspect>();

    for (Tracers value : Tracers.values()) {
      Tracer tracer = value.getTracer();
      if (tracer.enabled(config) && !Boolean.getBoolean("spm.tracer.disable." + tracer.getName())) {
        logspects.addAll(tracer.createLogspects(loader));
        structuralTransforms.addAll(Arrays.asList(tracer.getStructuralTransforms()));
      } else {
        log.info("Skipping " + tracer.getName() + " tracer.");
      }
    }

    logspects.addAll(loadExtensionLogspects());

    LastInchOfTrampoline.switchTo(AdviceDispatcher.Type.DEFAULT);

    final Set<Pointcut> pointcuts = new HashSet<Pointcut>();

    for (Logspect logspect : logspects) {
      pointcuts.addAll(logspect.getPointcuts());
    }

    /**
     * Register special logspects which should be used to dynamically instrument
     * custom methods.
     */
    userTracedMethodEntryPointDispatchId = registerUserTracedMethodLogspect(true);
    userTracedMethodNonEntryPointDispatchId = registerUserTracedMethodLogspect(false);

    final Map<Pointcut, Integer> pointcutToDispatchId = registerLoggers(logspects);

    if (!Boolean.getBoolean("spm.disableTracingClassTransformer")) {
      dynamicTransformers.add(new TracingClassDynamicTransformer(structuralTransforms));
    } else {
      log.warn("Disabled TracingClassDynamicTransformer");
    }
    if (!Boolean.getBoolean("spm.disableUnloggerTransformer")) {
      dynamicTransformers.add(new UnloggerDynamicTransformer(pointcutToDispatchId));
    } else {
      log.warn("Disabled UnloggerDynamicTransformer");
    }
    if (!Boolean.getBoolean("spm.disableWeavingTransformer")) {
      dynamicTransformers.add(new WeavingClassTrackerDynamicTransformer(pointcuts));
    } else {
      log.warn("Disabled WeavingClassTrackerDynamicTransformer");
    }
  }

  public int getUserTracedMethodEntryPointDispatchId() {
    return userTracedMethodEntryPointDispatchId;
  }

  public int getUserTracedMethodNonEntryPointDispatchId() {
    return userTracedMethodNonEntryPointDispatchId;
  }

  public List<DynamicTransformer> getDynamicTransformers() {
    return dynamicTransformers;
  }

  public List<Class<?>> getClassesToBeRetransformed() {
    return Arrays.asList(Runnable.class, Thread.class, AbstractExecutorService.class, ThreadPoolExecutor.class);
  }

  public static TracingArtifacts create(ClassLoader loader, Config config) {
    final TracingArtifacts artifacts = new TracingArtifacts(loader, config);
    artifacts.initialize();
    return artifacts;
  }

}
