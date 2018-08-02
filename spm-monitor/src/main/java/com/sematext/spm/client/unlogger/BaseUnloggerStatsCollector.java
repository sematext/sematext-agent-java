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

import static com.sematext.spm.client.util.ReflectionUtils.ClassValue.cv;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.MultipleStatsCollector;
import com.sematext.spm.client.Serializer;
import com.sematext.spm.client.StatValues;
import com.sematext.spm.client.StatsCollectionFailedException;
import com.sematext.spm.client.unlogger.AgentHelper.DynamicSwitch;
import com.sematext.spm.client.unlogger.agg.Aggregate;
import com.sematext.spm.client.unlogger.agg.AsIsAggregator;
import com.sematext.spm.client.unlogger.agg.ResultAggregator;
import com.sematext.spm.client.unlogger.annotations.ResultTransformer;
import com.sematext.spm.client.unlogger.utils.UnloggerThreadFactory;
import com.sematext.spm.client.unlogger.utils.UnloggerThreadFactory.NamedRunnable;
import com.sematext.spm.client.util.CollectionUtils;
import com.sematext.spm.client.util.CollectionUtils.Function;
import com.sematext.spm.client.util.StringUtils;

public abstract class BaseUnloggerStatsCollector extends MultipleStatsCollector<LogLine> {

  private final String loggingName;
  private final String[] aspectClasses;
  private final Function<String, Boolean> pointcutFilter;

  private final AggregationFlow aggregator;

  private static final Log LOG = LogFactory.getLog(BaseUnloggerStatsCollector.class);

  /**
   * @param loggingName
   * @param aspectClasses
   */
  protected BaseUnloggerStatsCollector(String loggingName, String[] aspectClasses, String configName,
                                       Map<String, String> params) {
    super(Serializer.COLLECTD);
    this.loggingName = loggingName;
    this.aspectClasses = aspectClasses;
    this.pointcutFilter = Config.pointcutsFilter(configName, params);
    this.aggregator = AggregationFlow.make(loggingName);
  }

  @Override
  protected final Collection<LogLine> getSlice(Map<String, Object> outerMetrics) throws StatsCollectionFailedException {
    return aggregator.getSlice();
  }

  @Override
  protected void appendStats(LogLine line, StatValues statValues) {
    String name = getName() + "-" + line.getName();
    statValues.add(name);
    statValues.add(System.currentTimeMillis());

    for (Object val : line) {
      statValues.add(asString(val));
    }
  }

  @Override
  public void init(Instrumentation instrumentation) {
    Collection<Logspect> loggers = Logspect
        .make(aspectClasses, pointcutFilter, BaseUnloggerStatsCollector.class.getClassLoader());

    for (Logspect logspect : loggers) {
      List<ResultAggregator> aggregators = logspect.guice(ResultTransformer.class, ResultAggregator.class,
                                                          cv(String.class, logspect.getName()));

      if (aggregators.isEmpty()) {
        LOG.warn("No result transformers for -> " + logspect + " , use default");
        aggregators = defaultAggregators(logspect.getName());
      }
      aggregator.addAggregators(logspect.getName(), aggregators);
    }

    AgentHelper.registerLoggers(loggers, aggregator, instrumentation);

    aggregator.start();
  }

  private static List<ResultAggregator> defaultAggregators(String name) {
    return Collections.<ResultAggregator>singletonList(new AsIsAggregator(name));
  }

  @Override
  public String getName() {
    return loggingName;
  }

  @Override
  public String getCollectorIdentifier() {
    return "";
  }

  protected String asString(Object val) {
    return val == null ? null : val.toString();
  }

  private static final class AggregationFlow implements LogLineCollector, NamedRunnable {
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool(new UnloggerThreadFactory(
        "unlogger-agg"));

    private final String name;
    private final long pumpingDelay;

    private final Map<String, Collection<? extends ResultAggregator>> aggregators = new UnifiedMap<String, Collection<? extends ResultAggregator>>();

    private AggregationFlow(String name, long pumpingDelay) {
      this.name = name;
      this.pumpingDelay = pumpingDelay;
    }

    @Override
    public String getName() {
      return name;
    }

    // We use one global queue per collector
    // to maintaine global order order from all pointcuts
    private final Queue<LogLine> aggregateStream = new ConcurrentLinkedQueue<LogLine>();

    private final Slice slice = new Slice();

    private class Slice {

      private final List<LogLine> ungrouped = new FastList<LogLine>();
      private final Map<Object, Aggregate> grouped = new UnifiedMap<Object, Aggregate>();

      public synchronized void log(Queue<LogLine> aggregateStream) {

        for (LogLine line = aggregateStream.poll();
          //
             line != null;
          //
             line = aggregateStream.poll()) {

          Collection<? extends ResultAggregator> aggregators = aggregatorsFor(line.getName());
          if (aggregators == null) {
            ungrouped.add(line);
            return;
          }
          for (ResultAggregator aggregator : aggregators) {
            aggregator.aggregate(line, ungrouped, grouped);
          }
        }
      }

      private Collection<? extends ResultAggregator> aggregatorsFor(String sectionName) {
        return aggregators.get(sectionName);
      }

      public synchronized Collection<LogLine> get() {
        Collection<LogLine> res = new FastList<LogLine>();
        res.addAll(ungrouped);

        for (Aggregate aggregate : grouped.values()) {
          res.add(aggregate.toOut());
        }

        ungrouped.clear();
        grouped.clear();
        return res;
      }
    }

    protected Collection<LogLine> getSlice() {
      return slice.get();
    }

    @Override
    public void log(LogLine line) {
      aggregateStream.add(line);
    }

    @Override
    public void log(Collection<? extends LogLine> lines) {
      aggregateStream.addAll(lines);
    }

    public synchronized void start() {
      EXECUTOR_SERVICE.execute(this);
    }

    protected synchronized void addAggregators(String sectionName, Collection<? extends ResultAggregator> aggregators) {
      this.aggregators.put(sectionName, aggregators);
    }

    private static final long ONE_SECOND = 1000;

    public static AggregationFlow make(String name) {
      // Due to profiling 0.1s it is compomise between size of queue
      // of unaggregated events and overheads of threads wakeups, etc.
      return new AggregationFlow(name, ONE_SECOND / 10);
    }

    @Override
    public void run() {
      try {
        while (!Thread.currentThread().isInterrupted()) {
          LogLine element = aggregateStream.peek();
          if (element == null) {
            Thread.sleep(pumpingDelay);
          } else {
            slice.log(aggregateStream);
          }
          // No blocking queue here to minimaze influence to monitored code.
          // So, the monitored code push information from pointcuts
          // to global concurrent queue, to minimaze latencies.
          // Due to some "pumping" periods the aggregation thread is wake up
          // and perfrom aggregation.
          // for (;
          // //
          // element != null;
          // //
          // element = aggregateStream.poll()) {
          // slice.log(element);
          // }
        }
      } catch (Exception e) {
        LOG.error("Aggregation interrupted for -> " + Thread.currentThread().getName(), e);
      }
    }

  }

  public static final class Config {

    private Config() {
      // It's utility class, can't be instantiated
    }

    private static final Function<String, Boolean> EXCLUDE_NOTHING = new Function<String, Boolean>() {
      public Boolean apply(String orig) {
        return false;
      }

      ;
    };

    private static final Function<String, Boolean> EXCLUDE_ALL = new Function<String, Boolean>() {
      public Boolean apply(String orig) {
        return true;
      }

      ;
    };

    protected static final Function<String, Boolean> pointcutsFilter(String name, Map<String, String> params) {
      String configString = StringUtils.trim(params.get(name));
      if (StringUtils.isEmpty(configString) || configString.equals("all")) {
        return EXCLUDE_NOTHING;
      }

      if (configString.equals("none")) {
        return EXCLUDE_ALL;
      }

      String toExclude = StringUtils.prefixed(configString, "excluded:");

      if (toExclude != null) {
        return CollectionUtils.notContains(new HashSet<String>(Arrays.asList(toExclude.split(","))));
      }

      // Unknown config
      return EXCLUDE_ALL;
    }

    public static boolean isHardOff(Map<String, String> params) {
      return "true".equals(params.get("unloggerHardOff"));
    }

    public static boolean isDynamicOff(Map<String, String> params) {
      return "false".equals(params.get("unloggerEnabled"));
    }

  }

  public static void dynamicOnAllPointcuts() {
    AgentHelper.dynamicSwitchTo(DynamicSwitch.ON);
  }

  public static void dynamicOffAllPointcuts() {
    AgentHelper.dynamicSwitchTo(DynamicSwitch.OFF);
  }

}
