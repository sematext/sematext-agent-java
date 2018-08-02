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

import java.util.List;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.StatsCollectionFailedException;
import com.sematext.spm.client.observation.AttributeObservation;
import com.sematext.spm.client.observation.ObservationBean;

public abstract class JsonAttributeObservation extends AttributeObservation<Object> {
  private static final Log LOG = LogFactory.getLog(JsonAttributeObservation.class);

  @Override
  public Object getValue(ObservationBean<?, ?> parentObservation, Object data, Map<String, ?> context,
                         Object... additionalParams) throws StatsCollectionFailedException {
    final String jsonDataNodePath = (String) additionalParams[0];
    Object measurement = readAttribute(data, jsonDataNodePath);

    if (measurement == null) {
      return null;
    }

    return getMetricValue(parentObservation, measurement);
  }

  protected Object readAttribute(Object jsonContent, String jsonDataNodePath) throws StatsCollectionFailedException {
    if (jsonContent == null) {
      throw new StatsCollectionFailedException("JSON data empty for path " + jsonDataNodePath);
    }

    List<JsonMatchingPath> matchingPaths = JsonUtil.findMatchingPaths(jsonContent, jsonDataNodePath);
    if (matchingPaths.size() > 1) {
      LOG.warn("Multiple matches found for attribute " + getAttributeName() + " for path " +
                   jsonDataNodePath + ". Discarding all matches...");
      return null;
    }

    if (matchingPaths.size() == 1) {
      Object matchedObject = matchingPaths.get(0).getMatchedObject();
      if (matchedObject instanceof Map) {
        String attributeName = getAttributeName();
        Object attributeValueByName = ((Map<String, Object>) matchedObject).get(attributeName);
        if (attributeName.contains(".")) {
          // possibly json expression here so let's check...
          Object attributeValueByExpression = JsonUtil.findValueIn(attributeName, matchedObject);

          if (attributeValueByName != null && attributeValueByExpression != null) {
            throw new StatsCollectionFailedException(
                "For attribute/expression: " + attributeName + " found ambiguous result. " +
                    "Both field with such name and such expression produce a value (field=" + attributeValueByName
                    + ", expression=" +
                    attributeValueByExpression);
          } else {
            return attributeValueByName != null ? attributeValueByName : attributeValueByExpression;
          }
        } else {
          return attributeValueByName;
        }
      } else {
        throw new StatsCollectionFailedException(
            "Expected to match an object (Map) with path " + jsonDataNodePath + ", instead " +
                "found " + matchedObject);
      }
    } else {
      // path doesn't match anymore, likely something changed in the data since collectors were recreated last time
      return null;
    }
  }
}
