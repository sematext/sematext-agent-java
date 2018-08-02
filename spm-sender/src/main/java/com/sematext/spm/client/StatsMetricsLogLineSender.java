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

import org.apache.flume.ChannelException;
import org.apache.flume.Event;
import org.apache.flume.agent.embedded.EmbeddedSource;
import org.apache.flume.event.SimpleEvent;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.Sender.SenderType;

public class StatsMetricsLogLineSender implements StatsLogLineBuilder<String, StatsCollector<String>> {
  private static final Log LOG = LogFactory.getLog(StatsMetricsLogLineSender.class);

  private static HashMap sortByValues(Map map) {
    List list = new LinkedList(map.entrySet());
    // Defined Custom Comparator here
    Collections.sort(list, new Comparator() {
      public int compare(Object o1, Object o2) {
        return ((Comparable) ((Map.Entry) (o2)).getValue())
            .compareTo(((Map.Entry) (o1)).getValue());
      }
    });

    // Here I am copying the sorted list in HashMap
    // using LinkedHashMap to preserve the insertion order
    HashMap sortedHashMap = new LinkedHashMap();
    for (Iterator it = list.iterator(); it.hasNext(); ) {
      Map.Entry entry = (Map.Entry) it.next();
      sortedHashMap.put(entry.getKey(), entry.getValue());
    }
    return sortedHashMap;
  }

  public static long COUNT = 0;

  @Override
  public String build(List<StatsCollector<String>> statsCollectors) {
    long t0 = System.currentTimeMillis();
    long totalCollectingTime = 0;

    EmbeddedSource source = getSource();
    if (source == null) {
      LOG.warn("Source is still null, can't write metrics");
      return null;
    }

    boolean logLines = StatsLoggingRegulator.shouldLogStats();
    StringBuilder sb = null;

    if (logLines) {
      sb = new StringBuilder();
    }

    Map<String, Long> collectingTimePerCollector = new HashMap<String, Long>();

    COUNT = 0;
    for (StatsCollector<String> collector : statsCollectors) {
      try {
        long t1 = System.currentTimeMillis();
        Iterator<String> data = collector.collect(null);
        COUNT++;
        long collectingTime = System.currentTimeMillis() - t1;

        if (LOG.isDebugEnabled()) {
          collectingTimePerCollector.put(collector.getId(), collectingTime);
        }

        totalCollectingTime += collectingTime;
        t1 = System.currentTimeMillis();
        while (data.hasNext()) {
          // this handles stats logs
          String statLine = data.next();
          String spmLogLine = StatsLogLineFormat.buildSpmLogLineToSend(statLine, System.currentTimeMillis(),
                                                                       collector.getSerializer()
                                                                           .shouldGeneratePrefix());

          // write to sender channel
          Event newEvent = new SimpleEvent();
          newEvent.setBody(spmLogLine.getBytes());
          source.put(newEvent);

          if (logLines) {
            sb.append(spmLogLine).append(MonitorUtil.LINE_SEPARATOR);
          }
        }
      } catch (ChannelException ce) {
        // handling channel errors, like channel-full
        // in this case we will stop further writing to the channel
        LOG.error("Failed to add stats line to flume channel, skipping writing of remaining lines", ce);
        break;
      } catch (Throwable thr) {
        // catching all exceptions here to prevent all stats gathering failure
        if (statsCollectors != null && statsCollectors.size() < 50) {
          LOG.error("Gathering stats failed, collector: " + collector + ", collectors: " + statsCollectors, thr);
        } else {
          LOG.error("Gathering stats failed, collector: " + collector, thr);
        }
        // DO NOTHING
      }
    }

    int totalCollectorsCount = StatsCollector.getCollectorsCount(statsCollectors);
    LOG.info(
        "Collectors collecting time: " + totalCollectingTime + ", total time: " + (System.currentTimeMillis() - t0));
    LOG.info("Collectors count: " + statsCollectors.size() + ", total collectors count: " + totalCollectorsCount);
    if (LOG.isDebugEnabled() && totalCollectorsCount < 500) {
      LOG.debug("Collecting time by collectors:\n" + sortByValues(collectingTimePerCollector));
    }

    return sb == null ? null : sb.toString();
  }

  private EmbeddedSource getSource() {
    return Sender.getSource(SenderType.STATS);
  }
}
