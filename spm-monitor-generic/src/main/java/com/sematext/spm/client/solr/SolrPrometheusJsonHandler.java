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

import com.fasterxml.jackson.core.JsonFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.json.PrometheusTextJsonHandler;

/**
 * Solr 10 labels search handler requests with internal="true" (shard sub-requests) or "false", but QUERY handlers
 * that are not search handlers (/stream, /graph, /mlt) get no internal label at all. A path filter can't match a
 * missing label, so those requests are labeled as client requests, which is what they are.
 */
public class SolrPrometheusJsonHandler extends PrometheusTextJsonHandler {
  @Override
  public Map<String, Object> parse(InputStream inputStream, JsonFactory jsonFactory) throws IOException {
    Map<String, Object> samples = super.parse(inputStream, jsonFactory);
    for (Map.Entry<String, Object> entry : samples.entrySet()) {
      if (entry.getKey().startsWith("solr_core_requests")) {
        markClientRequests((List<Map<String, Object>>) entry.getValue());
      }
    }
    return samples;
  }

  private static void markClientRequests(List<Map<String, Object>> samples) {
    for (Map<String, Object> sample : samples) {
      if ("QUERY".equals(sample.get("category")) && !sample.containsKey("internal")) {
        sample.put("internal", "false");
      }
    }
  }
}
