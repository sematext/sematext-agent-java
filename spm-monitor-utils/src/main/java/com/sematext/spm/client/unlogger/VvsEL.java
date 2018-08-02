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

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sematext.spm.client.unlogger.StrictMatcher.Result;
import com.sematext.spm.client.util.ReflectionUtils;

/**
 * Very, very simple EL.
 */
public final class VvsEL {

  private VvsEL() {
    // It's utility class, can't be instantiated.
  }

  private static final String FIELD_PATTERN = "(\\p{L}[\\p{L}\\$\\d]*)";
  private static final String SHORT_METHOD = "(\\p{L}+?)\\s*\\((.*?)\\)";
  private static final String FULL_METHOD = "([\\p{L}\\.\\$]+?)#" + SHORT_METHOD;

  public static final String METHOD_SIGNATURE_PATTERN = "\\s*([\\p{L}\\d\\.\\$]+?(?:\\[\\])*?)\\s+([\\p{L}\\d\\.\\$]+?)#(\\p{L}+[\\p{L}\\d\\$]+?)\\s*\\((.*?)\\)\\s*";
  // returnType type#method(unparsedParams)
  private static final String CONSTRUCTOR_SIGNATURE_PATTERN = "\\s*([\\p{L}\\.\\$]+?)\\s*\\((.*?)\\)\\s*";
  // type(unparsedParams)
  private static final String METHOD_PARAMS_PATTERN = "\\s*(\\p{L}[\\p{L}\\.\\$]+(?:\\[\\])*?)\\s+(\\p{L}[\\p{L}\\d]*)\\s*(?:,(?!\\s*$)|$)";

  // (?:,(?!\\s*$)|$) - comma but with some text after it or end of string

  public static final StrictMatcher METHOD_SIGNATURE_MATCHER = StrictMatcher.rootPattern(METHOD_SIGNATURE_PATTERN)
      .reparseGroupAsMap(4, METHOD_PARAMS_PATTERN, 2, 1).make();

  public static final StrictMatcher CONSTRUCTOR_SIGNATURE_MATCHER = StrictMatcher
      .rootPattern(CONSTRUCTOR_SIGNATURE_PATTERN).reparseGroupAsMap(2, METHOD_PARAMS_PATTERN, 2, 1).make();

  public static final StrictMatcher FIELD_SIGNATURE_MATCHER = StrictMatcher.rootPattern(FIELD_PATTERN).make();
  public static final StrictMatcher SHORT_METHOD_MATCHER = StrictMatcher.rootPattern(SHORT_METHOD)
      .reparseGroupAsMap(2, METHOD_PARAMS_PATTERN, 2, 1).make();
  public static final StrictMatcher FULL_METHOD_MATCHER = StrictMatcher.rootPattern(FULL_METHOD)
      .reparseGroupAsMap(3, METHOD_PARAMS_PATTERN, 2, 1).make();

  public enum Chunk {
    FIELD(FIELD_SIGNATURE_MATCHER) {
      @Override
      protected Object eval(Result match, Object object, ClassLoader primaryClassLoader, Map<String, ?> params)
          throws Exception {
        if (object == null) {
          return null;
        }
        String fieldName = match.get(1);
        return ReflectionUtils.getField(object.getClass(), fieldName).get(object);
      }
    },
    SHORT_METHOD(SHORT_METHOD_MATCHER) {
      @Override
      protected Object eval(Result match, Object object, ClassLoader primaryClassLoader, Map<String, ?> params)
          throws Exception {
        if (object == null) {
          return null;
        }
        String methodName = match.get(1);
        LinkedHashMap<String, String> methodParams = match.get(2, LinkedHashMap.class);
        return ReflectionUtils.getMethod(object.getClass(), methodName,
                                         ReflectionUtils.silentGetClasses(methodParams.values(), primaryClassLoader))
            .invoke(object,
                    localParams(methodParams.keySet(), params));
      }
    },
    FULL_METHOD(FULL_METHOD_MATCHER) {
      @Override
      protected Object eval(Result match, Object object, ClassLoader primaryClassLoader, Map<String, ?> params)
          throws Exception {
        String typeName = match.get(1);
        String methodName = match.get(2);
        LinkedHashMap<String, String> methodParams = match.get(3, LinkedHashMap.class);

        Class<?> type = ReflectionUtils.silentGetClass(typeName, primaryClassLoader);
        Class<?>[] paramTypes = ReflectionUtils.silentGetClasses(methodParams.values(), primaryClassLoader);
        return ReflectionUtils.getMethod(type, methodName, paramTypes).invoke(object,
                                                                              localParams(methodParams
                                                                                              .keySet(), params));
      }
    };

    private final StrictMatcher strictMatcher;

    private Chunk(StrictMatcher strictMatcher) {
      this.strictMatcher = strictMatcher;
    }

    public static Object eval(String evalString, Object object, ClassLoader primaryClassLoader, Map<String, ?> params) {
      for (Chunk chunk : values()) {
        // Sorry, depends of enum orders
        StrictMatcher.Result match = chunk.getMatcher().match(evalString);
        if (match != null) {
          try {
            return chunk.eval(match, object, primaryClassLoader, params);
          } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getTargetException());
          } catch (Exception e) {
            return null;
          }

        }
      }
      throw new IllegalStateException("Can't parse EL -> " + evalString);
    }

    protected abstract Object eval(StrictMatcher.Result match, Object object, ClassLoader primaryClassLoader,
                                   Map<String, ?> params) throws Exception;

    protected StrictMatcher getMatcher() {
      return strictMatcher;
    }

    protected static Object[] localParams(Collection<String> paramNames, Map<String, ?> paramsStore) {
      Object[] params = new Object[paramNames.size()];
      int i = 0;
      for (String paramName : paramNames) {
        params[i] = paramsStore.get(paramName);
        i++;
      }
      return params;
    }
  }

  public static Object eval(Object root, String evalString, Map<String, ?> params) {
    if (root == null) {
      return null;
    }
    return eval(root.getClass().getClassLoader(), root, evalString, params);
  }

  public static Object eval(Object root, String evalString) {
    return eval(root, evalString, Collections.<String, Object>emptyMap());
  }

  public static Object evalStatic(ClassLoader rootClassLoader, String evalString, Map<String, ?> params) {
    if (rootClassLoader == null) {
      return null;
    }
    return eval(rootClassLoader, null, evalString, params);
  }

  public static Object evalStatic(ClassLoader rootClassLoader, String evalString) {
    return evalStatic(rootClassLoader, evalString, Collections.<String, Object>emptyMap());
  }

  private static Object eval(ClassLoader primaryClassLoader, Object root, String evalString, Map<String, ?> params) {
    if (root == null && primaryClassLoader == null) {
      return null;
    }

    if (primaryClassLoader == null) {
      primaryClassLoader = root.getClass().getClassLoader();
    }
    String[] chunks = evalString.split("->");
    Object eval = root;
    for (String evalChunk : chunks) {
      eval = Chunk.eval(evalChunk, eval, primaryClassLoader, params);
      if (eval == null) {
        return null;
      }
    }
    return eval;
  }
}
