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
package com.sematext.spm.client.jmx;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.IOException;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ReflectionException;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.StatsCollectionFailedException;
import com.sematext.spm.client.attributes.MetricType;
import com.sematext.spm.client.attributes.MetricValueHolder;
import com.sematext.spm.client.config.FunctionInvokerConfig;
import com.sematext.spm.client.config.FunctionInvokerParamConfig;
import com.sematext.spm.client.observation.AttributeObservation;
import com.sematext.spm.client.observation.ObservationBean;

public class MBeanFunctionInvoker extends AttributeObservation<MBeanServerConnection> {
  private static final Log LOG = LogFactory.getLog(MBeanFunctionInvoker.class);
  private Object[] params;
  private String[] signature;
  private MetricValueHolder valueHolder;

  public MBeanFunctionInvoker(MetricValueHolder valueHolder) {
    this.valueHolder = valueHolder;
  }

  private Object invoke(MBeanServerConnection data, ObjectInstance discoveredObject)
      throws InstanceNotFoundException, IOException, ReflectionException, MBeanException {
    return data.invoke(discoveredObject.getObjectName(), getAttributeName(), params, signature);
  }

  public void initFromConfig(FunctionInvokerConfig invoker) throws ConfigurationFailedException {
    super.initFromConfig(invoker);

    if (invoker.getParam().size() == 0) {
      return;
    }

    Object[] params = new Object[invoker.getParam().size()];
    String[] signature = new String[invoker.getParam().size()];

    int counter = 0;
    for (FunctionInvokerParamConfig param : invoker.getParam()) {
      String paramTypeString = param.getType();
      String paramValueString = param.getValue();
      Object value = convert(paramTypeString, paramValueString);
      params[counter] = value;
      signature[counter] = paramTypeString;

      if (value == null || "".equals(value.toString().trim())) {
        throw new ConfigurationFailedException("Missing required 'value' attribute for 'func' definition");
      }
      if (paramTypeString == null || "".equals(paramTypeString.toString().trim())) {
        throw new ConfigurationFailedException("Missing required 'type' attribute for 'func' definition");
      }

      counter++;
    }

    this.params = params;
    this.signature = signature;
  }

  private Object convert(String targetType, String value) {
    if ("int".equals(targetType)) {
      return Integer.parseInt(value);
    }
    if ("double".equals(targetType)) {
      return Double.parseDouble(value);
    }
    if ("long".equals(targetType)) {
      return Long.parseLong(value);
    }
    if ("short".equals(targetType)) {
      return Short.parseShort(value);
    }
    if ("float".equals(targetType)) {
      return Float.parseFloat(value);
    }
    if ("boolean".equals(targetType)) {
      return Boolean.parseBoolean(value);
    }

    Class<?> targetTypeClass = null;
    try {
      targetTypeClass = Class.forName(targetType);
    } catch (ClassNotFoundException e) {
      LOG.error("Can't load class by name: " + targetType, e);
    }

    try {
      PropertyEditor editor = PropertyEditorManager.findEditor(targetTypeClass);
      editor.setAsText(value);
      return editor.getValue();
    } catch (Exception e) {
      LOG.error("Can't convert value to appropriate type, type: " + targetType + ", value: " + value, e);
      return null;
    }
  }

  @Override
  public Object getValue(ObservationBean<?, ?> parentObservation, MBeanServerConnection data, Map<String, ?> context,
                         Object... additionalParams) throws StatsCollectionFailedException {
    ObjectInstance discoveredObject = (ObjectInstance) additionalParams[0];
    try {
      Object invoke = invoke(data, discoveredObject);
      if (invoke != null) {
        return getMetricValue(parentObservation, invoke);
      } else {
        return null;
      }
    } catch (InstanceNotFoundException e) {
      LOG.warn("Failed to invoke function, function name: " +
                   getAttributeName() + " object name: " + discoveredObject.getObjectName(), e);
    } catch (IOException e) {
      LOG.warn("Failed to invoke function, function name: " +
                   getAttributeName() + " object name: " + discoveredObject.getObjectName(), e);
    } catch (ReflectionException e) {
      LOG.warn("Failed to invoke function, function name: " +
                   getAttributeName() + " object name: " + discoveredObject.getObjectName(), e);
    } catch (MBeanException e) {
      LOG.warn("Failed to invoke function, function name: " +
                   getAttributeName() + " object name: " + discoveredObject.getObjectName(), e);
    }

    return null;
  }

  @Override
  protected MetricValueHolder<?> createHolder() {
    // already created when constructor was called
    return valueHolder;
  }

  @Override
  public MetricType getMetricType() {
    return valueHolder.getMetricType();
  }
}
