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

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.ObservationConfigTagResolver;
import com.sematext.spm.client.aggregation.AgentAggregationFunction;
import com.sematext.spm.client.attributes.MetricType;
import com.sematext.spm.client.config.AcceptConfig;
import com.sematext.spm.client.config.IgnoreConfig;
import com.sematext.spm.client.config.MetricConfig;
import com.sematext.spm.client.config.ObservationDefinitionConfig;
import com.sematext.spm.client.config.TagConfig;

public abstract class ObservationBean<T extends AttributeObservation<?>, DATA_PROVIDER> {
  private static final Log LOG = LogFactory.getLog(ObservationBean.class);

  public static final String ATTRIBUTE_NAME_MAPPING_MARKER = "#attributeNameMapping";

  public static final String SOURCE_TYPE_FUNCTION = "func";
  public static final String SOURCE_TYPE_EVAL = "eval";
  public static final String SOURCE_TYPE_JMX = "jmx";
  public static final String SOURCE_TYPE_JSON = "json";
  public static final String SOURCE_TYPE_OUTER = "outer";

  private String metricNamespace;
  private List<T> attributeObservations;
  private Map<String, String> tags;
  private Map<String, String> attributeNameMappings;
  private Map<String, Set<String>> ignoreConditions;
  private Map<String, Set<String>> acceptConditions;
  private Set<String> omitAttributes;
  private List<DerivedAttribute> derivedAttributes;
  private Map<String, AgentAggregationFunction> attributesToAgentAggregationFunctions;
  private Map<String, MetricType> metricTypes;
  private List<PercentilesDefinition> percentilesDefinitions;

  protected ObservationDefinitionConfig observationDefinition;

  public ObservationBean(ObservationBean<T, DATA_PROVIDER> orig, Map<String, String> objectNameTags) {
    // Each AttributeObservation contains general properties of some attribute (based on which this attribute can
    // be measured), but it doesn't contain specific valueHolders (they are held separately in a map). Therefore,
    // attributeObservations list can be reused between ObservationBeans without copying
    // NOTE: change in logic, we introduced variable attribute names which get resolved into real attributes on the
    // fly, meaning, some observations will have to modify the list of attribute observations
    attributeObservations = new ArrayList<T>(orig.getAttributeObservations());
    metricNamespace = orig.getMetricNamespace();

    tags = orig.getTags() != null ? Collections.unmodifiableMap(orig.getTags()) : null;
    attributeNameMappings = orig.getAttributeNameMappings();
    ignoreConditions = orig.getIgnoreConditions();
    acceptConditions = orig.getAcceptConditions();
    omitAttributes = orig.getOmitAttributes();
    attributesToAgentAggregationFunctions = orig.getAttributesToAgentAggregationFunctions();
    metricTypes = orig.getMetricTypes();
    percentilesDefinitions = orig.getPercentilesDefinitions();

    // derived attributes can be stateful, in which case each instance of ObservationBean should get its own instance of
    // derived attribute
    if (orig.getDerivedAttributes() != null && orig.getDerivedAttributes().size() > 0) {
      boolean hasStateful = false;
      for (DerivedAttribute da : orig.getDerivedAttributes()) {
        if (da.isStateful() || da.usesPlaceholders()) {
          hasStateful = true;
          break;
        }
      }
      if (hasStateful) {
        derivedAttributes = new ArrayList<DerivedAttribute>();
        for (DerivedAttribute da : orig.getDerivedAttributes()) {
          if (da.isStateful() || da.usesPlaceholders()) {
            // create a copy
            derivedAttributes.add(new DerivedAttribute(da, objectNameTags));
          } else {
            // add as it is
            derivedAttributes.add(da);
          }
        }

      } else {
        derivedAttributes =
            orig.getDerivedAttributes() != null ? Collections.unmodifiableList(orig.getDerivedAttributes()) : null;
      }
    }
  }

  public abstract ObservationBean getCopy() throws ConfigurationFailedException;

  public ObservationBean(ObservationDefinitionConfig observationDefinition) throws ConfigurationFailedException {
    this.observationDefinition = observationDefinition;
    metricNamespace = observationDefinition.getMetricNamespace();
    if (metricNamespace == null || metricNamespace.trim().equals("")) {
      throw new ConfigurationFailedException("Attribute 'metricNamespace' is mandatory for observation definitions");
    }

    read(observationDefinition);

    // pack into unmodifiable maps
    ignoreConditions = ignoreConditions != null ? Collections.unmodifiableMap(ignoreConditions) : null;
    acceptConditions = acceptConditions != null ? Collections.unmodifiableMap(acceptConditions) : null;
    omitAttributes = omitAttributes != null ? Collections.unmodifiableSet(omitAttributes) : null;
    percentilesDefinitions =
        percentilesDefinitions != null ? Collections.unmodifiableList(percentilesDefinitions) : null;

    fillAgentAggregationFunctions();
    fillMetricTypes();
  }

  private void fillAgentAggregationFunctions() {
    if (attributeObservations != null) {
      attributesToAgentAggregationFunctions = new UnifiedMap<String, AgentAggregationFunction>();
      for (T attribute : attributeObservations) {
        attributesToAgentAggregationFunctions.put(attribute.getFinalName(), attribute.getAgentAggregationFunction());
      }
    }
    if (derivedAttributes != null) {
      if (attributesToAgentAggregationFunctions == null) {
        attributesToAgentAggregationFunctions = new UnifiedMap<String, AgentAggregationFunction>();
      }
      for (DerivedAttribute attribute : derivedAttributes) {
        attributesToAgentAggregationFunctions.put(attribute.getName(), attribute.getAgentAggregationFunction());
      }
    }
    if (attributesToAgentAggregationFunctions != null) {
      attributesToAgentAggregationFunctions = Collections.unmodifiableMap(attributesToAgentAggregationFunctions);
    }
  }

  private void fillMetricTypes() {
    if (attributeObservations != null) {
      metricTypes = new UnifiedMap<String, MetricType>();
      for (T attribute : attributeObservations) {
        metricTypes.put(attribute.getFinalName(), attribute.getMetricType());
      }
    }
    if (derivedAttributes != null) {
      if (metricTypes == null) {
        metricTypes = new UnifiedMap<String, MetricType>();
      }
      for (DerivedAttribute attribute : derivedAttributes) {
        metricTypes.put(attribute.getName(), attribute.getMetricType());
      }
    }

    if (metricTypes != null) {
      metricTypes = Collections.unmodifiableMap(metricTypes);
    }
  }

  public boolean shouldBeIgnored(Map<String, String> objectNameTags) {
    // first check if there is attributeNameMapping which would be left unresolved
    if (objectNameTags.containsKey(ATTRIBUTE_NAME_MAPPING_MARKER)) {
      if (!attributeNameMappings.containsKey(objectNameTags.get(ATTRIBUTE_NAME_MAPPING_MARKER))) {
        return true;
      }
    }

    Boolean matchesIgnore = null;
    Boolean matchesAccept = null;
    String matchingIgnoreKeyValue = null;
    String matchingAcceptKeyValue = null;

    if (ignoreConditions != null && ignoreConditions.size() > 0) {
      for (String name : ignoreConditions.keySet()) {
        String objectNameTag = objectNameTags.get(name);
        if (objectNameTag == null) {
          continue;
        }

        Set<String> ignoreValues = ignoreConditions.get(name);
        for (String ignoreVal : ignoreValues) {
          String calculatedIgnoreVal = ObservationConfigTagResolver
              .resolveSingleClause(objectNameTags, ignoreVal, this);

          if (tagsMatch(objectNameTag, calculatedIgnoreVal)) {
            matchesIgnore = Boolean.TRUE;
            matchingIgnoreKeyValue = name + "=" + calculatedIgnoreVal;
            break;
          }
        }
      }
    }
    if (acceptConditions != null && acceptConditions.size() > 0) {
      // has to match all accept conditions
      int countMatching = 0;
      for (String name : acceptConditions.keySet()) {
        String objectNameTag = objectNameTags.get(name);

        Set<String> acceptValues = acceptConditions.get(name);
        for (String acceptVal : acceptValues) {
          String calculatedAcceptVal = ObservationConfigTagResolver
              .resolveSingleClause(objectNameTags, acceptVal, this);

          if (tagsMatch(objectNameTag, calculatedAcceptVal)) {
            matchingAcceptKeyValue = (matchingAcceptKeyValue != null ? matchingAcceptKeyValue + ", " : "") +
                name + "=" + calculatedAcceptVal;
            countMatching++;
            break;
          }
        }
      }

      if (countMatching == acceptConditions.size()) {
        matchesAccept = Boolean.TRUE;
      } else {
        matchesAccept = Boolean.FALSE;
      }
    }

    if (Boolean.TRUE.equals(matchesIgnore) && Boolean.TRUE.equals(matchesAccept)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Bean " + getName() + " matches both ignore (" + matchingIgnoreKeyValue + ") and accept (" +
                      matchingAcceptKeyValue + ") values. It will be ignored.");
      }
      return true;
    }

    // now "calculate" whether to ignore it or not - for now, if any of "ignore" definitions match, ignore it; 
    // should be ignored when there is ignore condition that matches or when there are accept conditions from which none matches
    boolean shouldIgnore = Boolean.TRUE.equals(matchesIgnore) ||
        (acceptConditions != null && acceptConditions.size() > 0 && !Boolean.TRUE.equals(matchesAccept));

    if (shouldIgnore) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(
            "Bean " + getName() + " will be ignored since it either matches ignore or doesn't match accept condition ("
                +
                "matchesIgnore=" + matchesIgnore + ", matchesAccept=" + matchesAccept + ", acceptConditionsCount=" +
                (acceptConditions != null ? acceptConditions.size() : 0));
      }
    }

    return shouldIgnore;
  }

  private boolean tagsMatch(String tag1, String tag2) {
    if (tag1 == null && tag2 == null) {
      return true;
    } else if (tag1 == null || tag2 == null) {
      return false;
    } else {
      return tag1.equals(tag2);
    }
  }

  public void readAttributeObservations(ObservationDefinitionConfig observationDefinition)
      throws ConfigurationFailedException {
    List<T> attributeObservations = new FastList<T>();
    for (MetricConfig metric : observationDefinition.getMetric()) {
      String source = metric.getSource();
      if (source == null || source.trim().equals("")) {
        throw new ConfigurationFailedException("Missing mandatory 'source' attribute for 'metric'");
      }

      if (metric.getType() == null) {

      }

      String type = metric.getType() != null ? metric.getType().toLowerCase() : null;
      String finalAttributeName;

      T attributeObservation = null;
      DerivedAttribute derivedAttribute = null;

      if (source.startsWith(SOURCE_TYPE_EVAL + ":") ||
          source.startsWith(SOURCE_TYPE_FUNCTION + ":") || source.startsWith(SOURCE_TYPE_JMX + ":") ||
          source.startsWith(SOURCE_TYPE_JSON + ":") || source.startsWith(SOURCE_TYPE_OUTER + ":")) {
        if (getDerivedAttributes() == null) {
          setDerivedAttributes(new FastList<DerivedAttribute>());
        }

        finalAttributeName = metric.getName();

        if (finalAttributeName == null || "".equals(finalAttributeName.trim())) {
          throw new ConfigurationFailedException("Missing mandatory 'name' attribute for 'metric'");
        }

        // not great to have this logic here, but derived attributes are a special kind
        String typeLowercase = type.toLowerCase();
        MetricType metricType = typeLowercase.contains("gauge") ? MetricType.GAUGE :
            typeLowercase.contains("counter") ? MetricType.COUNTER :
                typeLowercase.contains("text") ? MetricType.TEXT : MetricType.OTHER;

        AgentAggregationFunction agentAggregationFunction = AttributeObservation.getAgentAggregationFunction(
            metricType, metric.getAgentAggregation());

        derivedAttribute = new DerivedAttribute(finalAttributeName, source,
                                                metric.isStateful(), null, this, agentAggregationFunction, metricType);
        getDerivedAttributes().add(derivedAttribute);
      } else {
        attributeObservation = createAttributeObservation(type);
        if (attributeObservation == null) {
          LOG.error("Unknown attribute observation type: " + type);
          throw new ConfigurationFailedException("Unknown attribute observation type: " + type);
        }
        attributeObservation.initFromConfig(metric);
        finalAttributeName = attributeObservation.getFinalName();
        attributeObservations.add(attributeObservation);
      }

      if (!metric.isSend()) {
        if (getOmitAttributes() == null) {
          setOmitAttributes(new HashSet<String>());
        }
        getOmitAttributes().add(finalAttributeName);
      }

      if (type == null || type.trim().equals("")) {
        // type can be empty only if send=false
        if (metric.isSend()) {
          // although derived attributes don't really have the type, it should be part of metric metainfo send to
          // server. So, derived attribute could collect the value and send it without knowing the type, but
          // it would be useful for the backend/UI since they wouldn't know what to do with it
          throw new ConfigurationFailedException("Missing mandatory 'type' attribute for element 'metric'");
        }
      }

      if (metric.getPctls() != null && !metric.getPctls().trim().equals("")) {
        if (percentilesDefinitions == null) {
          percentilesDefinitions = new FastList<PercentilesDefinition>(5);
        }
        percentilesDefinitions
            .add(new PercentilesDefinition(metric.getPctls(), attributeObservation, derivedAttribute));
      }
    }
    setAttributeObservations(attributeObservations);
  }

  protected abstract T createAttributeObservation(String type);

  public void readTagDefinitions(ObservationDefinitionConfig observationDefinition) {
    setTags(new LinkedHashMap<String, String>());
    for (TagConfig tag : observationDefinition.getTag()) {
      getTags().put(tag.getName(), tag.getValue());
    }
  }

  public void readIgnoreElements(ObservationDefinitionConfig observationDefinition) {
    if (!observationDefinition.getIgnore().isEmpty()) {
      setIgnoreConditions(new UnifiedMap<String, Set<String>>());
      for (IgnoreConfig ignore : observationDefinition.getIgnore()) {
        String elemName = ignore.getName();
        Set<String> conditions = getIgnoreConditions().get(elemName);
        if (conditions == null) {
          conditions = new LinkedHashSet<String>();
          getIgnoreConditions().put(elemName, conditions);
        }
        conditions.add(ignore.getValue());
      }
    }
  }

  public void readAcceptElements(ObservationDefinitionConfig observationDefinition) {
    if (!observationDefinition.getAccept().isEmpty()) {
      setAcceptConditions(new UnifiedMap<String, Set<String>>());
      for (AcceptConfig accept : observationDefinition.getAccept()) {
        String elemName = accept.getName();
        Set<String> conditions = getAcceptConditions().get(elemName);
        if (conditions == null) {
          conditions = new LinkedHashSet<String>();
          getAcceptConditions().put(elemName, conditions);
        }
        conditions.add(accept.getValue());
      }
    }
  }

  protected abstract void read(ObservationDefinitionConfig observationDefinition) throws ConfigurationFailedException;

  public abstract Set<ObservationBeanDump> collectStats(DATA_PROVIDER dataProvider);

  public abstract String getName();

  public List<T> getAttributeObservations() {
    return attributeObservations;
  }

  public void setAttributeObservations(List<T> attributeObservations) {
    this.attributeObservations = attributeObservations;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  public void setTags(Map<String, String> tags) {
    this.tags = tags;
  }

  public Map<String, String> getAttributeNameMappings() {
    return attributeNameMappings;
  }

  public void setAttributeNameMappings(Map<String, String> attributeNameMappings) {
    this.attributeNameMappings = attributeNameMappings;
  }

  public Map<String, Set<String>> getIgnoreConditions() {
    return ignoreConditions;
  }

  public void setIgnoreConditions(Map<String, Set<String>> ignoreConditions) {
    this.ignoreConditions = ignoreConditions;
  }

  public Set<String> getOmitAttributes() {
    return omitAttributes;
  }

  public List<DerivedAttribute> getDerivedAttributes() {
    return derivedAttributes;
  }

  public void setOmitAttributes(Set<String> omitAttributes) {
    this.omitAttributes = omitAttributes;
  }

  public void setDerivedAttributes(List<DerivedAttribute> derivedAttributes) {
    this.derivedAttributes = derivedAttributes;
  }

  public Map<String, Set<String>> getAcceptConditions() {
    return acceptConditions;
  }

  public void setAcceptConditions(Map<String, Set<String>> acceptConditions) {
    this.acceptConditions = acceptConditions;
  }

  public Map<String, AgentAggregationFunction> getAttributesToAgentAggregationFunctions() {
    return attributesToAgentAggregationFunctions;
  }

  public String getMetricNamespace() {
    return metricNamespace;
  }

  public Map<String, MetricType> getMetricTypes() {
    return metricTypes;
  }

  public List<PercentilesDefinition> getPercentilesDefinitions() {
    return percentilesDefinitions;
  }
}
