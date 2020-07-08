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
package com.sematext.spm.client.config;

import java.util.Collections;
import java.util.List;

public class ObservationDefinitionConfig {
  private String name;
  private String objectName;
  private String path;
  private String metricNamespace;
  private String rowIdColumns;
  private String singleRowResult;

  private List<MetricConfig> metric = Collections.EMPTY_LIST;
  private List<TagConfig> tag = Collections.EMPTY_LIST;
  private List<IgnoreConfig> ignore = Collections.EMPTY_LIST;
  private List<AcceptConfig> accept = Collections.EMPTY_LIST;
  private List<FunctionInvokerConfig> func = Collections.EMPTY_LIST;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getObjectName() {
    return objectName;
  }

  public void setObjectName(String objectName) {
    this.objectName = objectName;
  }

  public String getMetricNamespace() {
    return metricNamespace;
  }

  public void setMetricNamespace(String metricNamespace) {
    this.metricNamespace = metricNamespace;
  }

  public List<MetricConfig> getMetric() {
    return metric;
  }

  public void setMetric(List<MetricConfig> metric) {
    this.metric = metric;
  }

  public List<TagConfig> getTag() {
    return tag;
  }

  public void setTag(List<TagConfig> tag) {
    this.tag = tag;
  }

  public List<IgnoreConfig> getIgnore() {
    return ignore;
  }

  public void setIgnore(List<IgnoreConfig> ignore) {
    this.ignore = ignore;
  }

  public List<AcceptConfig> getAccept() {
    return accept;
  }

  public void setAccept(List<AcceptConfig> accept) {
    this.accept = accept;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public List<FunctionInvokerConfig> getFunc() {
    return func;
  }

  public void setFunc(List<FunctionInvokerConfig> func) {
    this.func = func;
  }

  public String getRowIdColumns() {
    return rowIdColumns;
  }

  public void setRowIdColumns(String rowIdColumns) {
    this.rowIdColumns = rowIdColumns;
  }

  public String getSingleRowResult() {
    return singleRowResult;
  }

  public void setSingleRowResult(String singleRowResult) {
    this.singleRowResult = singleRowResult;
  }
}
