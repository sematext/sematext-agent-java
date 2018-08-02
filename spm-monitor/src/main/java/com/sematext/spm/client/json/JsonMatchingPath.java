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
package com.sematext.spm.client.json;

import java.util.Map;

/**
 * In case when path is defined as: path="$._all.indices.${index}.shards.${shard}.routing\.number", instances of this class will
 * contain all matching elements (objects at the end of the tree with name "routing.number") in jsonElement property, along
 * with values for "index" and "shard" attributes resolved to real values for specific jsonElement.
 */
public class JsonMatchingPath {
  private String fullObjectPath;
  private Map<String, String> pathAttributes;
  private Object matchedObject;

  public JsonMatchingPath(String fullObjectPath, Map<String, String> pathAttributes, Object matchedObject) {
    this.fullObjectPath = fullObjectPath;
    this.pathAttributes = pathAttributes;
    this.matchedObject = matchedObject;
  }

  public String getFullObjectPath() {
    return fullObjectPath;
  }

  public Map<String, String> getPathAttributes() {
    return pathAttributes;
  }

  public Object getMatchedObject() {
    return matchedObject;
  }
}
