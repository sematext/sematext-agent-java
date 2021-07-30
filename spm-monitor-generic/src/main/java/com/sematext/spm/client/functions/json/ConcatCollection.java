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
package com.sematext.spm.client.functions.json;

import com.sematext.spm.client.JsonFunction;

import java.util.Collection;
import java.util.Iterator;

/**
 * For Collection type of objects, concatenates all values into a single string with comma as a separator.
 * Otherwise returns null.
 */
public class ConcatCollection implements JsonFunction {
    @Override
    public String toString(Object object) {
      if (object != null) {
        if (object instanceof Collection) {
          Collection col = (Collection) object;

          if (col.size() > 0) {
            Iterator iter = col.iterator();
            StringBuilder sb = new StringBuilder();

            while (iter.hasNext()) {
              if (sb.length() > 0) {
                 sb.append(",");
              }
              sb.append(iter.next());
            }

          return sb.toString();
        }
      }
    }

    return null;
  }
}
