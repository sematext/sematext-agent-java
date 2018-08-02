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
package com.sematext.spm.client.unlogger;

public class JoinPoint {

  private final String type;
  private final String shortName;
  private final String serializedParams;

  public JoinPoint(String type, String shortName, String serializedParams) {
    this.type = type;
    this.shortName = shortName;
    this.serializedParams = serializedParams;
  }

  public String getShortName() {
    return shortName;
  }

  public String getType() {
    return type;
  }

  public String getSerializedParams() {
    return serializedParams;
  }

  public Class[] getParameterTypes(ClassLoader loader) throws ClassNotFoundException {
    if (serializedParams.isEmpty()) {
      return new Class[0];
    }
    final String[] classNames = serializedParams.split(",");
    final Class[] types = new Class[classNames.length];
    for (int i = 0; i < classNames.length; i++) {
      String className = classNames[i];
      if (className.equals("int")) {
        types[i] = int.class;
      } else if (className.equals("long")) {
        types[i] = long.class;
      } else if (className.equals("float")) {
        types[i] = float.class;
      } else if (className.equals("double")) {
        types[i] = double.class;
      } else if (className.equals("short")) {
        types[i] = short.class;
      } else if (className.equals("byte")) {
        types[i] = byte.class;
      } else if (className.equals("boolean")) {
        types[i] = boolean.class;
      } else if (className.equals("char")) {
        types[i] = char.class;
      } else {
        types[i] = Class.forName(className, false, loader);
      }
    }
    return types;
  }

  /*CHECKSTYLE:OFF*/
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    JoinPoint joinPoint = (JoinPoint) o;

    if (serializedParams != null ?
        !serializedParams.equals(joinPoint.serializedParams) :
        joinPoint.serializedParams != null)
      return false;
    if (shortName != null ? !shortName.equals(joinPoint.shortName) : joinPoint.shortName != null) return false;
    if (type != null ? !type.equals(joinPoint.type) : joinPoint.type != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + (shortName != null ? shortName.hashCode() : 0);
    result = 31 * result + (serializedParams != null ? serializedParams.hashCode() : 0);
    return result;
  }
  /*CHECKSTYLE:ON*/

  @Override
  public String toString() {
    return type + "#" + shortName + "()";
  }
}
