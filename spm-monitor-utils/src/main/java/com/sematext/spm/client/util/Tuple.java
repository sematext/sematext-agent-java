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

public final class Tuple<T, V> {
  private final T first;
  private final V second;

  private Tuple(T first, V second) {
    this.first = first;
    this.second = second;
  }

  public T getFirst() {
    return first;
  }

  public V getSecond() {
    return second;
  }

  @Override
  public String toString() {
    return String.format("Tuple{first=%s, second=%s}", first, second);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    return Utils.equal(first, ((Tuple) o).first) && Utils.equal(second, ((Tuple) o).second);
  }

  @Override
  public int hashCode() {
    int result = first != null ? first.hashCode() : 0;
    result = 31 * result + (second != null ? second.hashCode() : 0);
    return result;
  }

  public static <T, V> Tuple<T, V> tuple(T first, V second) {
    return new Tuple<T, V>(first, second);
  }
}
