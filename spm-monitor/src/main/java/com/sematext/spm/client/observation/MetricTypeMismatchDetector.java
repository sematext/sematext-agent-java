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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.attributes.MetricType;

/**
 * Watches values read from the monitored source and warns when a metric is likely configured with the wrong type:
 * <ul>
 * <li>a metric defined as 'gauge' whose value only ever grows behaves like a counter</li>
 * <li>a metric defined as 'counter' whose value regularly drops behaves like a gauge</li>
 * </ul>
 * Values are inspected as they were read from the source (before counter values are turned into deltas) and a
 * conclusion is made only after a metric was collected at least {@link #DEFAULT_MIN_SAMPLES} times, since a couple
 * of consecutive values say nothing about the nature of a metric. Each metric is reported at most once.
 * <p/>
 * Tracking keeps some state for every collected metric, so it is done only when the agent runs with DEBUG logging
 * level (SPM_MONITOR_LOGGING_LEVEL=DEBUG).
 */
public final class MetricTypeMismatchDetector {
  private static final Log LOG = LogFactory.getLog(MetricTypeMismatchDetector.class);

  /**
   * How many values of a metric have to be collected before the detector makes any conclusion about that metric.
   */
  public static final int DEFAULT_MIN_SAMPLES = 30;

  /**
   * How many times a value has to move in the "wrong" direction before the detector reports a mismatch.
   */
  public static final int DEFAULT_MIN_CHANGES = 3;

  /**
   * Upper limit of tracked metrics, protects the agent from unbounded growth in case of very dynamic configs.
   */
  public static final int DEFAULT_MAX_TRACKED_METRICS = 5000;

  /**
   * A counter can drop when the monitored service restarts, so a metric defined as counter is reported as a gauge
   * only when such drops make at least 1/{@value #COUNTER_DROP_RATIO} of collected values.
   */
  private static final int COUNTER_DROP_RATIO = 10;

  private static final MetricTypeMismatchDetector INSTANCE = new MetricTypeMismatchDetector();

  private static volatile boolean enabled = LOG.isDebugEnabled();

  /**
   * Conclusion the detector made about a metric.
   */
  public enum Verdict {
    NONE,
    LIKELY_COUNTER,
    LIKELY_GAUGE
  }

  private final Map<String, MetricStats> trackedMetrics = new ConcurrentHashMap<String, MetricStats>();
  private final int minSamples;
  private final int minChanges;
  private final int maxTrackedMetrics;

  public MetricTypeMismatchDetector() {
    this(DEFAULT_MIN_SAMPLES, DEFAULT_MIN_CHANGES, DEFAULT_MAX_TRACKED_METRICS);
  }

  public MetricTypeMismatchDetector(int minSamples, int minChanges, int maxTrackedMetrics) {
    this.minSamples = minSamples;
    this.minChanges = minChanges;
    this.maxTrackedMetrics = maxTrackedMetrics;
  }

  public static MetricTypeMismatchDetector getInstance() {
    return INSTANCE;
  }

  /**
   * Tracking is off unless the agent logs on DEBUG level, in which case it can be switched on and off explicitly
   * (used by tests).
   */
  public static boolean isEnabled() {
    return enabled;
  }

  public static void setEnabled(boolean enabledParam) {
    enabled = enabledParam;
  }

  /**
   * Records one value collected for some metric and logs a warning when that value completes a mismatch.
   *
   * @param beanName    name of the observation bean the metric belongs to
   * @param metricName  name of the metric as it is used in the output
   * @param metricType  type the metric was defined with
   * @param measurement value as it was read from the monitored source
   */
  public void check(String beanName, String metricName, MetricType metricType, Object measurement) {
    if (metricName == null) {
      return;
    }

    String metricKey = (beanName == null) ? metricName : (beanName + ":" + metricName);
    Verdict verdict = record(metricKey, metricType, measurement);

    if (verdict == Verdict.LIKELY_COUNTER) {
      LOG.warn("Metric " + metricKey + " is defined as a gauge, but its source value only grew while " + minSamples +
                   " values were collected, which is how a counter behaves. Check the 'type' of that metric in its config.");
    } else if (verdict == Verdict.LIKELY_GAUGE) {
      LOG.warn("Metric " + metricKey + " is defined as a counter, but its source value dropped repeatedly while " +
                   minSamples + " values were collected, which is how a gauge behaves. Check the 'type' of that " +
                   "metric in its config.");
    }
  }

  /**
   * Records one value collected for some metric and returns the conclusion this value completes, if any. Only the
   * first mismatch found for a metric is returned, later values of that metric are ignored.
   */
  public Verdict record(String metricKey, MetricType metricType, Object measurement) {
    if (metricKey == null || (metricType != MetricType.COUNTER && metricType != MetricType.GAUGE)) {
      return Verdict.NONE;
    }
    if (!(measurement instanceof Number)) {
      return Verdict.NONE;
    }

    double value = ((Number) measurement).doubleValue();
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      return Verdict.NONE;
    }

    MetricStats stats = trackedMetrics.get(metricKey);
    if (stats == null) {
      if (trackedMetrics.size() >= maxTrackedMetrics) {
        return Verdict.NONE;
      }
      MetricStats newStats = new MetricStats();
      stats = trackedMetrics.putIfAbsent(metricKey, newStats);
      if (stats == null) {
        stats = newStats;
      }
    }

    synchronized (stats) {
      if (stats.reported) {
        return Verdict.NONE;
      }
      if (stats.samples > 0) {
        if (value > stats.previousValue) {
          stats.increases++;
        } else if (value < stats.previousValue) {
          stats.decreases++;
        }
      }
      stats.previousValue = value;
      stats.samples++;

      if (stats.samples < minSamples) {
        return Verdict.NONE;
      }

      Verdict verdict = Verdict.NONE;
      if (metricType == MetricType.GAUGE) {
        // value which never dropped, but did grow a few times, is a cumulative value, i.e. a counter
        if (stats.decreases == 0 && stats.increases >= minChanges) {
          verdict = Verdict.LIKELY_COUNTER;
        }
      } else {
        // an occasional drop is a restart of the monitored service, a value dropping regularly is not cumulative
        if (stats.decreases >= minChanges && stats.decreases * COUNTER_DROP_RATIO >= stats.samples) {
          verdict = Verdict.LIKELY_GAUGE;
        }
      }

      if (verdict != Verdict.NONE) {
        stats.reported = true;
      }
      return verdict;
    }
  }

  /**
   * Forgets everything collected so far (used by tests).
   */
  public void reset() {
    trackedMetrics.clear();
  }

  private static final class MetricStats {
    private double previousValue;
    private int samples;
    private int increases;
    private int decreases;
    private boolean reported;
  }
}
