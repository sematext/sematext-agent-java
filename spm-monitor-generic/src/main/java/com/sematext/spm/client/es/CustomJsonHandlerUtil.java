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
package com.sematext.spm.client.es;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public final class CustomJsonHandlerUtil {
  public CustomJsonHandlerUtil() {
  }

  public static void skipAllSiblings(JsonParser jsonParser) throws IOException {
    jsonParser.nextToken();

    while (true) {
      jsonParser.nextToken();
      jsonParser.skipChildren();
      jsonParser.nextToken();

      if (jsonParser.getCurrentToken() == JsonToken.END_OBJECT) {
        jsonParser.nextToken();
        break;
      }
    }
  }

  public static boolean navigateToSiblingFieldWithName(JsonParser jsonParser, String nodeDefAfterCurrent)
      throws IOException {
    while (true) {
      if (jsonParser.getCurrentToken() == JsonToken.FIELD_NAME) {
        String keyName = jsonParser.getCurrentName();
        jsonParser.nextToken();

        if (nodeDefAfterCurrent.equals(keyName)) {
          return true;
        } else {
          jsonParser.skipChildren();
          jsonParser.nextToken();
        }
      } else {
        return false;
      }
    }
  }

  public static String moveToNextObject(JsonParser jsonParser, boolean nullShouldBeSkipped) throws IOException {
    if (nullShouldBeSkipped) {
      int countNullTokensInARow = 0;
      while (jsonParser.getCurrentToken() == null || jsonParser.getCurrentToken() == JsonToken.START_OBJECT
          || jsonParser.getCurrentToken() == JsonToken.START_ARRAY) {

        if (jsonParser.getCurrentToken() == null) {
          countNullTokensInARow++;
        } else {
          countNullTokensInARow = 0;
        }

        if (countNullTokensInARow > 2) {
          // safety net - in case we encounter multiple null tokens in a row, it is a good sign that json is
          // malformed and we would end up in infinite loop
          break;
        }

        jsonParser.nextToken();
      }
    } else {
      while (jsonParser.getCurrentToken() == JsonToken.START_OBJECT
          || jsonParser.getCurrentToken() == JsonToken.START_ARRAY) {
        jsonParser.nextToken();
      }
    }
    return jsonParser.getCurrentName();
  }

  public static Map<String, Object> convertToMap(JsonNode routing) {
    Map<String, Object> res = new UnifiedMap<String, Object>();
    Iterator<Entry<String, JsonNode>> iter = routing.fields();
    while (iter.hasNext()) {
      Entry<String, JsonNode> field = iter.next();
      String fieldValue = field.getValue().asText();
      /* not needed, fields which can be null are not used from this particular output
      if (fieldValue.equals("null")) {
        res.put(field.getKey(), null);
      } else {
        res.put(field.getKey(), fieldValue);
      }*/
      res.put(field.getKey(), fieldValue);
    }
    return res;
  }

  public static Map<String, Object> convertToMap(TreeNode node) {
    // TODO - doesn't support arrays
    Map<String, Object> res = new UnifiedMap<String, Object>();
    Iterator<String> iter = node.fieldNames();
    while (iter.hasNext()) {
      String fieldName = iter.next();

      TreeNode treeNode = node.get(fieldName);

      if (treeNode.isValueNode()) {
        res.put(fieldName, treeNode.toString());
      } else {
        res.put(fieldName, convertToMap(treeNode));
      }
    }
    return res;
  }
}
