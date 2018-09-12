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
package com.sematext.spm.client.functions;

/**
 * Example: func: ExtractDoubleFromMapString(Value,cumulative_hits) - from monitored object with
 * name 'Value' (which is in key-value format), extracts and returns value of key
 * 'cumulative_hits'. Result will be of type 'double'.
 *
 * @author sematext, http://www.sematext.com/
 */
public class ExtractDoubleFromMapString extends ExtractValueFromMapString<Double> {
  @Override
  protected Double convertToResult(String value) {
    return Double.valueOf(value);
  }
}
