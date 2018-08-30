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

import org.apache.commons.io.FileUtils;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;

import com.sematext.spm.client.config.CollectorFileConfig;
import com.sematext.spm.client.config.CollectorFileConfig.ConfigType;
import com.sematext.spm.client.db.DbConnectionManager;
import com.sematext.spm.client.db.DbObservation;
import com.sematext.spm.client.db.DbStatsExtractorConfig;
import com.sematext.spm.client.db.GenericDbCollector;
import com.sematext.spm.client.db.GenericDbExtractor;
import com.sematext.spm.client.http.CachableReliableDataSourceBase;
import com.sematext.spm.client.jmx.GenericJmxCollector;
import com.sematext.spm.client.jmx.GenericJmxExtractor;
import com.sematext.spm.client.jmx.JmxMBeanServerConnectionWrapper;
import com.sematext.spm.client.jmx.JmxServiceContext;
import com.sematext.spm.client.jmx.JmxStatsExtractorConfig;
import com.sematext.spm.client.jmx.MBeanAttributeObservation;
import com.sematext.spm.client.jmx.MBeanObservation;
import com.sematext.spm.client.json.GenericJsonCollector;
import com.sematext.spm.client.json.GenericJsonExtractor;
import com.sematext.spm.client.json.JsonDataProvider;
import com.sematext.spm.client.json.JsonDataSourceCachedFactory;
import com.sematext.spm.client.json.JsonMatchingPath;
import com.sematext.spm.client.json.JsonObservation;
import com.sematext.spm.client.json.JsonStatsExtractorConfig;
import com.sematext.spm.client.json.JsonUtil;
import com.sematext.spm.client.jvm.JvmNotifBasedGcStatsCollector;
import com.sematext.spm.client.observation.AttributeObservation;
import com.sematext.spm.client.observation.ObservationBean;
import com.sematext.spm.client.tracing.TracingMonitorConfigurator;
import com.sematext.spm.client.util.CollectionUtils.FunctionT;
import com.sematext.spm.client.yaml.YamlConfigLoader;

public class GenericStatsCollectorsFactory extends StatsCollectorsFactory<StatsCollector<?>> {
  private static final Log LOG = LogFactory.getLog(GenericStatsCollectorsFactory.class);
  private final TracingMonitorConfigurator tracingConf = new TracingMonitorConfigurator();

  public static final Map<String, StatsCollector<?>> EXISTING_COLLECTORS_MAP = new UnifiedMap<String, StatsCollector<?>>();
  private static final int MAX_PCTLS_DEFINITIONS = 10;
  private static int CURRENT_COUNT_PCTLS_DEFINITIONS = 0;

  @Override
  public Collection<? extends StatsCollector<?>> create(Properties monitorProperties,
                                                        List<? extends StatsCollector<?>> currentCollectors,
                                                        MonitorConfig monitorConfig)
      throws StatsCollectorBadConfigurationException {
    try {
      final String jvmName = monitorConfig.getJvmName();
      final String appToken = monitorConfig.getAppToken();
      final String subType = monitorConfig.getSubType();

      // reload the props on each create call
//      Properties monitorProperties = new Properties();
//      File propsFile = MonitorUtil.fetchSpmMonitorPropertiesFileObject(appToken, jvmName, subType);
//      monitorProperties.load(new FileInputStream(propsFile));

      List<StatsCollector<?>> collectors = new FastList<StatsCollector<?>>();

      // for now just collectors are read from the config, we still can't mix token and collectors in the same config file (since config file
      // already contains some token)
      String configCollectors = MonitorUtil
          .stripQuotes(monitorProperties.getProperty("SPM_MONITOR_COLLECTORS", "").trim()).trim();
      List<String> types = new ArrayList<String>();
      if (configCollectors != null) {
        for (String c : configCollectors.split(",")) {
          c = c.trim();
          if ("".equals(c)) {
            continue;
          }
          types.add(c);
        }
      }

      GenericExtractor.SEND_JVM_NAME.set("true".equalsIgnoreCase(MonitorUtil.stripQuotes(monitorProperties
                                                                                             .getProperty("SPM_MONITOR_SEND_JVM_NAME", "false")
                                                                                             .trim()).trim()));

      if (MonitorUtil.MONITOR_RUNTIME_SETUP_JAVAAGENT.get()) {
        boolean tracingEnabled = "true".equalsIgnoreCase(MonitorUtil.stripQuotes(monitorProperties
                                                                                     .getProperty("SPM_MONITOR_TRACING_ENABLED", "false")
                                                                                     .trim()).trim());

        if (tracingEnabled) {
          try {
            // always configure, it collects only if right settings are present though (handles it internally)
            tracingConf
                .configure(monitorConfig, currentCollectors, collectors, Serializer.INFLUX, appToken, subType, jvmName);
          } catch (Throwable thr) {
            // don't propagate, just continue
            LOG.error("Error while configuring tracing conf", thr);
          }
        }
      }

      CURRENT_COUNT_PCTLS_DEFINITIONS = 0;

      LOG.info("Loading configs for collectors: " + types);

      for (String type : types) {
        File configDir = new File(MonitorUtil.SPM_MONITOR_COLLECTORS_CONFIG_BASE_DIR, type);
        scanForConfigs(type, configDir, currentCollectors, monitorConfig, monitorProperties, collectors);
      }

      // TODO externalize this a bit to live in separate handlers
      // note: JvmNotifBasedGcStatsCollector depends on sun.management package which was moved to com.sun.management in
      // Java 9 and also became internal API so --add-exports would also have to be used to make it available to agent
      if (types.contains("jvm") && MonitorUtil.JAVA_MAJOR_VERSION < 9) {
        updateCollector(currentCollectors, collectors, JvmNotifBasedGcStatsCollector.class, jvmName,
                        new FunctionT<String, JvmNotifBasedGcStatsCollector, StatsCollectorBadConfigurationException>() {
                          @Override
                          public JvmNotifBasedGcStatsCollector apply(String id) {
                            // TODO - Make serializer configurable
                            return new JvmNotifBasedGcStatsCollector(Serializer.INFLUX, appToken, jvmName, subType);
                          }
                        });
      }

      collectors = groupCollectorsByTags(collectors, monitorConfig);

      int collectorsCount = StatsCollector.getCollectorsCount(collectors);
      if (collectorsCount < 50) {
        LOG.info("Created " + collectors.size() + " collectors : " + collectors);
      } else {
        if (LOG.isDebugEnabled() && collectorsCount < 200) {
          LOG.info("Created " + collectors.size() + " collectors : " + collectors);
        } else {
          LOG.info("Created " + collectors.size() + " collectors");
        }
      }

      return collectors;
    } catch (Exception ex) {
      throw new StatsCollectorBadConfigurationException("Error while creating collectors!", ex);
    }
  }

  public void scanForConfigs(String collectorsGroup, File configFileDir,
                             List<? extends StatsCollector<?>> currentCollectors, MonitorConfig monitorConfig,
                             Properties monitorProperties, List<StatsCollector<?>> collectors) {
    if (!configFileDir.exists()) {
      LOG.warn("Config subdir " + configFileDir.getAbsolutePath() + " does not exist! Skipping...");
    } else if (configFileDir.isDirectory()) {
      LOG.info("Scanning subdir for configs: " + configFileDir.getAbsolutePath());
      File[] configFiles = configFileDir.listFiles();

      if (configFiles != null) {
        for (File configFile : configFiles) {
          scanForConfigs(collectorsGroup, configFile, currentCollectors, monitorConfig, monitorProperties, collectors);
        }
      } else {
        LOG.warn("Config subdir " + configFileDir.getAbsolutePath() + " is empty or can't be read");
      }
    } else if (configFileDir.isFile()) {
      loadConfigFile(configFileDir, currentCollectors, monitorConfig, monitorProperties, collectors);
    }
  }

  public void loadConfigFile(File configFile, List<? extends StatsCollector<?>> currentCollectors,
                             MonitorConfig monitorConfig, Properties monitorProperties,
                             List<StatsCollector<?>> collectors) {
    try {
      String configName = configFile.getName();

      if (!configName.endsWith(".yml") && !configName.endsWith(".yaml")) {
        LOG.warn("Only YAML config files are allowed, file " + configName + " will be skipped");
        return;
      }

      String configFileContent = FileUtils.readFileToString(configFile);
      StatsExtractorConfig<?> statsExtractorConfig = getStatsExtractorConfig(configFile, configFileContent, monitorProperties,
                                                                             monitorConfig);

      if (statsExtractorConfig == null) {
        LOG.info("Couldn't load " + configFile + ", it will be skipped");
        return;
      }

      if (!statsExtractorConfig.conditionsSatisfied()) {
        LOG.info("Skipping config " + configFile + " since conditions are not met");
        return;
      }

      int countPctlsDefinitions = getCountPctlsDefinitions(statsExtractorConfig);
      if (countPctlsDefinitions + CURRENT_COUNT_PCTLS_DEFINITIONS > MAX_PCTLS_DEFINITIONS) {
        LOG.warn("Skipping " + configName + " as it contains " + countPctlsDefinitions +
                     " pctls defintions which would go over the limit set at " + MAX_PCTLS_DEFINITIONS +
                     ", current count was " + CURRENT_COUNT_PCTLS_DEFINITIONS);
        return;
      }
      CURRENT_COUNT_PCTLS_DEFINITIONS += countPctlsDefinitions;

      ConfigType configType = statsExtractorConfig.getConfig().getType();
      if (configType == ConfigType.JMX) {
        createCollectorsForJmxConfig(configFile, monitorConfig, monitorProperties, collectors,
                                     configName, statsExtractorConfig);
      } else if (configType == ConfigType.JSON) {
        createCollectorsForJsonConfig(configFile, monitorConfig, monitorProperties, collectors,
                                      configName, statsExtractorConfig);
      } else if (configType == ConfigType.DB) {
        createCollectorsForDbConfig(configFile, monitorConfig, monitorProperties, collectors,
                                    configName, statsExtractorConfig);
      } else {
        LOG.warn("Currently unsupported config type found in file " + configName);
        return;
      }
    } catch (Throwable thr) {
      LOG.error("Error while reading config file: " + configFile, thr);
    }
  }

  private int getCountPctlsDefinitions(StatsExtractorConfig<?> statsExtractorConfig) {
    int count = 0;

    for (Object obs : statsExtractorConfig.getObservations()) {
      ObservationBean<?, ?> obsBean = (ObservationBean<?, ?>) obs;
      count += obsBean.getPercentilesDefinitions() != null ? obsBean.getPercentilesDefinitions().size() : 0;
    }

    return count;
  }

  public void createCollectorsForDbConfig(File configFile,
                                          MonitorConfig monitorConfig, Properties monitorProperties,
                                          List<StatsCollector<?>> collectors, String configName,
                                          StatsExtractorConfig<?> statsExtractorConfig)
      throws ConfigurationFailedException {

    DbStatsExtractorConfig config = (DbStatsExtractorConfig) statsExtractorConfig;

    String dbDriverClass = config.getDbDriverClass();
    String dbUrl = config.getDbUrl();
    String dbUser = config.getDbUser();
    String dbPassword = config.getDbPassword();
    String dbAdditionalConnectionParams = config.getDbAdditionalConnectionParams();

    DbConnectionManager dbConnectionManager = DbConnectionManager.getConnectionManager(dbUrl);

    if (dbConnectionManager == null) {
      try {
        dbConnectionManager = new DbConnectionManager(dbUrl, dbDriverClass, dbUser, dbPassword, dbAdditionalConnectionParams);
      } catch (Exception e) {
        LOG.error("Error while creating dbConnectionManager, skipping creation of collector factories", e);
        return;
      }
    }

    try {
      for (DbObservation obs : config.getObservations()) {
        if (GenericDbExtractor.canBeMonitored(obs.getName(), config, Collections.EMPTY_MAP)) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Bean can be monitored, will create a collector " + obs.getName());
          }
          String partCollectorId = GenericDbCollector.getCollectorIdentifier(configName, obs.getName(), "");
          String collectorId = GenericDbCollector.calculateIdForCollector(GenericDbCollector.class, partCollectorId);

          GenericDbCollector collector = findExistingCollector(GenericDbCollector.class, partCollectorId);

          if (collector != null) {
            collectors.add(collector);
          } else {
            // TODO - Make serializer configurable
            collector = new GenericDbCollector(obs.getMetricNamespace(), Serializer.INFLUX, monitorConfig,
                                               configName, "",
                                               obs.getName(), "", config, Collections.EMPTY_MAP);
            collectors.add(collector);

            EXISTING_COLLECTORS_MAP.put(collectorId, collector);
          }

          if (LOG.isDebugEnabled()) {
            LOG.debug("Collector " + collector + " added to list of current collectors");
          }

          collector.updateEnvTags(monitorConfig, monitorProperties);
        }
      }
    } catch (Throwable thr) {
      LOG.warn("Error while processing config " + configFile +
                   ". Skipping loading of beans associated with it in this iteration...", thr);
    }
  }

  public void createCollectorsForJsonConfig(File configFile,
                                            MonitorConfig monitorConfig, Properties monitorProperties,
                                            List<StatsCollector<?>> collectors, String configName,
                                            StatsExtractorConfig<?> statsExtractorConfig)
      throws StatsCollectorBadConfigurationException, ConfigurationFailedException {
    JsonStatsExtractorConfig config = (JsonStatsExtractorConfig) statsExtractorConfig;

    JsonDataSourceCachedFactory.ASYNC_DATA_SOURCE_REFRESH_INTERVAL_MS_MAP.put(
        config.getJsonServerInfo().getId(), new AtomicInteger((int) (monitorConfig.getMonitorCollectInterval()
            * 0.90)));

    CachableReliableDataSourceBase<Object, JsonDataProvider> dataSource = JsonDataSourceCachedFactory.getDataSource(
        config.getJsonServerInfo(), config.getDataRequestUrl(), config.isAsync(), config.isUseSmile(), config
            .getJsonHandlerClass());

    try {
      Object jsonData = dataSource.fetchData();

      if (LOG.isDebugEnabled()) {
        LOG.debug("For config " + configFile + " fetched data from URL " + config.getDataRequestUrl() + ", data was: "
                      + jsonData);
      }

      if (jsonData != null) {
        for (JsonObservation obs : config.getObservations()) {
          Collection<JsonMatchingPath> matchingPaths = JsonUtil
              .findDistinctMatchingPaths(jsonData, obs.getJsonDataNodePath());
          if (LOG.isDebugEnabled()) {
            LOG.debug(
                "For observation " + obs.getName() + ", looking for paths: " + obs.getJsonDataNodePath() + ", found " +
                    matchingPaths.size() + " paths");
          }
          for (JsonMatchingPath path : matchingPaths) {
            String objectPath = path.getFullObjectPath();

            if (LOG.isDebugEnabled()) {
              LOG.debug("Found matching path: " + objectPath);
            }
            // for each matched path, create one new generic json collector
            Map<String, String> beanPathTags = path.getPathAttributes();

            if (GenericJsonExtractor.canBeMonitored(obs.getName(), config, beanPathTags)) {
              if (LOG.isDebugEnabled()) {
                LOG.debug("Path can be monitored, will create a collector for: " + objectPath);
              }
              String partCollectorId = GenericJsonCollector
                  .getCollectorIdentifier(configName, obs.getName(), objectPath);
              String collectorId = GenericJsonCollector
                  .calculateIdForCollector(GenericJsonCollector.class, partCollectorId);

              GenericJsonCollector collector = findExistingCollector(GenericJsonCollector.class, partCollectorId);

              if (collector != null) {
                collectors.add(collector);
              } else {
                // TODO - Make serializer configurable
                collector = new GenericJsonCollector(obs.getMetricNamespace(), Serializer.INFLUX, monitorConfig,
                                                     configName, objectPath,
                                                     obs.getName(), obs.getJsonDataNodePath(), config, beanPathTags);
                collectors.add(collector);

                EXISTING_COLLECTORS_MAP.put(collectorId, collector);
              }

              if (LOG.isDebugEnabled()) {
                LOG.debug("Collector " + collector + " added to list of current collectors");
              }

              collector.updateEnvTags(monitorConfig, monitorProperties);
            }
          }
        }
      } else {
        LOG.warn("JSON data null for dataSource path " + config.getDataRequestUrl() + ", async=" + config.isAsync() +
                     ". Skipping loading of beans associated with it in this iteration...");
      }
    } finally {
      try {
        dataSource.close();
      } catch (Throwable thr) {
        LOG.error("Error while closing the datasource " + dataSource, thr);
      }
    }
  }

  public void createCollectorsForJmxConfig(File configFile,
                                           MonitorConfig monitorConfig, Properties monitorProperties,
                                           List<StatsCollector<?>> collectors, String configName,
                                           StatsExtractorConfig<?> statsExtractorConfig)
      throws IOException, StatsCollectorBadConfigurationException, ConfigurationFailedException {
    JmxStatsExtractorConfig config = (JmxStatsExtractorConfig) statsExtractorConfig;

    MBeanServerConnection conn = null;
    JmxMBeanServerConnectionWrapper wrapper = JmxMBeanServerConnectionWrapper.getInstance(
        JmxServiceContext.getContext(monitorConfig.getMonitorPropertiesFile()));

    if (wrapper != null) {
      conn = wrapper.getMbeanServerConnection();
    }

    if (conn != null) {
      for (MBeanObservation obs : config.getObservations()) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Looking at " + obs.getName() + " bean with definition " + obs.getOriginalObjectNamePattern() +
                        ", adjusted pattern: " + obs.getObjectNamePattern());
        }
        Set<ObjectInstance> beans = conn.queryMBeans(obs.getObjectNamePattern(), null);

        if (LOG.isDebugEnabled()) {
          LOG.debug("For " + obs.getName() + " found matching jmx beans: " + beans.size());
        }
        for (ObjectInstance oi : beans) {
          String resolvedBeanName = oi.getObjectName().toString();

          if (LOG.isDebugEnabled()) {
            LOG.debug("Checking bean: " + resolvedBeanName);
          }

          // extract tags based on object name and configuration (match config placeholders to real values)
          Map<String, String> beanPathTags = GenericJmxExtractor
              .getBeanPathTags(resolvedBeanName, obs.getObjectNamePattern().toString(),
                               obs.getObjectNamePatternGroupNames());

          if (LOG.isDebugEnabled()) {
            LOG.debug("For bean: " + resolvedBeanName + ", beanPathTags are: " + beanPathTags);
          }

          if (GenericJmxExtractor.canBeMonitored(obs.getName(), config, beanPathTags)) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Bean " + resolvedBeanName + " can be monitored, will create collector for it");
            }

            // check if there are any attributes with unresolved placeholders
            List<AttributeObservation<?>> unresolvedAttributes = getUnresolvedAttributes(obs);
            if (unresolvedAttributes.size() == 1) {
              // currently allow max 1 such attribute (to simplify the logic)

              // form regexp from its name (possibly remove any func:, eval: and similar prefixes?)
              // cache parsing results
              String attributeName = unresolvedAttributes.get(0).getAttributeName();
              attributeName = removeAttributePrefix(attributeName);
              ParsedAttributeName parsed = PARSED_ATTRIBUTE_NAMES.get(attributeName);
              if (parsed == null) {
                parsed = parseAttributeName(attributeName);
                PARSED_ATTRIBUTE_NAMES.put(attributeName, parsed);
              }

              try {
                // now iterate over all attributes of currently inspected bean, try to match
                MBeanInfo info = conn.getMBeanInfo(oi.getObjectName());
                if (info.getAttributes() != null) {
                  for (MBeanAttributeInfo attr : info.getAttributes()) {
                    Matcher m = parsed.patternMatcher.matcher(attr.getName());
                    if (m.find()) {
                      Map<String, String> allTags = new HashMap<String, String>();
                      allTags.putAll(beanPathTags);
                      resolveAttributePlaceholders(parsed, m, allTags);

                      String partCollectorId = GenericJmxCollector
                          .getCollectorIdentifier(configName, obs.getName(), resolvedBeanName);
                      String collectorId = GenericJmxCollector
                          .calculateIdForCollector(GenericJmxCollector.class, partCollectorId);
                      collectorId += attr.getName() + "_" + allTags.toString();
                      GenericJmxCollector collector = findExistingCollector(GenericJmxCollector.class, collectorId);

                      if (collector != null) {
                        collectors.add(collector);
                      } else {
                        // for each such attribute, create one new Generic collector (and for each collector create
                        // artifical ID which appends attribute name to what would otherwise be ID

                        // we have to change observation bean attribute names here!
                        // create a new config object with changed attribute
                        JmxStatsExtractorConfig configCopy = new JmxStatsExtractorConfig(config, true);
                        configCopy.replaceAttributeName(obs.getName(), unresolvedAttributes.get(0).getAttributeName(),
                                                        ((MBeanAttributeObservation) unresolvedAttributes.get(0))
                                                            .getCopy(attr.getName()));
                        collector = new GenericJmxCollector(obs.getMetricNamespace(), Serializer.INFLUX, monitorConfig,
                                                            configName, resolvedBeanName,
                                                            obs.getName(), obs
                                                                .getOriginalObjectNamePattern(), configCopy, allTags);
                        collectors.add(collector);

                        EXISTING_COLLECTORS_MAP.put(collectorId, collector);
                      }

                      if (LOG.isDebugEnabled()) {
                        LOG.debug("Collector " + collector + " added to list of current collectors");
                      }

                      collector.updateEnvTags(monitorConfig, monitorProperties);
                    }
                  }
                }
              } catch (Exception e) {
                LOG.error("Error while resolving attribute with placeholders: " + parsed.originalName +
                              ". Collector will be skipped", e);
              }
            } else if (unresolvedAttributes.isEmpty()) {
              String partCollectorId = GenericJmxCollector
                  .getCollectorIdentifier(configName, obs.getName(), resolvedBeanName);
              String collectorId = GenericJmxCollector
                  .calculateIdForCollector(GenericJmxCollector.class, partCollectorId);
              GenericJmxCollector collector = findExistingCollector(GenericJmxCollector.class, collectorId);

              if (collector != null) {
                collectors.add(collector);
              } else {
                // TODO - Make serializer configurable
                collector = new GenericJmxCollector(obs.getMetricNamespace(), Serializer.INFLUX, monitorConfig,
                                                    configName, resolvedBeanName,
                                                    obs.getName(), obs
                                                        .getOriginalObjectNamePattern(), config, beanPathTags);
                collectors.add(collector);

                EXISTING_COLLECTORS_MAP.put(collectorId, collector);
              }

              if (LOG.isDebugEnabled()) {
                LOG.debug("Collector " + collector + " added to list of current collectors");
              }

              collector.updateEnvTags(monitorConfig, monitorProperties);
            } else {
              LOG.error("For observation " + obs.getName()
                            + " found multiple attribute with variable-names, consider splitting it into " +
                            "multiple observation definitions, one for each such attribute. Skipping...");
            }
          }
        }
      }
    } else {
      LOG.warn("JMX connection is null, can't look for matching beans and create collectors");
    }
  }

  protected void resolveAttributePlaceholders(ParsedAttributeName parsed, Matcher m, Map<String, String> allTags) {
    for (int i = 1; i <= m.groupCount(); i++) {
      // for each matching attribute extract placeholder values, add them to new map which will
      // contain a union of those values and beanPathTags (and pass that union into collector constructor)
      String value = m.group(i);
      String key = parsed.placeholderNames.get(i - 1);
      allTags.put(key, value);
    }
  }

  private String removeAttributePrefix(String attributeName) {
    if (attributeName.startsWith("eval:")) {
      return attributeName.substring("eval:".length());
    } else if (attributeName.startsWith("complex:")) {
      return attributeName.substring("complex:".length());
    } else if (attributeName.startsWith("reflect:")) {
      return attributeName.substring("reflect:".length());
    } else if (attributeName.startsWith("const:")) {
      return attributeName.substring("const:".length());
    } else {
      return attributeName;
    }
  }

  private static final Map<String, ParsedAttributeName> PARSED_ATTRIBUTE_NAMES = new HashMap<String, ParsedAttributeName>();

  protected ParsedAttributeName parseAttributeName(String attributeName) {
    String tmpOriginalName = attributeName;
    List<String> placeholderNames = new ArrayList<String>();
    String regexp = "^";
    while (true) {
      int indexOfNextPlaceholder = attributeName.indexOf("${");
      int indexOfNextPlaceholderEnd = attributeName.indexOf("}");
      if (indexOfNextPlaceholder == -1 || indexOfNextPlaceholderEnd == -1
          || indexOfNextPlaceholder > indexOfNextPlaceholderEnd) {
        regexp += Pattern.quote(attributeName);
        break;
      }
      String placeholderName = attributeName.substring(indexOfNextPlaceholder + 2, indexOfNextPlaceholderEnd);
      regexp = regexp + Pattern.quote(attributeName.substring(0, indexOfNextPlaceholder)) + "(.*)";
      attributeName = attributeName.substring(indexOfNextPlaceholderEnd + 1);
      placeholderNames.add(placeholderName);
    }
    regexp += "$";
    Pattern patternMatcher = Pattern.compile(regexp);

    ParsedAttributeName parsed = new ParsedAttributeName();
    parsed.originalName = tmpOriginalName;
    parsed.regexp = regexp;
    parsed.placeholderNames = placeholderNames;
    parsed.patternMatcher = patternMatcher;

    return parsed;
  }

  private List<AttributeObservation<?>> getUnresolvedAttributes(MBeanObservation obs) {
    if (obs.getAttributeObservations() == null || obs.getAttributeObservations().isEmpty()) {
      return Collections.EMPTY_LIST;
    }
    List<AttributeObservation<?>> res = new ArrayList<AttributeObservation<?>>();
    for (AttributeObservation<?> attr : obs.getAttributeObservations()) {
      int indexOfPlaceholderStart = attr.getAttributeName().indexOf("${");
      int indexOfPlaceholderEnd = attr.getAttributeName().lastIndexOf("}");
      if (indexOfPlaceholderStart != -1 && indexOfPlaceholderEnd != -1
          && indexOfPlaceholderStart < indexOfPlaceholderEnd) {
        res.add(attr);
      }
    }
    return res;
  }

  private static final Map<String, Long> FILE_TO_LAST_MODIFIED = new HashMap<String, Long>();
  private static final Map<String, StatsExtractorConfig<?>> FILE_TO_LAST_CREATED_CONFIG_BEAN_MAP = new HashMap<String, StatsExtractorConfig<?>>();

  private StatsExtractorConfig<?> getStatsExtractorConfig(File configFile, String configFileContent,
                                                          Properties monitorProperties, MonitorConfig monitorConfig)
      throws ConfigurationFailedException {
    String fileKey = configFile.getAbsolutePath();

    // replace all monitor properties placeholders with real values from properties file
    for (Object property : monitorProperties.keySet()) {
      String propertyNamePlaceholder = "${" + String.valueOf(property) + "}";
      String propertyValue = MonitorUtil.stripQuotes(monitorProperties.getProperty(String.valueOf(property), "").trim())
          .trim();
      configFileContent = configFileContent.replace(propertyNamePlaceholder, propertyValue);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Config file " + configFile + " after resolving the placeholders: " + configFileContent);
    }

    Long lastModified = configFile.lastModified();
    Long previousLastModified = FILE_TO_LAST_MODIFIED.get(fileKey);

    if (previousLastModified != null && lastModified.equals(previousLastModified)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Using previous config object for " + fileKey + " since there were no changes");
      }
      return FILE_TO_LAST_CREATED_CONFIG_BEAN_MAP.get(fileKey);
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Creating new config object for " + fileKey);
      }
      StatsExtractorConfig<?> newConfig;

      CollectorFileConfig yamlConfig = YamlConfigLoader.load(configFileContent, configFile.getAbsolutePath());
      MonitorUtil.FILE_NAME_TO_LOADED_YAML_CONFIG.put(configFile.getAbsolutePath(), yamlConfig);

      ConfigType configType = yamlConfig.getType();

      if (configType == null) {
        LOG.warn("Config file " + configFile + " is missing mandatory attribute 'type' (values can be: jmx, json, db)");
        return null;
      }

      if (configType == ConfigType.JMX) {
        newConfig = new JmxStatsExtractorConfig(yamlConfig, monitorConfig);
      } else if (configType == ConfigType.JSON) {
        newConfig = new JsonStatsExtractorConfig(yamlConfig, monitorConfig);
      } else if (configType == ConfigType.DB) {
        newConfig = new DbStatsExtractorConfig(yamlConfig, monitorConfig);
      } else {
        throw new IllegalArgumentException("Extractor config can be created only for jmx, db or json configs!");
      }

      FILE_TO_LAST_MODIFIED.put(fileKey, lastModified);

      checkDuplicateBeanNames(configFile, newConfig);

      checkValidTagNames(configFile, newConfig);

      // if no errors, store the config into the map
      FILE_TO_LAST_CREATED_CONFIG_BEAN_MAP.put(fileKey, newConfig);

      return newConfig;
    }
  }

  private static final Set<String> RESERVED_TAG_NAMES = new HashSet<String>(Arrays.asList(
      GenericExtractor.OS_HOST_TAG, GenericExtractor.JVM_NAME_TAG,
      GenericExtractor.CONTAINER_HOST_HOSTNAME_TAG, GenericExtractor.CONTAINER_HOSTNAME_TAG));

  private void checkValidTagNames(File configFile, StatsExtractorConfig<?> newConfig)
      throws ConfigurationFailedException {
    for (Object bean : newConfig.getObservations()) {
      ObservationBean<?, ?> obsBean = (ObservationBean<?, ?>) bean;
      String beanName = obsBean.getName();

      Map<String, String> beanTags = obsBean.getTags();
      for (String tagName : beanTags.keySet()) {
        if (RESERVED_TAG_NAMES.contains(tagName)) {
          throw new ConfigurationFailedException(
              "Config " + configFile + " contains reserved tag name definition " + tagName +
                  " for bean " + beanName + ". Remove this tag definition to fix the config.");
        }
      }
    }
  }

  public void checkDuplicateBeanNames(File configFile, StatsExtractorConfig<?> newConfig)
      throws ConfigurationFailedException {
    Set<String> beanNames = new HashSet<String>(
        newConfig.getObservations().size() > 0 ? newConfig.getObservations().size() : 1);
    for (Object bean : newConfig.getObservations()) {
      ObservationBean<?, ?> obsBean = (ObservationBean<?, ?>) bean;
      String beanName = obsBean.getName();
      if (beanNames.contains(beanName)) {
        throw new ConfigurationFailedException(
            "Configuration file " + configFile + " contains multiple beans with same name: " + beanName);
      } else {
        beanNames.add(beanName);
      }
    }
  }

  private static final Map<String, GroupedByTagsCollector> GROUPED_BY_TAGS_COLLECTORS = new UnifiedMap<String, GroupedByTagsCollector>(100);

  private List<StatsCollector<?>> groupCollectorsByTags(List<StatsCollector<?>> collectors,
                                                        MonitorConfig monitorConfig) {
    Map<String, GroupedByTagsCollector> groupedCollectorsMap = new UnifiedMap<String, GroupedByTagsCollector>();
    List<StatsCollector<?>> nonGroupableCollectors = new FastList<StatsCollector<?>>();

    // clear collectors from existing GROUPED_BY_TAGS_COLLECTORS map before starting to group
    for (GroupedByTagsCollector gc : GROUPED_BY_TAGS_COLLECTORS.values()) {
      gc.reset();
    }

    for (StatsCollector<?> sc : collectors) {
      if (sc.producesMetricsAndTagsMaps()) {
        GenericCollectorInterface col = (GenericCollectorInterface) sc;

        boolean added = false;

        String possibleKey = GroupedByTagsCollector.getGroupedKey(col.getAppToken(),
                                                                  col.getMetricsNamespace(), GroupedByTagsCollector
                                                                      .getTagsAsString(col.getGenericExtractor()
                                                                                           .getPartlyResolvedObservationConfigTags()));

        GroupedByTagsCollector possibleGroup = GROUPED_BY_TAGS_COLLECTORS.get(possibleKey);
        if (possibleGroup != null) {
          if (possibleGroup.accept(sc)) {
            groupedCollectorsMap.put(possibleKey, possibleGroup);
            added = true;
          } else {
            // skip such collector, something is not right
            LOG.error("Bad state encountered, found possible matching group " + possibleKey
                          + ", but collector was not accepted, " +
                          "collector was " + sc);
            continue;
          }
        }

        // check if everything resolved too        
        if (!col.getGenericExtractor().isAllConfigTagsResolved()) {
          nonGroupableCollectors.add(sc);
        } else {
          if (!added) {
            // means there was no such group at all
            Map<String, String> tags = col.getGenericExtractor().getPartlyResolvedObservationConfigTags();
            String tagsAsString = GroupedByTagsCollector.getTagsAsString(tags);
            String groupedByTagsCollectorKey = GroupedByTagsCollector.getGroupedKey(col.getAppToken(),
                                                                                    col.getMetricsNamespace(), tagsAsString);

            // compressing version of grouped collector is used
            GroupedByTagsCollector newGroup = new GroupedByTagsCollector(col.getMetricsNamespace(), col.getAppToken(),
                                                                         tags, Serializer.INFLUX, monitorConfig, true, true);
            GROUPED_BY_TAGS_COLLECTORS.put(groupedByTagsCollectorKey, newGroup);

            if (!newGroup.accept(sc)) {
              LOG.error("Collector " + sc + " not accepted by grouped collector which was based on it! Tags were :" +
                            tags);
              continue;
            }

            groupedCollectorsMap.put(groupedByTagsCollectorKey, newGroup);
          }
        }
      } else {
        nonGroupableCollectors.add(sc);
      }
    }

    nonGroupableCollectors.addAll(groupedCollectorsMap.values());
    LOG.info("Found " + groupedCollectorsMap.size() + " grouped collectors, total number of collectors: "
                 + nonGroupableCollectors.size());

    // remove previously cached grouped collectors which are not used anymore
    GROUPED_BY_TAGS_COLLECTORS.keySet().retainAll(groupedCollectorsMap.keySet());

    return nonGroupableCollectors;
  }

  /**
   * This method can return null.
   */
  private static <T extends StatsCollector<?>> T findExistingCollector(Class<T> class1, String id) {
    String calculatedId = StatsCollector.calculateIdForCollector(class1, id);

    StatsCollector<?> sc = EXISTING_COLLECTORS_MAP.get(calculatedId);

    if (sc != null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("For calculated ID " + calculatedId + " FOUND existing collector");
      }
      return (T) sc;
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("For calculated ID " + calculatedId + " found NO existing collectors");
        // LOG.debug("EXISTING MAP: " + EXISTING_COLLECTORS_MAP);
      }
      return null;
    }
  }
}

class ParsedAttributeName {
  String originalName;
  String regexp;
  Pattern patternMatcher;
  List<String> placeholderNames;
}
