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

public class DoubleCounterValueHolder extends MetricValueHolder<Double> {
  @Override
  public Double getValue(Object measuredValue) {
    String stringValue = String.valueOf(measuredValue);
    double currentValue = Double.valueOf(stringValue);

    if (getPreviousValue() == null) {
      setPreviousValue(currentValue);
    }

    //reset performed
    if (getPreviousValue() > currentValue) {
      setPreviousValue(currentValue);
      // we can't know why the reset happened (could be a restart, but also could be some issue with delayed
      // stats reporting by the service we monitor, so value we get here could be huge cumulative). The safest approach
      // is to just report as 0
      return 0d;
    }

    double delta = currentValue - getPreviousValue();
    setPreviousValue(currentValue);

    return delta;
  }
}
