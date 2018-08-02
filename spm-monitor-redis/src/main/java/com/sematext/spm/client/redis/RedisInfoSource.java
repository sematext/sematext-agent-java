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

import redis.clients.jedis.Jedis;

public final class RedisInfoSource {
  private static final String DEFAULT_HOST = "localhost";
  private static final int DEFAULT_PORT = 6379;
  // we have 2 Collectors grabbing (different) data from Redis response, so we cache the response to avoid 2nd hit to Redis
  // 2000 ms is long-enough TTL to ensure the 2nd Collector gets the cached version
  private static final int CACHE_TTL = 2000;
  private Jedis jedis;
  private long lastQueryTime = System.currentTimeMillis();
  private RedisInfo cachedInfo = RedisInfo.empty();
  private String password;

  private RedisInfoSource(final Jedis jedis, final String password) {
    this.jedis = jedis;
    this.password = password;
  }

  private RedisInfoSource(final Jedis jedis) {
    this(jedis, null);
  }

  public RedisInfo fetchInfo() {
    boolean cacheExpired = System.currentTimeMillis() - lastQueryTime > CACHE_TTL;

    if (cachedInfo.isEmpty() || cacheExpired) {
      if (password != null && !password.isEmpty()) {
        jedis.auth(password);
      }
      String info = jedis.info();
      cachedInfo = RedisInfo.parse(info);
    }
    return cachedInfo;
  }

  public static RedisInfoSource collector(String host, Integer port, String password) {
    final String fixedHost = host == null ? DEFAULT_HOST : host;
    final Integer fixedPort = port == null ? DEFAULT_PORT : port;
    return new RedisInfoSource(new Jedis(fixedHost, fixedPort), password);
  }

  public static RedisInfoSource collector(final String uri) {
    String hostPart;
    String password = null;
    Integer userInfoSeparatorIdx = uri.indexOf('@');
    if (userInfoSeparatorIdx >= 0) {
      password = uri.substring(0, userInfoSeparatorIdx);
      hostPart = uri.substring(userInfoSeparatorIdx + 1);
    } else {
      hostPart = uri;
    }
    String host;
    Integer port = null;
    int portSeparatorIdx = hostPart.indexOf(':');
    if (portSeparatorIdx >= 0) {
      host = hostPart.substring(0, portSeparatorIdx);
      port = Integer.parseInt(hostPart.substring(portSeparatorIdx + 1));
    } else {
      host = hostPart;
    }
    return collector(host, port, password);
  }
}
