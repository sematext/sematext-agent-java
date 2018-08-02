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
package com.sematext.spm.client.unlogger.dispatch;

import static com.sematext.spm.client.unlogger.dispatch.DispatchUnit.getDispatchUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.unlogger.JoinPoint;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.Pointcut;
import com.sematext.spm.client.unlogger.TimerImpl;

public final class DefaultAdviceDispatcher implements AdviceDispatcher {

  protected DefaultAdviceDispatcher() {
  }

  private static class ContextHolder {
    private final ThreadLocal<DefaultLoggerContext> context = new ThreadLocal<DefaultLoggerContext>() {
      @Override
      protected DefaultLoggerContext initialValue() {
        return new DefaultLoggerContext();
      }
    };

    public DefaultLoggerContext getContext() {
      context.get().checkFrame();
      return context.get();
    }

    public DefaultLoggerContext enterFrame() {
      context.get().enterFrame();
      return context.get();
    }

    public DefaultLoggerContext leaveFrame() {
      context.get().leaveFrame();
      return context.get();
    }

  }

  private final ContextHolder contextHolder = new ContextHolder();

  private static final Log LOG = LogFactory.getLog(DispatchUnit.class);

  public void logBefore(int dispatchId, JoinPoint joinPoint, Object that, Object[] params) {
    DispatchUnit dispatchUnit = getDispatchUnit(dispatchId);
    if (dispatchUnit == null) {
      return;
    }

    try {
      DefaultLoggerContext context = contextHolder.enterFrame();
      dispatchUnit.logBefore(context, joinPoint, that, params);
    } catch (Exception e) {
      if (MonitorUtil.JAVA_MAJOR_VERSION >= 9) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Could not dispatch 'logBefore' method, error was: " + e.getMessage());
        }
      } else {
        LOG.error("Could not dispatch 'logBefore' method", e);
      }
    }

  }

  public void logAfter(int dispatchId, JoinPoint joinPoint, Object that, Object returnValue) {
    DispatchUnit dispatchUnit = getDispatchUnit(dispatchId);
    if (dispatchUnit == null) {
      return;
    }

    try {
      DefaultLoggerContext context = contextHolder.getContext();
      try {
        dispatchUnit.logAfter(context, joinPoint, that, returnValue);
      } finally {
        if (contextHolder.leaveFrame().isLastFrame()) {
          closeCallSequence(contextHolder.getContext());
        }
      }
    } catch (Exception e) {
      if (MonitorUtil.JAVA_MAJOR_VERSION >= 9) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Could not dispatch 'logAfter' method. That: " + that + ", error was: " + e.getMessage());
        }
      } else {
        LOG.error("Could not dispatch 'logAfter' method. That: " + that, e);
      }
    }

  }

  public void logThrow(int dispatchId, JoinPoint joinPoint, Object that, Throwable throwable) {
    DispatchUnit dispatchUnit = getDispatchUnit(dispatchId);
    if (dispatchUnit == null) {
      return;
    }

    try {
      DefaultLoggerContext context = contextHolder.getContext();
      try {
        dispatchUnit.logThrow(context, joinPoint, that, throwable);
      } finally {
        if (contextHolder.leaveFrame().isLastFrame()) {
          closeCallSequence(contextHolder.getContext());
        }
      }
    } catch (Exception e) {
      LOG.error("Could not dispatch 'logThrow' method", e);
    }
  }

  private void closeCallSequence(DefaultLoggerContext context) {
    // add flush for buffered schemes
  }

  protected static class DefaultLoggerContext implements LoggerContext {
    private static final AtomicInteger VM_CALL_SEQUENCE_ID_GENERATOR = new AtomicInteger(0);

    private int globalId;
    private int callId;
    private Frame currentFrame;
    private final Map<String, Object> sequenceParams = new HashMap<String, Object>();
    private final ArrayList<Frame> frames = new ArrayList<Frame>(0x100);
    private int callStackFlag;

    private static class Frame {
      // If somebody add new parameter, don't forget add clear for it
      // CHECKSTYLE:OFF
      String sectionName;
      Object that;
      JoinPoint joinPoint;
      Pointcut pointcut;
      Object[] params;
      int callFlag;

      private final Timer timer;

      // CHECKSTYLE:ON

      public Frame() {
        timer = new TimerImpl();
      }

      public Frame(Timer timer) {
        this.timer = timer;
      }

      public void clear() {
        callFlag = 0;
        sectionName = null;
        that = null;
        joinPoint = null;
        pointcut = null;
        params = null;
        timer.clear();
      }

      public Frame createNext() {
        return new Frame(timer.createChild());
      }
    }

    public DefaultLoggerContext() {
    }

    @Override
    public <T> T getMethodParam(String paramName) {
      return currentFrame.pointcut.getWellknownParam(paramName, currentFrame.params);
    }

    @Override
    public int getVmCallSequenceId() {
      return globalId;
    }

    @Override
    public int getRelativeCallDepth() {
      return callId;
    }

    @Override
    public String getSection() {
      return currentFrame.sectionName;
    }

    protected void setSection(String sectionName) {
      currentFrame.sectionName = sectionName;
    }

    @Override
    public Object getThat() {
      return currentFrame.that;
    }

    protected void setThat(Object that) {
      currentFrame.that = that;
    }

    @Override
    public JoinPoint getJoinPoint() {
      return currentFrame.joinPoint;
    }

    protected void setJoinPoint(JoinPoint joinPoint) {
      currentFrame.joinPoint = joinPoint;
    }

    @Override
    public void setSequenceLevelParam(String paramName, Object value) {
      sequenceParams.put(paramName, value);
    }

    @Override
    public <T> T getSequenceLevelParam(String paramName) {
      return (T) sequenceParams.get(paramName);
    }

    @Override
    public Timer getTimer() {
      return currentFrame.timer;
    }

    @Override
    public Pointcut getPointcut() {
      return currentFrame.pointcut;
    }

    protected void setPointcut(Pointcut pointcut, Object[] params) {
      currentFrame.pointcut = pointcut;
      currentFrame.params = params;
    }

    @Override
    public Object[] getAllParams() {
      return currentFrame.params;
    }

    private Frame constructFrame(int num) {
      if (num < frames.size()) {
        return frames.get(num);
      }
      if (num == frames.size()) {
        Frame frame = num != 0 ? frames.get(num - 1).createNext() : new Frame();
        frames.add(frame);
        return frame;
      }
      throw new IllegalStateException("Try to create non-monotone frame!!!!!");
    }

    public void checkFrame() {
      if (currentFrame == null) {
        // Add special error context - ?
        enterFrame();
      }
    }

    public void enterFrame() {
      if (currentFrame == null) {
        globalId = VM_CALL_SEQUENCE_ID_GENERATOR.incrementAndGet();
        callId = 0;
        currentFrame = constructFrame(callId);
      } else {
        currentFrame = constructFrame(++callId);
      }
    }

    public void leaveFrame() {
      if (currentFrame == null) {
        callId = 0;
        return;
      }
      currentFrame.clear();
      currentFrame = callId != 0 ? frames.get(--callId) : null;
    }

    public boolean isLastFrame() {
      return currentFrame == null;
    }

    @Override
    public int getCallFlag() {
      return currentFrame.callFlag;
    }

    @Override
    public void setCallFlag(int flag) {
      this.currentFrame.callFlag = flag;
    }

    @Override
    public int getCallStackFlag() {
      return callStackFlag;
    }

    @Override
    public void setCallStackFlag(int flag) {
      this.callStackFlag = flag;
    }
  }

  @Override
  public Type getType() {
    return Type.DEFAULT;
  }

}
