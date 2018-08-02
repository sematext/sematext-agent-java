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

import com.sematext.spm.client.attributes.RealCounterValueHolder;
import com.sematext.spm.client.observation.CalculationFunction;

public class CalculatePreSolr7TotalTime implements CalculationFunction {
  private static final Long ZERO = new Long(0L);

  // has a private var here, therefore related derivedAttrib has to be declared as stateful in collector config
  private RealCounterValueHolder totalTimeHolder;

  @Override
  public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    throw new UnsupportedOperationException("Can't be used in tag context");
  }

  @Override
  public Number calculateAttribute(Map<String, Object> metrics, Object... params) {
    Long requests = (Long) metrics.get("requests");
    Long totalTime = (Long) metrics.get("totalTime");
    Double avgTimePerRequest = (Double) metrics.get("avgTimePerRequest");

    if (totalTime != null) {
      return totalTime;
    } else {
      if (requests != null && requests > 0) {
        // adjustment for Solr 6.4 "bug"
        if (avgTimePerRequest != null) {
          if (totalTimeHolder == null) {
            totalTimeHolder = new RealCounterValueHolder();
          }

          // this is obviously wrong, but for some reason is producing almost exact values as expected
          // the wrong part is related to us multiplying "delta requests" with what is supposed to be
          // cummulative avg time per request (for all requests since the beginning)... Obviously internally
          // there is reset of this avg value happening very often which makes this calculation almost correct
          // NOTE: according to https://issues.apache.org/jira/browse/SOLR-10226 there is probably some
          // decay/sampling involed from 6.4, though it might just be a bug in Solr. In any case, this
          // obviously wrong calculaton is probably the only thing we could do to get somewhat approximate
          // "totalTime" value (multiply "delta requests" with most recent "adjusted" avgTimePerRequest value)
          long newTotalTime = (long) (avgTimePerRequest.doubleValue() * requests.longValue());
          return totalTimeHolder.getValue(newTotalTime);
        }
      } else if (requests != null) {
        return ZERO;
      }
    }

    return null;
  }
}
