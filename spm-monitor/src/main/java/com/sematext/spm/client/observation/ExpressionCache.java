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
package com.sematext.spm.client.observation;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

public class ExpressionCache {
  private static final Log LOG = LogFactory.getLog(ExpressionCache.class);
  private static long CACHE_DURATION_MS = 4000;
  private static long LAST_CACHE_RESET_TIME = 0;

  private static final Map<String, Object> EXPRESSION_TO_RESULT = new UnifiedMap<String, Object>();

  private static long countCheckContains = 0;
  private static long countGetResult = 0;
  private static long countAddResult = 0;

  public static synchronized boolean containsResult(String expression) {
    long currentTime = System.currentTimeMillis();
    if (currentTime - LAST_CACHE_RESET_TIME > CACHE_DURATION_MS) {
      EXPRESSION_TO_RESULT.clear();
      LAST_CACHE_RESET_TIME = currentTime;

      LOG.info("Resetting expression cache, contains=" + countCheckContains + ", get=" + countGetResult + ", add="
                   + countAddResult);
      countAddResult = 0;
      countCheckContains = 0;
      countGetResult = 0;

      return false;
    } else {
      countCheckContains++;
      return EXPRESSION_TO_RESULT.containsKey(expression);
    }
  }

  public static synchronized Object getResult(String expression) {
    countGetResult++;
    return EXPRESSION_TO_RESULT.get(expression);
  }

  public static synchronized void addResult(String expression, Object result) {
    countAddResult++;
    EXPRESSION_TO_RESULT.put(expression, result);
  }
}
