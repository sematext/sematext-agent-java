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

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.GenericExtractor;
import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.StatsCollectorBadConfigurationException;
import com.sematext.spm.client.config.CollectorFileConfig.ConfigType;

public class GenericJmxExtractor extends GenericExtractor<JmxStatsExtractorConfig,
    JmxStatsExtractor, MBeanObservation, MBeanAttributeObservation, MBeanObservationContext, MBeanServerConnection> {
  private static final Log LOG = LogFactory.getLog(GenericJmxExtractor.class);

  private static final Map<String, Pattern> OBJECT_NAME_ELEMENTS_PATTERNS = new UnifiedMap<String, Pattern>();
  private static final Map<String, List<String>> SPLIT_OBJECT_NAMES = new UnifiedMap<String, List<String>>();

  public GenericJmxExtractor(String realMonitoredBeanPath, String configBeanName,
                             JmxStatsExtractorConfig originalConfig,
                             Map<String, String> beanPathTags, boolean multiResultAllowed)
      throws StatsCollectorBadConfigurationException, ConfigurationFailedException {
    super(realMonitoredBeanPath, configBeanName, originalConfig, beanPathTags, multiResultAllowed);
  }

  // this method is implemented in a way that it is insensitive to order of properties in objectName
  public static Map<String, String> getBeanPathTags(String realObjectName, String configObjectNamePattern,
                                                    List<String> regexpGroupNames)
      throws StatsCollectorBadConfigurationException {
    Map<String, String> beanPathTags = new UnifiedMap<String, String>();

    // split the pattern into domain and attributes, throw away those that don't have a placeholder, keep the order (it has to match order of regexpGroupNames)
    List<String> realObjectNameElements = splitObjectName(realObjectName);
    // split realObjectName the same way
    List<String> configObjectNamePatternElements = splitObjectName(configObjectNamePattern);
    // now for each element of split that has a placeholder, extract the value using regexp, match to regexpGroupNames to produce a tag
    int currentPlaceholderUsed = 0;
    for (int i = 0; i < configObjectNamePatternElements.size(); i++) {
      String configElement = configObjectNamePatternElements.get(i);
      if (!configElement.contains("*")) {
        continue;
      } else {
        // domain part can be without =, but after that we handle it a bit differently
        if (i > 0) {
          if (!configElement.contains("=")) {
            // nothing to match, but we have to skip the placeholders
            currentPlaceholderUsed = currentPlaceholderUsed + countOccurrences(configElement, '*');
            continue;
          }
        }
      }

      // we can also break early if there are no more unmatched placeholders
      if (currentPlaceholderUsed >= regexpGroupNames.size()) {
        break;
      }

      String configElementName =
          configElement.indexOf("=") == -1 ? configElement : configElement.substring(0, configElement.indexOf("="));
      Pattern configElementPatternMatcher = OBJECT_NAME_ELEMENTS_PATTERNS.get(configElement);
      if (configElementPatternMatcher == null) {
        String configElementRegexp = configElement.replaceAll("\\*", "\\(\\.\\*\\)");
        configElementPatternMatcher = Pattern.compile(configElementRegexp);
        OBJECT_NAME_ELEMENTS_PATTERNS.put(configElement, configElementPatternMatcher);
      }

      // i == 0 --> domain, special case, always on the first position
      String realElement = (i == 0) ?
          realObjectNameElements.get(0) :
          findElementWithName(configElementName, realObjectNameElements);
      if (realElement == null) {
        throw new StatsCollectorBadConfigurationException(
            "Can't find element with name " + configElementName + " in real objectName: " +
                realObjectName);
      }

      Matcher m = configElementPatternMatcher.matcher(realElement);
      if (!m.find()) {
        throw new StatsCollectorBadConfigurationException(
            "For object " + realObjectName + ", part " + realElement + " doesn't match regexp: "
                + configElementPatternMatcher);
      } else {
        for (int j = 1; j <= m.groupCount(); j++) {
          if (currentPlaceholderUsed >= regexpGroupNames.size()) {
            throw new StatsCollectorBadConfigurationException(
                "For " + configObjectNamePattern + " found more matching groups than defined names!");
          }
          String value = m.group(j);
          String groupName = regexpGroupNames.get(currentPlaceholderUsed);
          currentPlaceholderUsed++;

          if (groupName.startsWith(MBeanObservation.UNNAMED_GROUP_PREFIX)) {
            // those whose placeholder name starts with "unnamed_group" should be ignored
          } else {
            // TODO add proper unescaping of values, for now just handle quotes
            if (value.startsWith("\"") && value.endsWith("\"")) {
              value = value.substring(1, value.length() - 1);
            }

            beanPathTags.put(groupName, value);
          }
        }
      }
    }

    return beanPathTags;
  }

  // TODO externalize to some Util class
  private static int countOccurrences(String string, char character) {
    int count = 0;
    for (int i = 0; i < string.length(); i++) {
      if (string.charAt(i) == character) {
        count++;
      }
    }
    return count;
  }

  private static String findElementWithName(String configElementName, List<String> realObjectNameElements) {
    for (String realElement : realObjectNameElements) {
      if (realElement.startsWith(configElementName + "=")) {
        return realElement;
      }
    }
    return null;
  }

  private static synchronized List<String> splitObjectName(String objectName) {
    List<String> split = SPLIT_OBJECT_NAMES.get(objectName);

    if (split == null) {
      split = new FastList<String>();
      int indexOfDomainEnd = objectName.indexOf(":");
      split.add(objectName.substring(0, indexOfDomainEnd));
      objectName = objectName.substring(indexOfDomainEnd + 1);

      // from here on, split on comma
      String[] tmp = objectName.split(",");
      for (String element : tmp) {
        if (element.trim().equals("")) {
          continue;
        }
        split.add(element);
      }
      SPLIT_OBJECT_NAMES.put(objectName, split);
    }

    return split;
  }

  public static boolean canBeMonitored(String configBeanName, JmxStatsExtractorConfig originalConfig,
                                       Map<String, String> objectNameTags) {
    for (MBeanObservation obs : originalConfig.getObservations()) {
      if (obs.getName().equals(configBeanName) && !obs.shouldBeIgnored(objectNameTags)) {
        return true;
      }
    }

    return false;
  }

  @Override
  protected MBeanObservation createBeanObservation(MBeanObservation obsConfig, String configBeanName,
                                                   String realMonitoredBeanPath, Map<String, String> beanPathTags)
      throws ConfigurationFailedException {
    return new MBeanObservation(obsConfig, configBeanName, realMonitoredBeanPath, beanPathTags);
  }

  @Override
  protected JmxStatsExtractor createStatsExtractor(JmxStatsExtractorConfig config)
      throws StatsCollectorBadConfigurationException {
    return new JmxStatsExtractor(config);
  }

  @Override
  protected JmxStatsExtractorConfig createExtractorConfig(JmxStatsExtractorConfig originalConfig) {
    return new JmxStatsExtractorConfig(originalConfig, false);
  }

  @Override
  protected ConfigType getConfigType() {
    return ConfigType.JMX;
  }
}
