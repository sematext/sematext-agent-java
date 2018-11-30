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
package com.sematext.spm.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ErrorTracker {
  private static Long MIN_TIME_BETWEEN_TWO_SAME_MESSAGES = 15 * 60 * 1000L;
  private static Long MAX_NUM_OF_STORED_ERRORS = 30L;
  private Map<Throwable, Long> errorPrintedTimes = new ConcurrentHashMap<Throwable, Long>();
  
  public static final ErrorTracker INSTANCE = new ErrorTracker();
  
  public synchronized void rememberStacktracePrinted(Throwable error) {
    Throwable previousDuplicate = findTrackedDuplicate(error);
    
    errorPrintedTimes.put(previousDuplicate != null ? previousDuplicate : error, System.currentTimeMillis());
  }

  public synchronized boolean shouldPrintStacktrace(Throwable error) {
    return isTracked(error) ? !printedRecently(error) : hasSpaceToTrackAnotherError();
  }
  
  private boolean isTracked(Throwable error) {
    return findTrackedDuplicate(error) != null;
  }
  
  private Throwable findTrackedDuplicate(Throwable error) {
    for (Throwable trackedError : errorPrintedTimes.keySet()) {
      if (matches(trackedError, error)) {
        return trackedError;
      }
    }
    
    return null;
  }

  private boolean matches(Throwable thr1, Throwable thr2) {
    if (thr1 == thr2) {
      return true;
    } else if (thr1 == null || thr2 == null) {
      return false;
    } else {
      return classMatches(thr1, thr2) && stackTraceMatches(thr1, thr2) && matches(thr1.getCause(), thr2.getCause());
    }
  }
  
  private boolean stackTraceMatches(Throwable thr1, Throwable thr2) {
    StackTraceElement[] trace1 = thr1.getStackTrace();
    StackTraceElement[] trace2 = thr2.getStackTrace();    
    boolean sameLength = trace1.length == trace2.length;
    
    if (!sameLength) {
      return false;
    }
    
    for (int i = 0; i < trace1.length; i++) {
      if (!matches(trace1[i], trace2[i])) {
        return false;
      }
    }
    return true;
  }

  private boolean matches(StackTraceElement traceElement1, StackTraceElement traceElement2) {
    if (traceElement1 == null && traceElement2 == null) {
      return true;
    } else if (traceElement1 == null || traceElement2 == null) {
      return false;
    } else {
      return traceElement1.equals(traceElement2);
    }
  }

  private boolean classMatches(Throwable thr1, Throwable thr2) {
    return thr1.getClass().equals(thr2.getClass());
  }

  private boolean printedRecently(Throwable error) {
    if (isTracked(error)) {
      long currentTime = System.currentTimeMillis();
      long previousTime = errorPrintedTimes.get(findTrackedDuplicate(error));
      long timeSinceLast = currentTime - previousTime;
      
      return timeSinceLast < MIN_TIME_BETWEEN_TWO_SAME_MESSAGES;
    } else {
      return false;
    }
  }

  private boolean hasSpaceToTrackAnotherError() {
    return errorPrintedTimes.size() < MAX_NUM_OF_STORED_ERRORS;
  }
}
