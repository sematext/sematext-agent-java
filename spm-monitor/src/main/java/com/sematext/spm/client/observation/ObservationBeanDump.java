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

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;

/**
 * Represents one observed bean with metrics (attributes) it was used to collect.
 */
public class ObservationBeanDump {
  private final ObservationBeanName name;
  private final Map<String, Object> attributes = new UnifiedMap<String, Object>();

  public ObservationBeanDump(String name) {
    this(ObservationBeanName.mkBean(name));
  }

  public ObservationBeanDump(ObservationBeanName name) {
    this.name = name;
  }

  public void setAttribute(String name, Object value) {
    attributes.put(name, value);
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public String getName() {
    return name.getName();
  }

  public ObservationBeanName getBeanName() {
    return name;
  }

  /*CHECKSTYLE:OFF*/
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ObservationBeanDump that = (ObservationBeanDump) o;

    if (name != null ? !name.equals(that.name) : that.name != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return name != null ? name.hashCode() : 0;
  }

  public ObservationBeanDump getCopyWithName(String newName) {
    ObservationBeanDump newDump = new ObservationBeanDump(newName);
    newDump.getAttributes().putAll(this.getAttributes());
    return newDump;
  }
  /*CHECKSTYLE:ON*/
}
