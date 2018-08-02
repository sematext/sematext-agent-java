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
package com.sematext.spm.client.unlogger.errors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.util.Identity;

public class ExceptionHandler {
  // To prevent calls equals & hashCodes(at least, objects can be uninitialized when we hijack it constructor)
  // we use IdentityHashMap
  private final LinkedHashMap<Identity<Throwable>, Long> monitoredExceptions = moveTopFrontLinkedMap();
  private final LinkedHashMap<Identity<Throwable>, Long> unhandled = moveTopFrontLinkedMap();

  public enum Type {
    HANDLED, UNHNADLED;
  }

  public static final class ExceptionChain {
    private final Throwable root;
    private final long timestamp;
    private final Type type;

    private ExceptionChain(Throwable root, long timestamp, Type type) {
      this.root = root;
      this.timestamp = timestamp;
      this.type = type;
    }

    public boolean isHandled() {
      return type == Type.HANDLED;
    }

    public Throwable getRoot() {
      return root;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public static ExceptionChain handled(Throwable root, long timestamp) {
      return new ExceptionChain(root, timestamp, Type.HANDLED);
    }

    public static ExceptionChain unhandled(Throwable root, long timestamp) {
      return new ExceptionChain(root, timestamp, Type.UNHNADLED);
    }

  }

  public void store(Throwable throwable) {
    Identity<Throwable> entry = Identity.make(throwable);
    if (monitoredExceptions.containsKey(entry)) {
      // That is possible, because our hijack
      // hooks woven to all constructors bodies,
      // and may called multiple times,
      // when one constructor calls another
      return;
    }
    monitoredExceptions.put(entry, System.currentTimeMillis());
  }

  public void storeAsUnhandled(Throwable throwable) {
    store(throwable);
    unhandled.put(Identity.make(throwable), System.currentTimeMillis());
  }

  // So, here we have a two type of exceptions collected for each call
  // "Unhandled" means exceptions which showed to user
  // and catched by setting pointcuts on wellknow functions like HttpServlet#doGet
  // "monitoredExceptions" contains the instancies of monitored exceptions
  // hijacked on time it construction (not time of it raise, sorry)
  // So, "monitoredExceptions" may contains, or may not contains
  // the instances of "Unhandled" exceptions.
  // So, here we try to remove duplicates of exceptions
  // (with give more priority to "unhandled") and
  // take in account exception chains.
  public List<ExceptionChain> collapseChains() {
    return collapseChains((Map<Identity<Throwable>, Long>) unhandled.clone(),
                          (Map<Identity<Throwable>, Long>) monitoredExceptions.clone());
  }

  private static List<ExceptionChain> collapseChains(Map<Identity<Throwable>, Long> roots,
                                                     Map<Identity<Throwable>, Long> allExceptions) {
    for (Map.Entry<Identity<Throwable>, Long> entry : copy(roots.entrySet())) {
      Identity<Throwable> rootIdentity = entry.getKey();
      // That is possibe when that exception
      // in the "tail" of previous
      if (!allExceptions.containsKey(rootIdentity)) {
        continue;
      }
      removeTails(rootIdentity, roots);
      removeWithTails(rootIdentity, allExceptions);
    }

    for (Map.Entry<Identity<Throwable>, Long> entry : copy(allExceptions.entrySet())) {
      Identity<Throwable> rootIdentity = entry.getKey();
      removeTails(rootIdentity, allExceptions);
    }

    List<ExceptionChain> res = new ArrayList<ExceptionChain>();

    for (Map.Entry<Identity<Throwable>, Long> entry : roots.entrySet()) {
      Identity<Throwable> rootIdentity = entry.getKey();
      long timestamp = entry.getValue();
      res.add(ExceptionChain.unhandled(rootIdentity.getItem(), timestamp));
    }

    for (Map.Entry<Identity<Throwable>, Long> entry : allExceptions.entrySet()) {
      Identity<Throwable> rootIdentity = entry.getKey();
      long timestamp = entry.getValue();
      res.add(ExceptionChain.handled(rootIdentity.getItem(), timestamp));
    }

    return res;
  }

  private static void removeWithTails(Identity<Throwable> rootIdentity, Map<Identity<Throwable>, ?> store) {
    store.remove(rootIdentity);
    removeTails(rootIdentity, store);
  }

  private static void removeTails(Identity<Throwable> rootIdentity, Map<Identity<Throwable>, ?> store) {
    Throwable throwable = rootIdentity.getItem();
    if (throwable == null) {
      return;
    }
    Identity<Throwable> tail = Identity.make(throwable.getCause());
    store.remove(tail);
    removeTails(tail, store);
  }

  private static <K, V> LinkedHashMap<K, V> moveTopFrontLinkedMap() {
    return new LinkedHashMap<K, V>(10, 0.75f, true);
  }

  private static <T> List<T> copy(Collection<T> from) {
    return new ArrayList<T>(from);
  }

}
