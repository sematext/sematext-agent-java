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
package com.sematext.spm.client.observation;

import java.util.Map;

/**
 * General interface for calculation functions. Each implementation can provide logic for just one of methods and make the other
 * one unsupported (e.g., if logic calculates some tag based on two other tags, then it obviously has no relation to metrics and
 * trying to use it as such in configuration should produce an error).
 * <p>
 * There will be implementations that can provide either a tag or a metric (can work in both contexts), for example if using
 * jmx bean path or json path expression which don't depend on content of other metrics and tags.
 * <p>
 * Optionally we can pass params to the functions from config definitions. All params are of type String and needs
 * to be interpreted and converted accordingly in the function implementation. When no params are passed from config xml,
 * `params` will be null.
 * <p>
 * To pass params use :
 * source: func:com.sematext.spm.client.tomcat.MultiplyFunction(cache.size.kb,1024)
 * <p>
 * In case of no param use :
 * source: func:com.sematext.spm.client.solr.ExtractCacheMaxSize
 */
public interface CalculationFunction {
  Object calculateAttribute(Map<String, Object> metrics, Object... params);

  String calculateTag(Map<String, String> objectNameTags, Object... params);
}
