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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sematext.spm.client.attributes.MetricType;
import com.sematext.spm.client.observation.MetricTypeMismatchDetector.Verdict;

public class MetricTypeMismatchDetectorTest {
  private static final int MIN_SAMPLES = MetricTypeMismatchDetector.DEFAULT_MIN_SAMPLES;

  private MetricTypeMismatchDetector detector() {
    return new MetricTypeMismatchDetector();
  }

  private Verdict recordAll(MetricTypeMismatchDetector detector, String metricKey, MetricType type, double... values) {
    Verdict lastVerdict = Verdict.NONE;
    for (double value : values) {
      Verdict verdict = detector.record(metricKey, type, value);
      if (verdict != Verdict.NONE) {
        lastVerdict = verdict;
      }
    }
    return lastVerdict;
  }

  @Test
  public void testGaugeWhichOnlyGrowsIsReportedAsCounter() {
    MetricTypeMismatchDetector detector = detector();

    for (int i = 1; i < MIN_SAMPLES; i++) {
      assertEquals(Verdict.NONE, detector.record("bean:requests", MetricType.GAUGE, i * 10L));
    }

    assertEquals(Verdict.LIKELY_COUNTER, detector.record("bean:requests", MetricType.GAUGE, MIN_SAMPLES * 10L));
  }

  @Test
  public void testGaugeWhichMovesInBothDirectionsIsNotReported() {
    MetricTypeMismatchDetector detector = detector();

    for (int i = 0; i < 5 * MIN_SAMPLES; i++) {
      assertEquals(Verdict.NONE, detector.record("bean:cpu", MetricType.GAUGE, (i % 5) * 1.5d));
    }
  }

  @Test
  public void testGaugeWhichNeverChangesIsNotReported() {
    MetricTypeMismatchDetector detector = detector();

    for (int i = 0; i < 5 * MIN_SAMPLES; i++) {
      assertEquals(Verdict.NONE, detector.record("bean:threads.max", MetricType.GAUGE, 200L));
    }
  }

  @Test
  public void testCounterWhichKeepsDroppingIsReportedAsGauge() {
    MetricTypeMismatchDetector detector = detector();
    Verdict verdict = Verdict.NONE;

    // value goes up and down, i.e. it is not cumulative at all
    for (int i = 0; i < 5 * MIN_SAMPLES; i++) {
      Verdict current = detector.record("bean:active.connections", MetricType.COUNTER, (i % 4) * 3L);
      if (current != Verdict.NONE) {
        verdict = current;
      }
    }

    assertEquals(Verdict.LIKELY_GAUGE, verdict);
  }

  @Test
  public void testCounterWithOccasionalResetIsNotReported() {
    MetricTypeMismatchDetector detector = detector();

    // a real counter which was reset twice, e.g. because the monitored service was restarted
    for (int round = 0; round < 3; round++) {
      for (int i = 1; i <= 5 * MIN_SAMPLES; i++) {
        assertEquals(Verdict.NONE, detector.record("bean:requests.total", MetricType.COUNTER, i * 7L));
      }
    }
  }

  @Test
  public void testMetricIsReportedOnlyOnce() {
    MetricTypeMismatchDetector detector = detector();
    int reports = 0;

    for (int i = 1; i <= 10 * MIN_SAMPLES; i++) {
      if (detector.record("bean:requests", MetricType.GAUGE, i * 10L) != Verdict.NONE) {
        reports++;
      }
    }

    assertEquals(1, reports);
  }

  @Test
  public void testNonNumericAndNotANumberValuesAreIgnored() {
    MetricTypeMismatchDetector detector = detector();

    for (int i = 1; i <= 10 * MIN_SAMPLES; i++) {
      assertEquals(Verdict.NONE, detector.record("bean:status", MetricType.GAUGE, "value-" + i));
      assertEquals(Verdict.NONE, detector.record("bean:ratio", MetricType.GAUGE, Double.NaN));
      assertEquals(Verdict.NONE, detector.record("bean:load", MetricType.GAUGE, Double.POSITIVE_INFINITY));
      assertEquals(Verdict.NONE, detector.record("bean:missing", MetricType.GAUGE, null));
    }
  }

  @Test
  public void testMetricsWhichAreNeitherGaugeNorCounterAreIgnored() {
    MetricTypeMismatchDetector detector = detector();

    for (int i = 1; i <= 10 * MIN_SAMPLES; i++) {
      assertEquals(Verdict.NONE, detector.record("bean:version", MetricType.TEXT, i * 10L));
      assertEquals(Verdict.NONE, detector.record("bean:other", MetricType.OTHER, i * 10L));
      assertEquals(Verdict.NONE, detector.record("bean:pctl", MetricType.PERCENTILE, i * 10L));
    }
  }

  @Test
  public void testNumberOfTrackedMetricsIsLimited() {
    MetricTypeMismatchDetector detector = new MetricTypeMismatchDetector(MIN_SAMPLES,
                                                                        MetricTypeMismatchDetector.DEFAULT_MIN_CHANGES,
                                                                        2);

    assertEquals(Verdict.LIKELY_COUNTER, recordAll(detector, "bean:first", MetricType.GAUGE, values(MIN_SAMPLES)));
    assertEquals(Verdict.LIKELY_COUNTER, recordAll(detector, "bean:second", MetricType.GAUGE, values(MIN_SAMPLES)));
    // detector is full, so the third metric is not tracked at all
    assertEquals(Verdict.NONE, recordAll(detector, "bean:third", MetricType.GAUGE, values(MIN_SAMPLES)));
  }

  @Test
  public void testResetForgetsCollectedValues() {
    MetricTypeMismatchDetector detector = detector();

    for (int i = 1; i < MIN_SAMPLES; i++) {
      assertEquals(Verdict.NONE, detector.record("bean:requests", MetricType.GAUGE, i * 10L));
    }
    detector.reset();

    // after reset the value collected above doesn't count anymore, so one more value is not enough for a conclusion
    assertEquals(Verdict.NONE, detector.record("bean:requests", MetricType.GAUGE, MIN_SAMPLES * 10L));
  }

  private double[] values(int count) {
    double[] values = new double[count];
    for (int i = 0; i < count; i++) {
      values[i] = (i + 1) * 10d;
    }
    return values;
  }
}
