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

import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Collections;
import java.util.Map;

public final class ObservationBeanName {
  private final String name;
  private final Map<String, String> keyProperties;

  private ObservationBeanName(String name, Map<String, String> keyProperties) {
    this.name = name;
    this.keyProperties = keyProperties;
  }

  public String getName() {
    return name;
  }

  public Map<String, String> getKeyProperties() {
    return keyProperties;
  }

  /* CHECKSTYLE:OFF */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ObservationBeanName that = (ObservationBeanName) o;

    if (keyProperties != null ? !keyProperties.equals(that.keyProperties) : that.keyProperties != null) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (keyProperties != null ? keyProperties.hashCode() : 0);
    return result;
  }
  /* CHECKSTYLE:ON */

  @Override
  public String toString() {
    return new ToStringBuilder(this).append(name).append(keyProperties).toString();
  }

  public static ObservationBeanName mkBean(String name, Map<String, String> keyProperties) {
    return new ObservationBeanName(name, Collections.<String, String>unmodifiableMap(keyProperties));
  }

  public static ObservationBeanName mkBean(String name) {
    return new ObservationBeanName(name, Collections.<String, String>emptyMap());
  }
}
