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
package com.sematext.spm.client.tracing.agent.transformation.transforms;

import java.util.Set;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorUtil;
import com.sematext.spm.client.instrumentation.Mixins;
import com.sematext.spm.client.tracing.agent.transformation.TracingTransform;

import javassist.CtClass;

public final class MixinTransform implements TracingTransform {
  private static final Log LOG = LogFactory.getLog(MixinTransform.class);

  private final String target;
  private final Class<?> iface;

  public MixinTransform(String target, Class<?> iface) {
    this.target = target;
    this.iface = iface;
  }

  public Class<?> getIface() {
    return iface;
  }

  @Override
  public boolean transform(CtClass ctClass, Set<String> hierarchy) {
    if (!ctClass.isInterface() && hierarchy.contains(target)) {
      try {
        final boolean transformed = Mixins.mixin(ctClass, iface);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Performed mixin transform for " + ctClass.getName() + ", (" + iface + ")");
        }
        return transformed;
      } catch (Exception e) {
        if (MonitorUtil.JAVA_MAJOR_VERSION >= 9) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Can't perform mixin transform for " + ctClass.getName() + ", ( " + iface + "), error was: " + e
                .getMessage());
          }
        } else {
          LOG.error("Can't perform mixin transform for " + ctClass.getName() + ", ( " + iface + ").", e);
        }
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "Mixin transform { " + target + ", " + iface + " }";
  }
}
