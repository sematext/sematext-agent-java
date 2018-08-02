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

package com.sematext.spm.client.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PercentileUtils {
  private static final int MAX_PCTLS_PER_DEFINITION = 5;
  private static final Map<String, List<Long>> DEFINITIONS_TO_PCTLS_LISTS = new HashMap<String, List<Long>>(10);

  private static String formPctlName(Long pctl, String metricName) {
    return metricName + ".pctl." + pctl;
  }

  public static Map<Long, String> getPctlToNameMap(String definition, String baseMetricName) {
    definition = definition.trim();
    List<Long> pctls = DEFINITIONS_TO_PCTLS_LISTS.get(definition);

    if (pctls == null) {
      pctls = new ArrayList<Long>(MAX_PCTLS_PER_DEFINITION);
      for (String s : definition.split(",")) {
        s = s.trim();
        if (s.equals("")) {
          continue;
        }

        if (pctls.size() == MAX_PCTLS_PER_DEFINITION) {
          throw new IllegalArgumentException(
              "Pctls definition '" + definition + "' contains more than " + MAX_PCTLS_PER_DEFINITION +
                  " pctls");
        }

        Long pctl = Long.valueOf(s);
        if (pctl <= 0 || pctl >= 100) {
          throw new IllegalArgumentException("Unsupported pctl definition: " + pctl);
        }

        pctls.add(pctl);
      }
    }
    Map<Long, String> pctlsToNames = new HashMap<Long, String>(pctls.size());
    for (Long pctl : pctls) {
      pctlsToNames.put(pctl, formPctlName(pctl, baseMetricName));
    }
    return pctlsToNames;
  }
}
