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
package com.sematext.spm.client.storm;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

public class ExecutorOutputStatsKey {
  private String executorId;
  private String componentId;
  private String stream;

  public ExecutorOutputStatsKey(String executorId, String componentId, String stream) {
    this.executorId = executorId;
    this.componentId = componentId;
    this.stream = stream;
  }

  public String getExecutorId() {
    return executorId;
  }

  public void setExecutorId(String executorId) {
    this.executorId = executorId;
  }

  public String getComponentId() {
    return componentId;
  }

  public void setComponentId(String componentId) {
    this.componentId = componentId;
  }

  public String getStream() {
    return stream;
  }

  public void setStream(String stream) {
    this.stream = stream;
  }

  /*CHECKSTYLE:OFF*/
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExecutorOutputStatsKey that = (ExecutorOutputStatsKey) o;

    if (componentId != null ? !componentId.equals(that.componentId) : that.componentId != null) return false;
    if (executorId != null ? !executorId.equals(that.executorId) : that.executorId != null) return false;
    if (stream != null ? !stream.equals(that.stream) : that.stream != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = executorId != null ? executorId.hashCode() : 0;
    result = 31 * result + (componentId != null ? componentId.hashCode() : 0);
    result = 31 * result + (stream != null ? stream.hashCode() : 0);
    return result;
  }

  public String toKey() {
    return StringUtils.join(Arrays.asList(executorId, componentId, stream), ":");
  }

  /*CHECKSTYLE:ON*/
}
