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
package com.sematext.spm.client.redis;

final class ValueInjections {
  private ValueInjections() {
  }

  static interface ValueInjection<T> {
    T inject(String value);
  }

  static final ValueInjection<Long> LONG_INJECTION = new ValueInjection<Long>() {
    @Override
    public Long inject(String value) {
      try {
        Long parsed = Long.parseLong(value);
        if (parsed < 0) {
          return 0L;
        }
        return parsed;
      } catch (Exception e) {
        return 0L;
      }
    }
  };

  static final ValueInjection<Double> DOUBLE_INJECTION = new ValueInjection<Double>() {
    @Override
    public Double inject(String value) {
      try {
        Double parsed = Double.parseDouble(value);
        if (parsed < 0) {
          return 0D;
        }
        return parsed;
      } catch (Exception e) {
        return 0d;
      }
    }
  };
}
