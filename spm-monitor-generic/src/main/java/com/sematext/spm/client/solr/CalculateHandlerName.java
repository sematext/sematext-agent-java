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
package com.sematext.spm.client.solr;

import java.util.Map;

import com.sematext.spm.client.observation.CalculationFunction;

/**
 * Solr 10 labels shard sub-requests of a search handler with internal="true". Solr 9 reported them as a separate
 * handler named "<handler>[shard]", so the same name is used to keep both versions on the same series.
 * Params: names of the handler and internal tags.
 */
public class CalculateHandlerName implements CalculationFunction {
  @Override
  public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    throw new UnsupportedOperationException("Can't be used in attribute context");
  }

  @Override
  public String calculateTag(Map<String, String> tags, Object... params) {
    if (params == null || params.length != 2) {
      throw new IllegalArgumentException("Expected handler and internal tag names");
    }
    String handler = tags.get(params[0].toString());
    if (handler != null && "true".equals(tags.get(params[1].toString()))) {
      return handler + "[shard]";
    }
    return handler;
  }
}
