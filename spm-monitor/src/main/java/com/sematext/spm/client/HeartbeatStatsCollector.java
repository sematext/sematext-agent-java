/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.spm.client;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Has to be set as the last collector in list.
 *
 * @author sematext, http://www.sematext.com/
 */
public class HeartbeatStatsCollector extends MultipleStatsCollector<Integer> {
  private static final Log LOG = LogFactory.getLog(HeartbeatStatsCollector.class);
  
  private static final Collection<Integer> NO_HEARTBEAT = new ArrayList<Integer>();
  private static final Collection<Integer> HEARTBEAT = new ArrayList<Integer>();
  
  static {
    HEARTBEAT.add(1);
  }
  
  private final String appToken;
  private final String jvmName;
  private final String subType;
  private final File propsFile;
  private final String finalJvmName;

  public HeartbeatStatsCollector(Serializer serializer, String appToken, String jvmName, String subType) {
    super(serializer);

    this.appToken = appToken;
    this.jvmName = jvmName;
    this.subType = subType;
    if (subType == null || subType.trim().equals("")) {
      this.finalJvmName = jvmName;
    } else {
      this.finalJvmName = jvmName + "-" + subType;
    }

    this.propsFile = MonitorUtil.fetchSpmMonitorPropertiesFileObject(appToken, jvmName, subType);
    LOG.info("Heartbeat stats collector initialized");
  }
  
  @Override
  public String getName() {
    return this.getClass().getName();
  }

  @Override
  public String getCollectorIdentifier() {
    return jvmName;
  }

  @Override
  protected Collection<Integer> getSlice(Map<String, Object> outerMetrics) throws StatsCollectionFailedException {
    if (CollectionStats.CURRENT_RUN_GATHERED_LINES.get() > 0) {
      return HEARTBEAT;
    } else {
      return NO_HEARTBEAT;
    }
  }

  @Override
  protected void appendStats(Integer protoStats, StatValues statValues) {
    statValues.setMetrics(new UnifiedMap<String, Object>());
    statValues.getMetrics().put("alive", 1);
    statValues.setTags(new UnifiedMap<String, String>());
    
    // currently heartbeat stats are gathered on the level of whole agent
    // statValues.getTags().put(GenericExtractor.JVM_NAME_TAG, finalJvmName);

    StatValuesHelper.fillHostTags(statValues, propsFile);
    statValues.setTimestamp(System.currentTimeMillis());
    statValues.setAppToken(appToken);
    statValues.setMetricNamespace("heartbeat");      
  }
}
