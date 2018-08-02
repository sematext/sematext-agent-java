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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.unlogger.UnloggerTransformerFilter;

import javassist.CtBehavior;

public final class InstrumentationSettings {
  private final Log log = LogFactory.getLog(InstrumentationSettings.class);

  private final Map<BehaviorDescription, BehaviorState> behaviorToState = new ConcurrentHashMap<BehaviorDescription, BehaviorState>();

  private final UnloggerTransformerFilter unloggerTransformerFilter = new UnloggerTransformerFilter() {
    @Override
    public boolean shouldBeTransformed(CtBehavior behaviour) {
      final BehaviorState state = behaviorToState.get(BehaviorDescription.fromBehaviour(behaviour));
      return state != null ? state.isEnabled() : true;
    }
  };

  public UnloggerTransformerFilter getUnloggerTransformerFilter() {
    return unloggerTransformerFilter;
  }

  public void updateBehaviorState(BehaviorDescription descr, BehaviorState state) {
    behaviorToState.put(descr, state);
    log.info("Current behaviors: " + behaviorToState + ".");
  }

  public void update(Map<BehaviorDescription, BehaviorState> state) {
    behaviorToState.putAll(state);
  }

  public Set<String> getClassNamesToBeRetransformed() {
    final Set<String> classesToBeWeaved = new HashSet<String>();

    for (final BehaviorDescription descr : behaviorToState.keySet()) {
      try {
        classesToBeWeaved.add(descr.toPointcut().getTypeName());
      } catch (Exception e) {
        log.error("Can't add class to be weaved.", e);
      }
    }

    return classesToBeWeaved;
  }

  public Map<BehaviorDescription, BehaviorState> getBehaviorsToBeWeaved() {
    final Map<BehaviorDescription, BehaviorState> behaviorsToBeWeaved = new HashMap<BehaviorDescription, BehaviorState>();
    for (Map.Entry<BehaviorDescription, BehaviorState> entry : behaviorToState.entrySet()) {
      if (entry.getValue().isEnabled()) {
        behaviorsToBeWeaved.put(entry.getKey(), entry.getValue());
      }
    }
    return behaviorsToBeWeaved;
  }
}
