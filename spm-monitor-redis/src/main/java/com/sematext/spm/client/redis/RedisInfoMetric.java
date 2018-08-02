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
package com.sematext.spm.client.redis;

public enum RedisInfoMetric {
  USED_MEMORY {
    @Override
    public RedisInfoMetricExtractor<Object> createMetricExtractor() {
      return RedisInfoMetricExtractors.longExtractor("used_memory");
    }
  },
  USED_MEMORY_PEAK {
    @Override
    public RedisInfoMetricExtractor<Object> createMetricExtractor() {
      return RedisInfoMetricExtractors.longExtractor("used_memory_peak");
    }
  },
  USED_MEMORY_RSS {
    @Override
    public RedisInfoMetricExtractor<Object> createMetricExtractor() {
      return RedisInfoMetricExtractors.longExtractor("used_memory_rss");
    }
  },
  CONNECTED_CLIENTS {
    @Override
    public RedisInfoMetricExtractor<Object> createMetricExtractor() {
      return RedisInfoMetricExtractors.longExtractor("connected_clients");
    }
  },
  EXPIRED_KEYS {
    @Override
    public RedisInfoMetricExtractor<Object> createMetricExtractor() {
      return RedisInfoMetricExtractors.counterExtractor("expired_keys");
    }
  },
  CONNECTED_SLAVES {
    @Override
    public RedisInfoMetricExtractor<Object> createMetricExtractor() {
      return RedisInfoMetricExtractors.longExtractor("connected_slaves");
    }
  },
  MASTER_LAST_IO_SECONDS_AGO {
    @Override
    public RedisInfoMetricExtractor<Object> createMetricExtractor() {
      return RedisInfoMetricExtractors.longExtractor("master_last_io_seconds_ago");
    }
  },
  KEYSPACE_HITS {
    @Override
    public RedisInfoMetricExtractor<Object> createMetricExtractor() {
      return RedisInfoMetricExtractors.counterExtractor("keyspace_hits");
    }
  },
  KEYSPACE_MISSES {
    @Override
    public RedisInfoMetricExtractor<Object> createMetricExtractor() {
      return RedisInfoMetricExtractors.counterExtractor("keyspace_misses");
    }
  },
  EVICTED_KEYS {
    @Override
    public RedisInfoMetricExtractor<Object> createMetricExtractor() {
      return RedisInfoMetricExtractors.counterExtractor("evicted_keys");
    }
  },
  TOTAL_COMMANDS_PROCESSED {
    @Override
    public RedisInfoMetricExtractor<Object> createMetricExtractor() {
      return RedisInfoMetricExtractors.counterExtractor("total_commands_processed");
    }
  },
  DB_KEYS {
    @Override
    public RedisInfoMetricExtractor<Object> createMetricExtractor() {
      return RedisInfoMetricExtractors.longDBExtractor("keys");
    }
  },
  DB_KEYS_EXPIRES {
    @Override
    public RedisInfoMetricExtractor<Object> createMetricExtractor() {
      return RedisInfoMetricExtractors.longDBExtractor("expires");
    }
  };

  public abstract RedisInfoMetricExtractor<Object> createMetricExtractor();
}
