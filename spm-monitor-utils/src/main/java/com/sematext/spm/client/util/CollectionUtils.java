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

import java.lang.reflect.Array;
import java.util.*;

public final class CollectionUtils {

  private CollectionUtils() {
    // It's utility class
  }

  /**
   * Returns first value from the collection, if exists. Otherwise it returns null.
   *
   * @param collection
   * @return
   */
  public static <T> T first(Collection<T> collection) {
    return (collection == null || collection.isEmpty()) ? null : collection.iterator().next();
  }

  public static <K, V> Set<V> flatByKey(Collection<Map<K, V>> collection, K key) {
    if (collection == null) {
      return new HashSet<V>();
    }
    Set<V> res = new LinkedHashSet<V>(collection.size());
    for (Map<K, V> map : collection) {
      res.add(map.get(key));
    }
    return res;
  }

  public static <T> List<T> copyAll(Queue<T> queue) {
    List<T> ret = new ArrayList<T>();
    T row = queue.poll();
    while (row != null) {
      ret.add(row);
      row = queue.poll();
    }
    return ret;
  }

  public static <T> Iterator<T> decorate(final Iterator<T> iterator, final Function<T, T> functor) {
    return transform(iterator, functor);
  }

  public static <F, T> Iterator<T> transform(final Iterator<F> iterator, final Function<F, T> functor) {
    return new Iterator<T>() {
      private T internalNext;

      @Override
      public boolean hasNext() {
        if (internalNext == null) {
          // find next available (non-null) row
          findNext();
        }

        return (internalNext != null);
      }

      private void findNext() {
        while (iterator.hasNext()) {
          internalNext = functor.apply(iterator.next());

          if (internalNext != null) {
            break;
          }
        }
      }

      @Override
      public T next() {
        if (internalNext == null) {
          findNext();
        }

        T tmp = internalNext;
        internalNext = null;

        return tmp;
      }

      @Override
      public void remove() {
        iterator.remove();
      }
    };
  }

  public interface Function<F, T> {
    T apply(F orig);
  }

  public static interface Function0<R> {
    R apply();
  }

  public static <F> Function<F, Boolean> notContains(final Collection<F> notIn) {
    return new Function<F, Boolean>() {
      @Override
      public Boolean apply(F orig) {
        return notIn.contains(orig);
      }

    };
  }

  // CHECKSTYLE:OFF
  public interface FunctionT<FROM, TO, E extends Exception> {
    TO apply(FROM orig) throws E;
  }

  // Workaround to stupid checkstyle bug.
  // Checkstyle falls on constructions like () throws E, where E is generic.
  // It tries to resolve E as real class and throws RuntimeException.
  // Constructions like 'CHECKSTYLE_OFF' didn't work because
  // checkstyle initially perform analisys of whole AST,
  // and only after that suppress warning/errors for marked pieces.
  // So, we wrote fake class 'E' to make checkstyle happy.
  private static final class E {
  }

  // CHECKSTYLE:ON

  public static <F> Iterator<F> iterator(F value) {
    return Collections.singletonList(value).iterator();
  }

  public static <F> Iterator<F> emptyIterator() {
    return Collections.<F>emptyList().iterator();
  }

  public static <T> List<T> join(T first, T... others) {
    List<T> res = new ArrayList<T>(1 + others.length);
    res.add(first);
    res.addAll(Arrays.asList(others));
    return res;
  }

  public static <T> T[] join(Class<T> type, T[]... elems) {
    List<T> res = new ArrayList<T>();
    for (T[] chunk : elems) {
      res.addAll(Arrays.asList(chunk));
    }
    return res.toArray((T[]) Array.newInstance(type, res.size()));

  }
}
