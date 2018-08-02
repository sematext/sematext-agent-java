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
package com.sematext.spm.client.storm;

public enum StormInfoMetric {
  //per cluster
  SUPERVISORS_COUNT {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.longExtractor("supervisors_count");
    }
  },
  TOPOLOGIES_COUNT {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.longExtractor("topologies_count");
    }
  },
  //per supervisor
  SLOTS_COUNT {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.longExtractor("slots_count");
    }
  },
  USED_SLOTS_COUNT {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.longExtractor("used_slots_count");
    }
  },
  SUPERVISOR_HOST {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.stringExtractor("supervisor_host");
    }
  },
  //per topology
  TOPOLOGY_STATUS {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.stringExtractor("topology_status");
    }
  },
  WORKERS_COUNT {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.longExtractor("workers_count");
    }
  },
  EXECUTORS_COUNT {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.longExtractor("executors_count");
    }
  },
  TASKS_COUNT {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.longExtractor("tasks_count");
    }
  },
  BOLTS_COUNT {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.longExtractor("bolts_count");
    }
  },
  SPOUTS_COUNT {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.longExtractor("spouts_count");
    }
  },
  STATE_SPOUTS_COUNT {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.longExtractor("state_spouts_count");
    }
  },
  BOLTS_EMITTED {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.counterExtractor("bolts_emitted");
    }
  },
  BOLTS_TRANSFERRED {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.counterExtractor("bolts_transferred");
    }
  },
  BOLTS_ASKED {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.counterExtractor("bolts_asked");
    }
  },
  BOLTS_EXECUTED {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.counterExtractor("bolts_executed");
    }
  },
  BOLTS_FAILED {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.counterExtractor("bolts_failed");
    }
  },
  BOLTS_EXECUTED_LATENCY {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.doubleCounterExtractor("bolts_executed_latency");
    }
  },
  BOLTS_PROCESSED_LATENCY {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.doubleCounterExtractor("bolts_processed_latency");
    }
  },
  SPOUTS_EMITTED {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.counterExtractor("spouts_emitted");
    }
  },
  SPOUTS_TRANSFERRED {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.counterExtractor("spouts_transferred");
    }
  },
  SPOUTS_ASKED {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.counterExtractor("spouts_asked");
    }
  },
  SPOUTS_FAILED {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.counterExtractor("spouts_failed");
    }
  },
  SPOUTS_COMPLETE_LATENCY {
    @Override
    public StormInfoMetricExtractor<Object> createMetricExtractor() {
      return StormInfoMetricExtractors.doubleCounterExtractor("spouts_complete_latency");
    }
  };

  public abstract StormInfoMetricExtractor<Object> createMetricExtractor();
}
