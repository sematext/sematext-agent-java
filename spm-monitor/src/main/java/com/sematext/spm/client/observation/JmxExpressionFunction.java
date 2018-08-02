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

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.jmx.JmxHelper;
import com.sematext.spm.client.jmx.JmxServiceContext;

public class JmxExpressionFunction implements CalculationFunction {
  private static final Log LOG = LogFactory.getLog(JmxExpressionFunction.class);

  private String objectName;
  private String attributeName;
  private String fullPlaceholderResolvedExpression;

  private JmxServiceContext ctx;

  public JmxExpressionFunction(JmxServiceContext ctx) {
    this.ctx = ctx;
  }

  public static JmxExpressionFunction getFunction(String fullPlaceholderResolvedExpression) {
    String objectNameWithAttrib;
    if (fullPlaceholderResolvedExpression.startsWith("jmx:")) {
      objectNameWithAttrib = fullPlaceholderResolvedExpression.substring("jmx:".length()).trim();
    } else {
      objectNameWithAttrib = fullPlaceholderResolvedExpression;
    }

    if (objectNameWithAttrib.indexOf(" ") != -1) {
      // any placeholders are already resolved by here if that was needed 
      String objectName = objectNameWithAttrib.substring(0, objectNameWithAttrib.lastIndexOf(" ")).trim();
      String attributeName = objectNameWithAttrib.substring(objectNameWithAttrib.lastIndexOf(" ")).trim();
      return new JmxExpressionFunction(objectName, attributeName, fullPlaceholderResolvedExpression);
    } else {
      LOG.warn("Incorrect format of jmx expression, attribute name should be separated from object name with a space: "
                   + fullPlaceholderResolvedExpression);
      return null;
    }
  }

  public JmxExpressionFunction(String objectName, String attributeName, String fullPlaceholderResolvedExpression) {
    this.objectName = objectName;
    this.attributeName = attributeName;
    this.fullPlaceholderResolvedExpression = fullPlaceholderResolvedExpression;
  }

  private Object calculate() {
    Object jmxObject = JmxHelper.queryJmx(ctx, objectName, attributeName);
    return jmxObject;
  }

  @Override
  public Object calculateAttribute(Map<String, Object> metrics, Object... params) {
    return calculate();
  }

  @Override
  public String calculateTag(Map<String, String> objectNameTags, Object... params) {
    // cache used only for tags, they will typically be reused very often, while attributes would be reused almost never
    if (ExpressionCache.containsResult(fullPlaceholderResolvedExpression)) {
      Object res = ExpressionCache.getResult(fullPlaceholderResolvedExpression);
      if (res == null) {
        return null;
      } else if (res instanceof String) {
        return (String) res;
      } else {
        return String.valueOf(res);
      }
    } else {
      Object res = calculate();
      String resultStr = res != null ? String.valueOf(res) : null;
      ExpressionCache.addResult(fullPlaceholderResolvedExpression, resultStr);
      return resultStr;
    }
  }
}
