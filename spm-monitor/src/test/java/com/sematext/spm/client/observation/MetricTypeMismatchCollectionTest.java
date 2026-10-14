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
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.LogWriter;
import com.sematext.spm.client.config.MetricConfig;
import com.sematext.spm.client.config.ObservationDefinitionConfig;
import com.sematext.spm.client.json.JsonObservation;

/**
 * Checks that metric type mismatches are noticed while metrics are being collected, i.e. that the detector sees the
 * values agent reads from the monitored source.
 */
public class MetricTypeMismatchCollectionTest {
  private static final int COLLECTIONS = 2 * MetricTypeMismatchDetector.DEFAULT_MIN_SAMPLES;

  private static class CapturingLogWriter implements LogWriter<String> {
    private final StringBuffer output = new StringBuffer();

    @Override
    public void write(String logLine) {
      output.append(logLine).append("\n");
    }

    @Override
    public void write(String logLine, Throwable throwable) {
      output.append(logLine).append("\n");
    }
  }

  private CapturingLogWriter logWriter;
  private boolean detectorWasEnabled;

  @Before
  public void setup() {
    logWriter = new CapturingLogWriter();
    LogFactory.setLoggingLevel("DEBUG");
    LogFactory.init(logWriter);
    detectorWasEnabled = MetricTypeMismatchDetector.isEnabled();
    MetricTypeMismatchDetector.setEnabled(true);
    MetricTypeMismatchDetector.getInstance().reset();
  }

  @After
  public void tearDown() {
    MetricTypeMismatchDetector.setEnabled(detectorWasEnabled);
    MetricTypeMismatchDetector.getInstance().reset();
    LogFactory.setLoggingLevel("INFO");
  }

  @Test
  public void testGrowingGaugeIsReportedWhileCollecting() throws Exception {
    collect(observation("growing-gauge-bean", "gauge"), true);

    assertEquals(1, countWarnings("growing-gauge-bean:requests"));
    assertTrue(logWriter.output.toString().contains("is defined as a gauge"));
  }

  @Test
  public void testGrowingCounterIsNotReportedWhileCollecting() throws Exception {
    collect(observation("growing-counter-bean", "counter"), true);

    assertEquals(0, countWarnings("growing-counter-bean:requests"));
  }

  @Test
  public void testFluctuatingGaugeIsNotReportedWhileCollecting() throws Exception {
    collect(observation("fluctuating-gauge-bean", "gauge"), false);

    assertEquals(0, countWarnings("fluctuating-gauge-bean:requests"));
  }

  @Test
  public void testNothingIsCollectedWhenDetectorIsDisabled() throws Exception {
    MetricTypeMismatchDetector.setEnabled(false);

    collect(observation("disabled-bean", "gauge"), true);

    assertEquals(0, countWarnings("disabled-bean:requests"));
  }

  private JsonObservation observation(String beanName, String metricType) throws ConfigurationFailedException {
    MetricConfig metric = new MetricConfig();
    metric.setName("requests");
    metric.setSource("requests");
    metric.setType(metricType);

    ObservationDefinitionConfig observationDefinition = new ObservationDefinitionConfig();
    observationDefinition.setName(beanName);
    observationDefinition.setMetricNamespace("test");
    observationDefinition.setPath("$.");
    observationDefinition.setMetric(Arrays.asList(metric));

    return new JsonObservation(observationDefinition, null);
  }

  private void collect(JsonObservation observation, boolean growing) {
    boolean anyValueCollected = false;

    for (int i = 1; i <= COLLECTIONS; i++) {
      Map<String, Object> data = new HashMap<String, Object>();
      data.put("requests", growing ? (long) (i * 10) : (long) ((i % 5) * 10));

      for (ObservationBeanDump dump : observation.collectStats(data)) {
        anyValueCollected = anyValueCollected || dump.getAttributes().get("requests") != null;
      }
    }

    // makes sure the checks below are about the detector and not about metrics which were never collected
    assertTrue(anyValueCollected);
  }

  private int countWarnings(String metricKey) {
    int count = 0;
    for (String line : logWriter.output.toString().split("\n")) {
      if (line.contains("WARN") && line.contains("Metric " + metricKey + " is defined as a")) {
        count++;
      }
    }
    return count;
  }
}
