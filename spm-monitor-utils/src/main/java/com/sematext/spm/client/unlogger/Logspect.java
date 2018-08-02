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

import static com.sematext.spm.client.util.CollectionUtils.join;
import static com.sematext.spm.client.util.ReflectionUtils.ClassValue.cvu;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;
import com.sematext.spm.client.util.CollectionUtils.Function;
import com.sematext.spm.client.util.ReflectionUtils;
import com.sematext.spm.client.util.ReflectionUtils.ClassValue;

/**
 * It is too pathetic to name that as Aspect. So, it is Logspect - object contains information about logging aspects -
 * name, pointcuts, etc...
 */
public final class Logspect {

  private static final Log LOG = LogFactory.getLog(Logspect.class);

  private static final Function<String, Boolean> ALLOW_ALL = new Function<String, Boolean>() {
    @Override
    public Boolean apply(String orig) {
      return false;
    }
  };

  private final String name;
  private final Class<? extends UnloggableLogger> adviceType;
  private final Collection<Pointcut> pointcuts;
  private final ClassValue<?> cv;

  public Logspect(String name, Class<? extends UnloggableLogger> adviceType, Collection<Pointcut> pointcuts,
                  ClassValue<?> cv) {
    this.name = name;
    this.adviceType = adviceType;
    this.pointcuts = pointcuts;
    this.cv = cv;
  }

  public Logspect(String name, Class<? extends UnloggableLogger> adviceType, Collection<Pointcut> pointcuts) {
    this(name, adviceType, pointcuts, null);
  }

  public String getName() {
    return name;
  }

  public Class<? extends UnloggableLogger> getAdviceType() {
    return adviceType;
  }

  public Collection<Pointcut> getPointcuts() {
    return pointcuts;
  }

  public <T> UnloggableLogger makeInstance(ClassValue<?>... cv) {
    UnloggableLogger logger = ReflectionUtils.instance(adviceType, cv);
    if (logger != null) {
      return logger;
    }
    return ReflectionUtils.instance(adviceType);
  }

  public UnloggableLogger createUnlogger() {
    if (this.cv != null) {
      return makeInstance(this.cv);
    }
    return ReflectionUtils.instance(adviceType);
  }

  @Override
  public String toString() {
    return "Logspect [name=" + name + ", type=" + adviceType + ", pointcuts=" + pointcuts + "]";
  }

  public static Collection<Logspect> make(String[] loggerClassNames, ClassLoader classLoader) {
    return make(loggerClassNames, ALLOW_ALL, classLoader);
  }

  public static Collection<Logspect> make(String[] loggerClassNames, Function<String, Boolean> pointcutNameFilter,
                                          ClassLoader loader) {
    List<Logspect> res = new ArrayList<Logspect>(loggerClassNames.length);
    for (String loggerClassName : loggerClassNames) {
      Logspect logspect = constructLogspect(loggerClassName, pointcutNameFilter, loader);
      if (logspect != null) {
        res.add(logspect);
      }
    }
    return res;
  }

  private static Logspect constructLogspect(String loggerClassName, Function<String, Boolean> pointcutNameFilter,
                                            ClassLoader loader) {
    Class<? extends UnloggableLogger> loggerClass = loggerClass(loggerClassName, loader);
    if (loggerClass == null) {
      LOG.warn("Can't find logger class -> " + loggerClassName + " (using classloader: " + loader + ")");
      return null;
    }
    Set<Pointcut> pointcuts = pointcuts(loggerClass);
    if (pointcuts == null || pointcuts.isEmpty()) {
      return null;
    }
    String name = name(loggerClass);
    if (name == null) {
      LOG.warn("Logger class -> " + loggerClassName + " does not have name");
      return null;
    }

    if (pointcutNameFilter.apply(name)) {
      LOG.info("Logger class -> " + loggerClassName + " skipped");
      return null;
    }

    return new Logspect(name, loggerClass, pointcuts);
  }

  private static String name(Class<? extends UnloggableLogger> loggerClass) {
    LoggerPointcuts loggerPointcuts = loggerClass.getAnnotation(LoggerPointcuts.class);
    if (loggerPointcuts == null) {
      return null;
    }
    return loggerPointcuts.name();
  }

  private static Set<Pointcut> pointcuts(Class<? extends UnloggableLogger> loggerClass) {
    LoggerPointcuts loggerPointcuts = loggerClass.getAnnotation(LoggerPointcuts.class);
    if (loggerPointcuts == null) {
      return null;
    }

    Set<Pointcut> pointcuts = new LinkedHashSet<Pointcut>();
    MethodPointcut.make(loggerPointcuts.methods(), loggerPointcuts.ignorePatterns(), pointcuts);
    ConstructorPointcut.FACTORY.make(loggerPointcuts.constructors(), pointcuts);
    WholeClassPointcut.FACTORY.make(loggerPointcuts.classes(), pointcuts);
    MethodAnnotationPointcut.FACTORY.make(loggerPointcuts.methodAnnotations(), pointcuts);
    return pointcuts;
  }

  private static Class<? extends UnloggableLogger> loggerClass(String loggerClassName, ClassLoader loader) {
    Class<?> claxx = ReflectionUtils.silentGetClass(loggerClassName, loader);
    if (claxx == null) {
      return null;
    }

    if (!UnloggableLogger.class.isAssignableFrom(claxx)) {
      return null;
    }

    return (Class<? extends UnloggableLogger>) claxx;
  }

  /**
   * A la google guice. Construct set of objects which described at annotations.
   *
   * @param markerType
   * @param restType
   * @param cv
   * @return
   */
  public <R> List<R> guice(Class<? extends Annotation> markerType, Class<R> restType, ClassValue<?>... cv) {
    return guice(getAdviceType(), markerType, restType, cv);
  }

  private static <R> List<R> guice(Class<?> from, Class<? extends Annotation> markerType, Class<R> restType,
                                   ClassValue<?>... cv) {
    List<Annotation> markered = ReflectionUtils.getAnnotatedAnnotations(from, markerType);

    List<R> res = new ArrayList<R>(markered.size());
    for (Annotation config : markered) {
      Class<?> configType = config.annotationType();
      Annotation marker = configType.getAnnotation(markerType);
      Class<? extends R> resultType = ReflectionUtils.silentMethodGet(marker, "value");
      R r = ReflectionUtils.instance(resultType, join(cvu(configType, config), cv));
      res.add(r);
    }
    return res;
  }
}
