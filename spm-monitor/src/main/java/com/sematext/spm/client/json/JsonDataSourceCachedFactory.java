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
package com.sematext.spm.client.json;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.http.CachableReliableDataSourceBase;
import com.sematext.spm.client.http.HttpDataSourceAuthentication;
import com.sematext.spm.client.http.HttpDataSourceBasicAuthentication;
import com.sematext.spm.client.http.ServerInfo;

/**
 * Create Json based datasources and caches them (so for each data request URL there is only one)
 * data source instance.
 */
public final class JsonDataSourceCachedFactory {
  private static final Log LOG = LogFactory.getLog(JsonDataSourceCachedFactory.class);
  private static final Map<String, CustomJsonHandler<Object>> CUSTOM_JSON_HANDLERS =
      new UnifiedMap<String, CustomJsonHandler<Object>>();

  private Map<String, CachableReliableDataSourceBase<Object, JsonDataProvider>> datasources;
  private boolean https;
  private String hostname;
  private String port;
  private HttpDataSourceAuthentication auth;

  public static final Map<String, AtomicInteger> ASYNC_DATA_SOURCE_REFRESH_INTERVAL_MS_MAP = new UnifiedMap<String, AtomicInteger>();
  // AtomicInteger(9000);

  private static final Map<String, JsonDataSourceCachedFactory> FACTORIES = new UnifiedMap<String, JsonDataSourceCachedFactory>();

  private JsonDataSourceCachedFactory(ServerInfo jsonServerInfo) {
    LOG.info("Initializing " + JsonDataSourceCachedFactory.class.getName());

    String server = jsonServerInfo.getServer();
    String firstSevenChars = server.substring(0, Math.min(7, server.length())).toLowerCase();
    this.https = firstSevenChars.startsWith("https:/");
    this.hostname = jsonServerInfo.getHostname();
    this.port = jsonServerInfo.getPort();
    this.datasources = new ConcurrentHashMap<String, CachableReliableDataSourceBase<Object, JsonDataProvider>>();

    if (jsonServerInfo.getBasicHttpAuthUsername() != null && jsonServerInfo.getBasicHttpAuthPassword() != null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Using Basic HTTP Auth");
      }
      this.auth = new HttpDataSourceBasicAuthentication(jsonServerInfo.getBasicHttpAuthUsername(), jsonServerInfo
          .getBasicHttpAuthPassword());
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("No HTTP Auth used");
      }
    }

    LOG.info("Initialized " + JsonDataSourceCachedFactory.class.getName());
  }

  private static JsonDataSourceCachedFactory getFactory(ServerInfo jsonServerInfo) {
    JsonDataSourceCachedFactory f = FACTORIES.get(jsonServerInfo.getId());
    if (f == null) {
      f = new JsonDataSourceCachedFactory(jsonServerInfo);
      FACTORIES.put(jsonServerInfo.getId(), f);
    }
    return f;
  }

  public static CachableReliableDataSourceBase<Object, JsonDataProvider> getDataSource(ServerInfo jsonServerInfo,
                                                                                       String dataRequestUrl,
                                                                                       boolean async,
                                                                                       boolean useSmile) {
    return getDataSource(jsonServerInfo, dataRequestUrl, async, useSmile, null);
  }

  public static CachableReliableDataSourceBase<Object, JsonDataProvider> getDataSource(ServerInfo jsonServerInfo,
                                                                                       String dataRequestUrl,
                                                                                       boolean async, boolean useSmile,
                                                                                       String customJsonHandlerClassName) {
    if (dataRequestUrl == null) {
      return null;
    }

    JsonDataSourceCachedFactory factory = getFactory(jsonServerInfo);

    String dataSourceKey = (useSmile ? "smile:" : "") + dataRequestUrl;

    if (factory == null) {
      throw new IllegalStateException("JsonDataSourceCachedFactory not initialized!");
    } else {
      if (factory.datasources.containsKey(dataSourceKey)) {
        CachableReliableDataSourceBase<Object, JsonDataProvider> ds = factory.datasources.get(dataSourceKey);

        if (ds.isAsync() != async) {
          throw new IllegalStateException(
              "DataSource for URL: " + dataRequestUrl + " already created as async=" + ds.isAsync() +
                  ", can't be created again as async=" + async);
        }

        return ds;
      } else {
        synchronized (factory) {
          // avoiding DCL report from checkstyle by using a slightly different expression than before
          // synchronized block - we can avoid it here since we know that the map instance itself is
          // already safely initialized and the object we're fetching is being constructed safely
          // within larger synchronized block
          CachableReliableDataSourceBase ds = factory.datasources.get(dataSourceKey);
          if (ds != null) {
            CustomJsonHandler<Object> handler = ((JsonDataProvider) ds.getDataProvider()).getCustomJsonHandler();
            String handlerClass = handler != null ? handler.getClass().getName() : null;

            // we shouldn't provide a separate datasource - the reason is that json expressions that we use in collector configs
            // don't have support for specifying the handler so they depend on either not needing one or some other
            // piece of logic (like bean definition) already specifying the handler for the same datasource. However, that would
            // be the case where handlerClass!=null and customJsonHandlerClassName=null, which is the only one where we will not
            // print any messages

            // TODO consider separating DS by handler too, maybe allowing json expressions to specify the handler, though even
            // not allowing that may be ok??
            if (handlerClass == null) {
              if (customJsonHandlerClassName != null) {
                LOG.warn("For datasource with URL " + dataRequestUrl
                             + " found existing entry with customHandler=null, requested " +
                             "handler was:" + customJsonHandlerClassName
                             + ". No handler will be used for this request");
              }
            } else {
              if (customJsonHandlerClassName == null) {
                // the case where we don't print any warnings
              } else if (!handlerClass.equals(customJsonHandlerClassName)) {
                LOG.warn("For datasource with URL " + dataRequestUrl + " found existing entry with customHandler=" +
                             handlerClass + ", while requested handler was:" + customJsonHandlerClassName + ". Handler "
                             + handlerClass +
                             " will be used.");
              }
            }
            return ds;
          } else {
            JsonDataProvider jdp;

            if (factory.hostname != null && factory.port != null) {
              jdp = new JsonDataProvider(factory.https, factory.hostname, factory.port, dataRequestUrl, factory.auth, useSmile);
            } else {
              jdp = new JsonDataProvider(dataRequestUrl, factory.auth, useSmile);
            }

            if (customJsonHandlerClassName != null && !customJsonHandlerClassName.trim().equals("")) {
              CustomJsonHandler<Object> handlerClass = CUSTOM_JSON_HANDLERS.get(customJsonHandlerClassName);
              if (handlerClass == null) {
                try {
                  Class c = Class.forName(customJsonHandlerClassName);
                  handlerClass = (CustomJsonHandler<Object>) c.newInstance();
                  handlerClass.setJsonServerInfo(jsonServerInfo);

                  CUSTOM_JSON_HANDLERS.put(customJsonHandlerClassName, handlerClass);
                } catch (Throwable thr) {
                  LOG.error("Error while creating custom handler: " + customJsonHandlerClassName
                                + ", using default null", thr);
                }
              }

              jdp.setCustomJsonHandler(handlerClass);
            }

            ds = new CachableReliableDataSourceBase<Object, JsonDataProvider>(jdp, async, jsonServerInfo);

            factory.datasources.put(dataSourceKey, ds);

            return ds;
          }
        }
      }
    }
  }
}
