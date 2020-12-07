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

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.StatsCollectionFailedException;
import com.sematext.spm.client.attributes.DoubleCounterValueHolder;
import com.sematext.spm.client.attributes.DoubleGaugeValueHolder;
import com.sematext.spm.client.attributes.GaugeValueHolder;
import com.sematext.spm.client.attributes.LongGaugeValueHolder;
import com.sematext.spm.client.attributes.MetricValueHolder;
import com.sematext.spm.client.attributes.RealCounterValueHolder;
import com.sematext.spm.client.attributes.TextValueHolder;
import com.sematext.spm.client.attributes.ValueChangeHolder;
import com.sematext.spm.client.attributes.VoidValueHolder;
import com.sematext.spm.client.config.FunctionInvokerConfig;
import com.sematext.spm.client.config.ObservationDefinitionConfig;
import com.sematext.spm.client.observation.ObservationBean;
import com.sematext.spm.client.observation.ObservationBeanDump;
import com.sematext.spm.client.observation.ObservationBeanName;
import com.sematext.spm.client.util.StringUtils;
import com.sematext.spm.client.util.StringUtils.Chain;

/**
 * Defines what to observe for particular MBean
 */
public class MBeanObservation extends ObservationBean<MBeanAttributeObservation, MBeanObservationContext> {
  public static final String UNNAMED_GROUP_PREFIX = "unnamed_group_";

  private static final Log LOG = LogFactory.getLog(MBeanObservation.class);

  private List<MBeanFunctionInvoker> functionInvokers;

  private Name name;
  private String objectName;

  private static final class Name {
    private final String originalName;
    private final ObjectName objectNamePattern;
    private final String originalObjectNamePattern;
    private final Chain chain;

    public ObjectName getObjectNamePattern() {
      return objectNamePattern;
    }

    public String name(ObjectInstance objectInstance) {
      return chain.process(objectName(objectInstance));
    }

    public Map<String, String> extractParams(ObjectInstance objectInstance) {
      return chain.extractParams(objectName(objectInstance));
    }

    private static String objectName(ObjectInstance objectInstance) {
      return objectInstance.getObjectName().toString();
    }

    public String getNamePattern() {
      return originalName;
    }

    private Name(String orginalName, ObjectName objectNamePattern, String originalObjectNamePattern, Chain chain) {
      this.originalName = orginalName;
      this.objectNamePattern = objectNamePattern;
      this.originalObjectNamePattern = originalObjectNamePattern;
      this.chain = chain;
    }

    public static Name make(String objectNamePattern, String namePattern) throws ConfigurationFailedException {
      try {
        String jmxPattern = objectNamePattern.replaceAll("\\$\\{.*?\\}", "*");

        Chain chain = StringUtils.Chain.make(objectNamePattern, namePattern, "${", "}");
        return new Name(namePattern, ObjectName.getInstance(jmxPattern), objectNamePattern, chain);

      } catch (MalformedObjectNameException e) {
        LOG.error("Bad objectName value: " + objectNamePattern, e);
        throw new ConfigurationFailedException("Bad objectName value: " + objectNamePattern, e);
      }
    }

    public String getOriginalObjectNamePattern() {
      return originalObjectNamePattern;
    }

    public List<String> getObjectNamePatternGroupNames() {
      String remaining = getOriginalObjectNamePattern();
      List<String> regexpGroupNames = new FastList<String>();

      int countUnnamed = 0;
      while (true) {
        int indexOfPlaceholder = remaining.indexOf("${");
        int indexOfWildcard = remaining.indexOf("*");

        // TODO error handling, externalize, add unit tests
        if (indexOfPlaceholder == -1 && indexOfWildcard == -1) {
          break;
        } else if (indexOfPlaceholder == -1 || (indexOfWildcard != -1 && indexOfWildcard < indexOfPlaceholder)) {
          // wildcard is next group
          remaining = remaining.substring(indexOfWildcard + 1);
          regexpGroupNames.add(UNNAMED_GROUP_PREFIX + (countUnnamed++));
        } else if (indexOfWildcard == -1 || (indexOfPlaceholder != -1 && indexOfPlaceholder < indexOfWildcard)) {
          // placeholder is next group
          remaining = remaining.substring(indexOfPlaceholder + 2);
          String paramName = remaining.substring(0, remaining.indexOf("}"));
          regexpGroupNames.add(paramName);
          remaining = remaining.substring(remaining.indexOf("}") + 1);
        }
      }

      return regexpGroupNames;
    }
  }

  // used when instantiating a "real" MBeanObservation object for some particular jmx bean (resulting object is not just a 
  // config holder anymore)
  public MBeanObservation(MBeanObservation orig, String beanName, String objectName, Map<String, String> objectNameTags)
      throws ConfigurationFailedException {
    super(orig, objectNameTags);
    functionInvokers = Collections.unmodifiableList(orig.functionInvokers);
    this.name = Name.make(objectName, beanName);
    this.objectName = objectName;
  }

  // used when instantiating a config object
  public MBeanObservation(ObservationDefinitionConfig observationDefinition) throws ConfigurationFailedException {
    super(observationDefinition);
  }

  public ObservationBean getCopy() throws ConfigurationFailedException {
    return new MBeanObservation(this.observationDefinition);
  }

  @Override
  public String getName() {
    return name.getNamePattern();
  }

  public String getOriginalObjectNamePattern() {
    return name.getOriginalObjectNamePattern();
  }

  public ObjectName getObjectNamePattern() {
    return name.getObjectNamePattern();
  }

  public List<String> getObjectNamePatternGroupNames() {
    return name.getObjectNamePatternGroupNames();
  }

  @Override
  public Set<ObservationBeanDump> collectStats(MBeanObservationContext observationContext) {
    // first find any beans matching our objectName (such objectName can contain wildcard characters)
    // ObjectName discoveredObject = objectName;
    try {
      MBeanServerConnection mbeanServer = observationContext.getConnection();
      Set<ObjectInstance> objects = mbeanServer.queryMBeans(name.getObjectNamePattern(), null);
      // first find any beans matching our objectName (such objectName can contain wildcard characters)

      Set<ObservationBeanDump> res = new HashSet<ObservationBeanDump>();
      for (ObjectInstance discoveredObject : objects) {
        String mbeanName = name.name(discoveredObject);
        Map<String, Object> contextData = new UnifiedMap<String, Object>();
        contextData.putAll(observationContext.getContextData());
        contextData.putAll(name.extractParams(discoveredObject));
        ObservationBeanDump mbeanDump = collectSingleMBean(mbeanServer, mbeanName, discoveredObject, contextData);
        if (res.contains(mbeanDump)) {
          throw new IllegalStateException("JMX bean with objectName '" + mbeanDump.getName() +
                                              "' has more than 1 match! Matches : " + objects);
        }
        res.add(mbeanDump);
      }
      return res;
    } catch (Exception e) {
      LOG.error("Failed to fetch data for, originalName: " + name.originalName + ", objectNamePattern: " + name
          .getObjectNamePattern(), e);
      throw new RuntimeException(e);
    }
  }

  private ObservationBeanDump collectSingleMBean(MBeanServerConnection mbeanServer, String name,
                                                 ObjectInstance discoveredObject, Map<String, ?> context) {
    ObservationBeanDump stats = new ObservationBeanDump(ObservationBeanName
                                                            .mkBean(name, discoveredObject.getObjectName()
                                                                .getKeyPropertyList()));
    for (MBeanAttributeObservation attributeObservation : getAttributeObservations()) {
      Object value = null;
      try {
        value = attributeObservation.getValue(this, mbeanServer, context, discoveredObject);
      } catch (StatsCollectionFailedException e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Failed to extract stats value, name: " + name + ", attribute name: " +
                        attributeObservation.getAttributeName() + " object name: " + discoveredObject.getObjectName() +
                        ", message: " + e.getMessage());
        }
      } catch (RuntimeException re) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Failed to extract stats value, name: " + name + ", attribute name: " +
                        attributeObservation.getAttributeName() + " object name: " + discoveredObject.getObjectName() +
                        ", message: " + re.getMessage());
        }
      }
      stats.setAttribute(attributeObservation.getFinalName(), value);
    }

    for (MBeanFunctionInvoker invoker : functionInvokers) {
      try {
        stats.setAttribute(invoker.getAttributeName(), invoker.getValue(this, mbeanServer, context, discoveredObject));
      } catch (StatsCollectionFailedException e) {
        LOG.warn("Failed to invoke function, name: " + name + ", function name: " +
                     invoker.getAttributeName() + " object name: " + discoveredObject.getObjectName(), e);
      }
    }

    return stats;
  }

  @Override
  public void read(ObservationDefinitionConfig observationDefinition) throws ConfigurationFailedException {
    String name = observationDefinition.getName();
    if (name == null || "".equals(name.toString().trim())) {
      throw new ConfigurationFailedException("Observation missing required attribute 'name'");
    }
    String objectName = observationDefinition.getObjectName();
    if (objectName == null || "".equals(objectName.toString().trim())) {
      throw new ConfigurationFailedException("JMX observation missing required attribute 'objectName'");
    }

    this.name = Name.make(objectName, name);

    readAttributeObservations(observationDefinition);
    readFunctionInvokers(observationDefinition);
    readTagDefinitions(observationDefinition);
    // readAttributeNameMappings(observationDefinition);
    readIgnoreElements(observationDefinition);
    readAcceptElements(observationDefinition);
  }

  public void readFunctionInvokers(ObservationDefinitionConfig observationDefinition)
      throws ConfigurationFailedException {
    functionInvokers = new FastList<MBeanFunctionInvoker>();
    for (FunctionInvokerConfig invoker : observationDefinition.getFunc()) {
      String type = invoker.getType() != null ? invoker.getType() : null;
      MBeanFunctionInvoker functionInvoker = new MBeanFunctionInvoker(createHolder(type));
      functionInvoker.initFromConfig(invoker);
      functionInvokers.add(functionInvoker);

      // metrics produced by function invokers are just temporary values which can be used to calculate other metrics, so
      // we omit them from the result
      if (getOmitAttributes() == null) {
        setOmitAttributes(new HashSet<String>());
      }
      getOmitAttributes().add(invoker.getName());
    }
  }

  @Override
  protected MBeanAttributeObservation createAttributeObservation(String type) {
    return createObservation(type);
  }

  private static MBeanAttributeObservation createObservation(String typeName) {
    if ("counter".equals(typeName)) {
      return new JmxRealCounterAttribute();
    }
    if ("double_counter".equals(typeName)) {
      return new JmxDoubleCounterAttribute();
    }
    if ("text".equals(typeName)) {
      return new JmxTextAttribute();
    }
    if ("gauge".equals(typeName)) {
      return new JmxGaugeAttribute();
    }
    if ("long_gauge".equals(typeName)) {
      return new JmxLongGaugeAttribute();
    }
    if ("double_gauge".equals(typeName)) {
      return new JmxDoubleGaugeAttribute();
    }
    if ("change".equals(typeName)) {
      return new JmxValueChangeAttribute();
    }
    return null;
  }

  private static MetricValueHolder createHolder(String typeName) {
    if ("double_counter".equals(typeName)) {
      return new DoubleCounterValueHolder();
    }
    if ("counter".equals(typeName)) {
      return new RealCounterValueHolder();
    }
    if ("text".equals(typeName)) {
      return new TextValueHolder();
    }
    if ("gauge".equals(typeName)) {
      return new GaugeValueHolder();
    }
    if ("long_gauge".equals(typeName)) {
      return new LongGaugeValueHolder();
    }
    if ("double_gauge".equals(typeName)) {
      return new DoubleGaugeValueHolder();
    }
    if ("change".equals(typeName)) {
      return new ValueChangeHolder();
    }
    if ("void".equals(typeName)) {
      return new VoidValueHolder();
    }
    return null;
  }
}
