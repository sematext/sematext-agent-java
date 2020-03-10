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
package com.sematext.spm.client;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sematext.spm.client.aggregation.AgentAggregationFunction;
import com.sematext.spm.client.attributes.MetricType;
import com.sematext.spm.client.config.CollectorFileConfig.ConfigType;
import com.sematext.spm.client.observation.AttributeObservation;
import com.sematext.spm.client.observation.ObservationBean;
import com.sematext.spm.client.observation.PercentilesDefinition;
import com.sematext.spm.client.sender.SenderUtil;
import com.sematext.spm.client.tag.TagUtils;
import com.sematext.spm.client.util.StringUtils;
import com.sematext.spm.client.util.Tuple;

public abstract class GenericExtractor<S extends StatsExtractorConfig<O>, T extends StatsExtractor<S, O>,
    O extends ObservationBean<A, DATA_PROVIDER>, A extends AttributeObservation<DATA_SOURCE>, DATA_PROVIDER, DATA_SOURCE> {

  public static final AtomicBoolean SEND_JVM_NAME = new AtomicBoolean(false);

  public static final String JVM_NAME_TAG = "jvm";
  public static final String CONTAINER_HOSTNAME_TAG = "container.hostname";
  public static final String CONTAINER_NAME_TAG = "container.name";
  public static final String CONTAINER_ID_TAG = "container.id";
  public static final String CONTAINER_IMAGE_NAME_TAG = "container.image.name";
  public static final String CONTAINER_IMAGE_TAG_TAG = "container.image.tag";
  public static final String CONTAINER_IMAGE_DIGEST_TAG = "container.image.digest";
  public static final String K8S_POD_NAME_TAG = "kubernetes.pod.name";
  public static final String K8S_NAMESPACE_ID_TAG = "kubernetes.namespace";
  public static final String K8S_CLUSTER_TAG = "kubernetes.cluster.name";
  public static final String OS_HOST_TAG = "os.host";

  private static final Log LOG = LogFactory.getLog(GenericExtractor.class);

  private T statsExtractor;
  private S config;

  private Map<String, String> partlyResolvedObservationConfigTags;
  private String partlyResolvedObservationConfigTagsAsString;
  private boolean allConfigTagsResolved;
  private String configBeanName;
  private String realMonitoredBeanPath;
  private Map<String, String> beanPathTags;
  //  private String attributeNameMappingValue;
//  private Map<String, String> attributeNameMappings;
  private Map<String, AgentAggregationFunction> attributesToAgentAggregationFunctions;
  private Map<String, MetricType> metricTypes;
  private List<PercentilesDefinition> percentilesDefinitions;

  public boolean multiResultAllowed;

  public GenericExtractor(String realMonitoredBeanPath, String configBeanName,
                          S originalConfig, Map<String, String> beanPathTags,
                          boolean multiResultAllowed)
      throws StatsCollectorBadConfigurationException, ConfigurationFailedException {
    this.beanPathTags = beanPathTags;
    this.realMonitoredBeanPath = realMonitoredBeanPath;
    this.multiResultAllowed = multiResultAllowed;

    config = createExtractorConfig(originalConfig);
    this.statsExtractor = createStatsExtractor(config);
    this.configBeanName = configBeanName;
    // now find the observation bean, adjust its name, remove all other observations
    O obsConfig = null;
    for (O obs : config.getObservations()) {
      if (obs.shouldBeIgnored(beanPathTags)) {
        continue;
      }
      if (obs.getName().equals(configBeanName)) {
        obsConfig = obs;
        break;
      }
    }
    if (obsConfig != null) {
      config.getObservations().clear();
      O newObs = createBeanObservation(obsConfig, configBeanName, realMonitoredBeanPath, beanPathTags);
      config.getObservations().add(newObs);
      partlyResolvedObservationConfigTags = new HashMap<String, String>();
      Map<String, String> observationConfigTags = newObs.getTags();
      attributesToAgentAggregationFunctions = newObs.getAttributesToAgentAggregationFunctions();
      metricTypes = newObs.getMetricTypes();
      percentilesDefinitions = newObs.getPercentilesDefinitions();

      // iterate over all <tag> definitions from the config, try to resolve them now
      for (String tagName : observationConfigTags.keySet()) {
        String val = observationConfigTags.get(tagName);
        String resolvedVal = ObservationConfigTagResolver.resolve(beanPathTags, val, newObs);
        partlyResolvedObservationConfigTags.put(tagName, resolvedVal);
      }

      // not unmodifiable anymore - we refresh env tags on each config reload (every 1 min or so)
      // partlyResolvedObservationConfigTags = Collections.unmodifiableMap(partlyResolvedObservationConfigTags);

      allConfigTagsResolved = true;
      for (String tagValue : partlyResolvedObservationConfigTags.values()) {
        if (tagValue != null) {
          tagValue = tagValue.trim();
          // env tags shouldn't count into unresolved tags since they are resolved to env values which are equal to all
          // collected metrics, essentially meaning they act as if they are resolved
          if ((tagValue.startsWith("${") && !tagValue.startsWith("${env:")) || tagValue.startsWith("eval:")) {
            allConfigTagsResolved = false;
            break;
          }
        }
      }

      partlyResolvedObservationConfigTagsAsString = partlyResolvedObservationConfigTags.toString();
    } else {
      throw new StatsCollectorBadConfigurationException("Couldn't find monitorable bean with name " + configBeanName);
    }
  }

  public List<ExtractorResult> getStats(Map<String, Object> outerMetrics) throws StatsCollectionFailedException {
    List<ExtractorResult> res = new ArrayList<ExtractorResult>();
    Map<String, Map<String, Object>> stats = statsExtractor.extractStats(outerMetrics);

    if (!multiResultAllowed) {
      if (stats.size() == 1) {
        ExtractorResult singleRes = new ExtractorResult();
        singleRes.stats = stats.get(stats.keySet().iterator().next());

        // currently unused
//        Object mappedAttributeValue = res.stats.get(ObservationBean.ATTRIBUTE_NAME_MAPPING_MARKER);
//        if (mappedAttributeValue != null) {
//          if (attributeNameMappingValue != null && attributeNameMappings == null) {
//            throw new IllegalArgumentException("Attribute name mapping value used, but no mappings actually defined!");
//          }
//          
//          if (attributeNameMappings != null && attributeNameMappingValue != null) {
//            String newMappedAttributeName = attributeNameMappings.get(attributeNameMappingValue);
//            if (newMappedAttributeName == null) {
//              // if undefined, use the value itself
//              newMappedAttributeName = attributeNameMappingValue;
//            }
//            res.stats.put(newMappedAttributeName, mappedAttributeValue);
//            res.stats.remove(ObservationBean.ATTRIBUTE_NAME_MAPPING_MARKER);
//          }
//        }

        singleRes.tags = resolveTags(singleRes.stats);

        // after tags are resolved
        statsExtractor.removeOmitStats(stats);

        res.add(singleRes);
        return res;
      } else {
        // TODO at the moment GenericDbExtractor can actually return N beans (when N rows result from one query)
        // it would fail here, so we need to add support for multiple rows; however, we don't want to add such
        // support to json/jmx extractors, they use different logic (collectors/extractors are created in advance)
        // and having N beans in result would indicate an error; the solution is introduction of different,
        // MultipleStatsGenericExtractor and Collector
        throw new StatsCollectionFailedException("Expected 1 bean for " + configBeanName + ", but found: " + stats);
      }
    } else {
      for (String dumpKey : stats.keySet()) {
        Map<String, Object> singleDump = stats.get(dumpKey);
        ExtractorResult singleRes = new ExtractorResult();
        singleRes.stats = singleDump;
        singleRes.tags = resolveTags(singleRes.stats);
        res.add(singleRes);
      }
      statsExtractor.removeOmitStats(stats);

      return res;
    }
  }

  public Map<String, String> resolveTags(Map<String, Object> stats) throws StatsCollectionFailedException {
    Map<String, String> resolvedTags = new LinkedHashMap<String, String>(partlyResolvedObservationConfigTags.size());
    for (String tagName : partlyResolvedObservationConfigTags.keySet()) {
      String val = partlyResolvedObservationConfigTags.get(tagName);
      if (val == null) {
        resolvedTags.put(tagName, val);
        continue;
      }

      val = val.trim();
      if (val.startsWith("eval:")) {
        int indexOfKeyStart = val.indexOf("${");
        int indexOfKeyEnd = val.lastIndexOf("}");
        if (indexOfKeyStart == -1 || indexOfKeyEnd == -1 || (indexOfKeyEnd <= indexOfKeyStart)) {
          LOG.warn("Incorrect format of tag expressions: " + val + ", should look like: eval:${someMetricName})");
        } else {
          String statKey = val.substring(indexOfKeyStart + 2, indexOfKeyEnd);
          resolveTagToStatValue(stats, resolvedTags, tagName, statKey);
        }
      } else if (val.indexOf("${") != -1) {
        String statKey = val.substring(2, val.length() - 1).trim();

        // note: we shouldn't resolve env keys when first loading the config because it is possible we will want to allow 
        // some env values to change without restart (e.g. hostname, jvmName, etc)
        // if we do conclude that env values can be resolved at creation time, that would bring a performance boost as env
        // values wouldn't have to be resolved during each data collection
        resolveTagToStatValue(stats, resolvedTags, tagName, statKey);
      } else {
        resolvedTags.put(tagName, val);
      }
    }
    return resolvedTags;
  }

  public void resolveTagToStatValue(Map<String, Object> stats, Map<String, String> resolvedTags, String tagName,
                                    String statKey) throws StatsCollectionFailedException {
    Object statsVal = stats.get(statKey);
    if (statsVal != null) {
      resolvedTags.put(tagName, String.valueOf(statsVal));
    } else {
      String objectNameVal = beanPathTags.get(statKey);
      if (objectNameVal != null) {
        resolvedTags.put(tagName, objectNameVal);
      } else {
        // TODO this one could be thrown at creation time
        throw new StatsCollectionFailedException(
            "Can't resolve tag value for stats key: " + statKey + ", stats: " + stats +
                ", partlyResolved: " + partlyResolvedObservationConfigTags + ", monitoredBeanPath: "
                + realMonitoredBeanPath);
      }
    }
  }

  public void updateEnvTags(MonitorConfig monitorConfig, Properties monitorProperties) {
    if (SEND_JVM_NAME.get()) {
      String subType = monitorConfig.getSubType();
      if (subType == null || subType.trim().equals("")) {
        partlyResolvedObservationConfigTags.put(JVM_NAME_TAG, monitorConfig.getJvmName());
      } else {
        partlyResolvedObservationConfigTags
            .put(JVM_NAME_TAG, monitorConfig.getJvmName() + "-" + monitorConfig.getSubType());
      }
    }

    File monitorPropertiesFile = monitorConfig.getMonitorPropertiesFile();

    partlyResolvedObservationConfigTags
        .put(OS_HOST_TAG, SenderUtil.calculateHostParameterValue());

    String containerHostname = MonitorUtil.getContainerHostname(monitorPropertiesFile, monitorProperties,
        SenderUtil.isInContainer());
    if (containerHostname != null) {
      partlyResolvedObservationConfigTags.put(CONTAINER_HOSTNAME_TAG, containerHostname);
    }

    if (SenderUtil.isInContainer()) {
      String containerName = SenderUtil.getContainerName();
      if (containerName != null) {
        partlyResolvedObservationConfigTags.put(CONTAINER_NAME_TAG, containerName);
      }
      String containerId = SenderUtil.getContainerId();
      if (containerId != null) {
        partlyResolvedObservationConfigTags.put(CONTAINER_ID_TAG, containerId);
      }
      String containerImageName = SenderUtil.getContainerImageName();
      if (containerImageName != null) {
        partlyResolvedObservationConfigTags.put(CONTAINER_IMAGE_NAME_TAG, containerImageName);
      }
      String containerImageTag = SenderUtil.getContainerImageTag();
      if (containerImageTag != null) {
        partlyResolvedObservationConfigTags.put(CONTAINER_IMAGE_TAG_TAG, containerImageTag);
      }
      String containerImageDigest = SenderUtil.getContainerImageDigest();
      if (containerImageDigest != null) {
        partlyResolvedObservationConfigTags.put(CONTAINER_IMAGE_DIGEST_TAG, containerImageDigest);
      }
    }

    if (SenderUtil.isInKubernetes()) {
      String podName = SenderUtil.getK8sPodName();
      if (podName != null) {
        partlyResolvedObservationConfigTags.put(K8S_POD_NAME_TAG, podName);
      }
      String namespace = SenderUtil.getK8sNamespace();
      if (namespace != null) {
        partlyResolvedObservationConfigTags.put(K8S_NAMESPACE_ID_TAG, namespace);
      }
      String cluster = SenderUtil.getK8sCluster();
      if (cluster != null) {
        partlyResolvedObservationConfigTags.put(K8S_CLUSTER_TAG, cluster);
      }
    }
    
    for (Tuple<String, String> tuple : TagUtils.getConfigTags(monitorProperties)) {
      partlyResolvedObservationConfigTags.put(tuple.getFirst(), tuple.getSecond());
    }

    partlyResolvedObservationConfigTagsAsString = partlyResolvedObservationConfigTags.toString();
  }

  public void cleanup() {
    if (statsExtractor != null) {
      statsExtractor.close();
      statsExtractor = null;
    }
  }

  public Map<String, String> getPartlyResolvedObservationConfigTags() {
    return partlyResolvedObservationConfigTags;
  }

  public String getPartlyResolvedObservationConfigTagsAsString() {
    return partlyResolvedObservationConfigTagsAsString;
  }

  public String toString() {
    return getConfigType() + ":" + configBeanName + ":" + realMonitoredBeanPath;
  }

  public boolean isAllConfigTagsResolved() {
    return allConfigTagsResolved;
  }

  protected abstract O createBeanObservation(O obsConfig, String configBeanName, String realMonitoredBeanPath,
                                             Map<String, String> beanPathTags) throws ConfigurationFailedException;

  protected abstract T createStatsExtractor(S config) throws StatsCollectorBadConfigurationException;

  protected abstract S createExtractorConfig(S originalConfig) throws ConfigurationFailedException;

  protected abstract ConfigType getConfigType();

  public Map<String, AgentAggregationFunction> getAttributesToAgentAggregationFunctions() {
    return attributesToAgentAggregationFunctions;
  }

  public Map<String, MetricType> getMetricTypes() {
    return metricTypes;
  }

  public List<PercentilesDefinition> getPercentilesDefinitions() {
    return percentilesDefinitions;
  }
}
