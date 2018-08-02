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
package com.sematext.spm.client.attributes;

/**
 * Provides "classic" counter logic which returns real value every time getValue is called
 */
public class RealCounterValueHolder extends MetricValueHolder<Long> {
  @Override
  public Long getValue(Object measuredValue) {
    String stringValue = String.valueOf(measuredValue);
    // Long currentValue = Long.valueOf(stringValue);
    Long currentValue = Double.valueOf(stringValue).longValue();

    if (getPreviousValue() == null) {
      setPreviousValue(currentValue);
    }

    //reset performed
    if (getPreviousValue() > currentValue) {
      setPreviousValue(currentValue);
      return currentValue;
    }

    long delta = currentValue - getPreviousValue();
    setPreviousValue(currentValue);

    return delta;
  }
}
