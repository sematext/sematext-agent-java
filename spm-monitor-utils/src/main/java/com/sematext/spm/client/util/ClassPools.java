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
package com.sematext.spm.client.util;

import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javassist.ClassPool;
import javassist.LoaderClassPath;

public final class ClassPools {

  private static class PoolHolder {
    private final ClassLoader loader;
    private final ClassPool parent;
    private ClassPool pool;
    private final AtomicInteger callsCount = new AtomicInteger(0);

    public PoolHolder(ClassLoader loader, ClassPool parent) {
      this.pool = new ClassPool(parent);
      this.pool.appendClassPath(new LoaderClassPath(loader));
      this.pool.childFirstLookup = true;
      this.loader = loader;
      this.parent = parent;
    }

    public ClassPool get() {
      if (callsCount.incrementAndGet() > 100) {
        pool = new ClassPool(parent);
        pool.appendClassPath(new LoaderClassPath(loader));
        pool.childFirstLookup = true;
        callsCount.set(0);
        return pool;
      }
      return pool;
    }
  }

  private WeakHashMap<ClassLoader, PoolHolder> cache = new WeakHashMap<ClassLoader, PoolHolder>();

  public ClassPool getClassPool(ClassLoader loader) {
    if (loader == null) {
      return ClassPool.getDefault();
    }

    PoolHolder holder = cache.get(loader);
    if (holder != null) {
      return holder.get();
    }

    ClassPool parent = getClassPool(loader.getParent());
    parent.childFirstLookup = true;
    holder = new PoolHolder(loader, parent);

    cache.put(loader, holder);
    return holder.get();
  }

}
