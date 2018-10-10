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

import java.util.ArrayList;
import java.util.List;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MonitorConfig;
import com.sematext.spm.client.StatsExtractorConfig;

public abstract class BeanConditionCheck {
  private static final Log LOG = LogFactory.getLog(BeanConditionCheck.class);

  private static final String CLAUSE_SEPARATOR_REGEXP = "\\|\\|";
  private static final String ESCAPED_SEPARATOR = "\\||";  // what user would use in his configs when trying to use || as a string
  private static final String TMP_REPLACED_ESCAPED_SEPARATOR = "TMP_REPLACED_ESCAPED_SEPARATOR";

  private MonitorConfig monitorConfig;
  private StatsExtractorConfig extractorConfig;
  private String requiredValuesString;
  private List<String> requiredValues = new ArrayList<String>();
  private int countNeededMatches = 1; // default: 1 match is enough

  public void setRequiredValuesString(String requiredValuesString) {
    this.requiredValuesString = requiredValuesString;
    requiredValuesString = requiredValuesString.replace(ESCAPED_SEPARATOR, TMP_REPLACED_ESCAPED_SEPARATOR);
    String[] values = requiredValuesString.split(CLAUSE_SEPARATOR_REGEXP);
    for (String value : values) {
      if (!value.trim().equals("")) {
        requiredValues.add(value.replace(TMP_REPLACED_ESCAPED_SEPARATOR, "||"));
      }
    }
  }

  public boolean satisfies() {
    if (countNeededMatches > requiredValues.size()) {
      LOG.error("For " + this.getClass() + " countNeededMatches (" + countNeededMatches
                    + ") is bigger than number of clauses (" +
                    requiredValues.size() + "): " + requiredValuesString);
      return false;
    }

    String conditionValue = getConditionValue();

    if (LOG.isDebugEnabled()) {
      LOG.debug("Checking " + getClass() + " for required=" + requiredValuesString + ", countNeededMatches="
                    + countNeededMatches);
    }

    int countMatches = 0;
    for (String val : requiredValues) {
      boolean clauseResult = clauseSatisfies(conditionValue, val);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Condition value=" + conditionValue + ", requiredValue=" + val + ", result was: " + clauseResult);
      }
      if (clauseResult) {
        countMatches++;
        if (countMatches >= countNeededMatches) {
          return true;
        }
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug(this.getClass() + " doesn't satisfy conditions since just " + countMatches + " clauses out of "
                    + requiredValues.size() +
                    " match, minimum required was " + countNeededMatches);
    }
    return false;
  }

  protected abstract String getConditionValue();

  protected abstract boolean clauseSatisfies(String conditionValue, String expectedValue);

  public void setCountNeededMatches(int countNeededMatches) {
    this.countNeededMatches = countNeededMatches;
  }

  public StatsExtractorConfig getExtractorConfig() {
    return extractorConfig;
  }

  public void setExtractorConfig(StatsExtractorConfig extractorConfig) {
    this.extractorConfig = extractorConfig;
  }

  public MonitorConfig getMonitorConfig() {
    return monitorConfig;
  }

  public void setMonitorConfig(MonitorConfig monitorConfig) {
    this.monitorConfig = monitorConfig;
  }
}
