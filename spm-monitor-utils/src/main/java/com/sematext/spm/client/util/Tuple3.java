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

public final class Tuple3<V1, V2, V3> {
  private final V1 first;
  private final V2 second;
  private final V3 third;

  public Tuple3(V1 first, V2 second, V3 third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }

  public V1 getFirst() {
    return first;
  }

  public V2 getSecond() {
    return second;
  }

  public V3 getThird() {
    return third;
  }

  @Override
  public String toString() {
    return String.format("Tuple3{first=%s, second=%s, third=%s}", first, second, third);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    return Utils.equal(first, ((Tuple3) o).first) && Utils.equal(second, ((Tuple3) o).second) && Utils
        .equal(third, ((Tuple3) o).third);
  }

  @Override
  public int hashCode() {
    int result = first != null ? first.hashCode() : 0;
    result = 31 * result + (second != null ? second.hashCode() : 0);
    result = 31 * result + (third != null ? third.hashCode() : 0);
    return result;
  }

  public static <V1, V2, V3> Tuple3<V1, V2, V3> tuple3(V1 first, V2 second, V3 third) {
    return new Tuple3<V1, V2, V3>(first, second, third);
  }
}
