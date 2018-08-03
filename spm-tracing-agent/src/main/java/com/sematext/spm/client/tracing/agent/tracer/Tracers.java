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
package com.sematext.spm.client.tracing.agent.tracer;

import java.util.Collection;

import com.sematext.spm.client.tracing.agent.config.Config;
import com.sematext.spm.client.tracing.agent.es.transport.*;
import com.sematext.spm.client.tracing.agent.httpclient3.HeaderAccess;
import com.sematext.spm.client.tracing.agent.httpclient3.HostConfigurationAccess;
import com.sematext.spm.client.tracing.agent.httpclient3.HttpClientAccess;
import com.sematext.spm.client.tracing.agent.httpclient3.HttpMethodAccess;
import com.sematext.spm.client.tracing.agent.httpclient3.HttpMethodDirectorAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.AbstractHttpClientAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.CloseableHttpClientAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.Header4Access;
import com.sematext.spm.client.tracing.agent.httpclient4.HttpHostAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.HttpMessageAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.HttpRequestAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.HttpResponseAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.RequestLineAccess;
import com.sematext.spm.client.tracing.agent.httpclient4.StatusLineAccess;
import com.sematext.spm.client.tracing.agent.jpa.SpmQueryAccess;
import com.sematext.spm.client.tracing.agent.pointcuts.custom.TracedMethodPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.es.transport.ActionRequestBuilderCtorPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.es.transport.ActionRequestBuilderExecutePointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.es.transport.AdapterActionFuturePointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.es.transport.PlainListenableActionFuturePointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.es.transport.SearchRequestPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.httpclient3.HttpClientPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.httpclient3.HttpMethodDirectorPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.httpclient4.CloseableHttpClientPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.httpclient42.AbstractHttpClientPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.jaxrs.JaxRsRequestMappingPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.jdbc.JDBCConnectionPrepareStatementPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.jdbc.JDBCDriverPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.jdbc.JDBCPreparedStatementPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.jdbc.JDBCStatementPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.jersey.ResponseWriterPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.jetty7.Jetty73HttpConnectionPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.jetty8.AbstractHttpConnectionPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.jetty9.HttpConnectionPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.jpa.JpaEntityManagerFindPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.jpa.JpaEntityManagerPersistPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.jpa.JpaEntityManagerQueryPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.jpa.JpaQueryPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.servlet.GetRequestDispatcherPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.servlet.RequestDispatcherPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.servlet.ServletPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.solrj4.ConcurrentUpdateSolrServerPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.solrj4.HttpSolrServerCtorPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.solrj5.ConcurrentUpdateSolrClientPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.solrj5.HttpSolrClientCtorPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.spring.SpringRequestMappingPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.thread.ExecutorServiceInvokePointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.thread.ExecutorServiceSubmitPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.thread.RunnableAndCallablePointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.thread.ThreadPointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.tomcat.ResponsePointcut;
import com.sematext.spm.client.tracing.agent.pointcuts.url.URLConnectionPointcut;
import com.sematext.spm.client.tracing.agent.servlet.SpmAsyncEventAccess;
import com.sematext.spm.client.tracing.agent.servlet.SpmGenericServletAccess;
import com.sematext.spm.client.tracing.agent.servlet.SpmHttpServletRequestAccess;
import com.sematext.spm.client.tracing.agent.servlet.SpmHttpServletResponseAccess;
import com.sematext.spm.client.tracing.agent.servlet.SpmRequestDispatcherAccess;
import com.sematext.spm.client.tracing.agent.servlet.SpmServletConfigAccess;
import com.sematext.spm.client.tracing.agent.solrj4.HttpSolrServerAccess;
import com.sematext.spm.client.tracing.agent.solrj5.HttpSolrClientAccess;
import com.sematext.spm.client.tracing.agent.solrj5.SolrParamsAccess;
import com.sematext.spm.client.tracing.agent.solrj5.SolrRequestAccess;
import com.sematext.spm.client.tracing.agent.solrj5.SolrResponseBaseAccess;
import com.sematext.spm.client.tracing.agent.solrj5.UpdateRequestAccess;
import com.sematext.spm.client.tracing.agent.sql.SpmConnectionAccess;
import com.sematext.spm.client.tracing.agent.sql.SpmPreparedStatementAccess;
import com.sematext.spm.client.tracing.agent.transformation.TracingTransform;
import com.sematext.spm.client.tracing.agent.transformation.transforms.MixinTransform;
import com.sematext.spm.client.unlogger.Logspect;

public enum Tracers {
  INTER_THREAD_COMMUNICATION(new InterThreadCommunication()),
  JDBC(new JDBCTracer()),
  JPA(new JpaTracer()),
  SERVLET(new ServletTracer()),
  JAVA_NET_URL(new JavaNetURLTracer()),
  HTTP_CLIENT_3(new HttpClient3Tracer()),
  HTTP_CLIENT_4(new HttpClient4Tracer()),
  HTTP_CLIENT_4_2(new HttpClient42Tracer()),
  SPRING_FRAMEWORK(new SpringFrameworkTracer()),
  JAX_RS(new JaxRsTracer()),
  TRACED_METHODS(new TracedMethodsTracer()),
  JETTY(new JettyTracer()),
  TOMCAT(new TomcatTracer()),
  ES_TRANSPORT_CLIENT(new ESTransportClient()),
  SOLR_7_CLIENT(new Solrj7Tracer()),
  SOLR_6_CLIENT(new Solrj6Tracer()),
  SOLR_5_CLIENT(new Solrj5Tracer()),
  SOLR_4_CLIENT(new Solrj4Tracer());

  private final Tracer tracer;

  Tracers(Tracer tracer) {
    this.tracer = tracer;
  }

  public Tracer getTracer() {
    return tracer;
  }

  private static abstract class BaseTracer implements Tracer {
    public abstract String[] getUnloggers();

    @Override
    public Collection<Logspect> createLogspects(ClassLoader loader) {
      return Logspect.make(getUnloggers(), loader);
    }
  }

  public static class InterThreadCommunication extends BaseTracer {
    @Override
    public String getName() {
      return "inter-thread";
    }

    @Override
    public String[] getUnloggers() {
      return new String[] {
          RunnableAndCallablePointcut.class.getName(),
          ThreadPointcut.class.getName(),
          ExecutorServiceSubmitPointcut.class.getName(),
          ExecutorServiceInvokePointcut.class.getName()
      };
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[0];
    }

    @Override
    public boolean enabled(Config config) {
      return config.isThreadInstrumentationEnabled();
    }
  }

  public static class JavaNetURLTracer extends BaseTracer {

    @Override
    public String getName() {
      return "java-net-url";
    }

    @Override
    public String[] getUnloggers() {
      return new String[] { URLConnectionPointcut.class.getName() };
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[] {};
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }
  }

  public static class HttpClient3Tracer extends BaseTracer {
    @Override
    public String getName() {
      return "http-client-3-tracer";
    }

    @Override
    public String[] getUnloggers() {
      return new String[] {
          HttpClientPointcut.class.getName(),
          HttpMethodDirectorPointcut.class.getName()
      };
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[] {
          new MixinTransform("org.apache.commons.httpclient.Header", HeaderAccess.class),
          new MixinTransform("org.apache.commons.httpclient.HostConfiguration", HostConfigurationAccess.class),
          new MixinTransform("org.apache.commons.httpclient.HttpMethod", HttpMethodAccess.class),
          new MixinTransform("org.apache.commons.httpclient.HttpMethodDirector", HttpMethodDirectorAccess.class),
          new MixinTransform("org.apache.commons.httpclient.HttpClient", HttpClientAccess.class)
      };
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }
  }

  public static class HttpClient4Tracer extends BaseTracer {
    @Override
    public String getName() {
      return "http-client-4-tracer";
    }

    @Override
    public String[] getUnloggers() {
      return new String[] {
          CloseableHttpClientPointcut.class.getName()
      };
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[] {
          new MixinTransform("org.apache.http.Header", Header4Access.class),
          new MixinTransform("org.apache.http.HttpHost", HttpHostAccess.class),
          new MixinTransform("org.apache.http.HttpMessage", HttpMessageAccess.class),
          new MixinTransform("org.apache.http.HttpRequest", HttpRequestAccess.class),
          new MixinTransform("org.apache.http.HttpResponse", HttpResponseAccess.class),
          new MixinTransform("org.apache.http.RequestLine", RequestLineAccess.class),
          new MixinTransform("org.apache.http.StatusLine", StatusLineAccess.class),
          new MixinTransform("org.apache.http.impl.client.CloseableHttpClient", CloseableHttpClientAccess.class)
      };
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }
  }

  public static class HttpClient42Tracer extends BaseTracer {
    @Override
    public String getName() {
      return "http-client-4.2-tracer";
    }

    @Override
    public String[] getUnloggers() {
      return new String[] {
          AbstractHttpClientPointcut.class.getName()
      };
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[] {
          new MixinTransform("org.apache.http.Header", Header4Access.class),
          new MixinTransform("org.apache.http.HttpHost", HttpHostAccess.class),
          new MixinTransform("org.apache.http.HttpMessage", HttpMessageAccess.class),
          new MixinTransform("org.apache.http.HttpRequest", HttpRequestAccess.class),
          new MixinTransform("org.apache.http.HttpResponse", HttpResponseAccess.class),
          new MixinTransform("org.apache.http.RequestLine", RequestLineAccess.class),
          new MixinTransform("org.apache.http.StatusLine", StatusLineAccess.class),
          new MixinTransform("org.apache.http.impl.client.AbstractHttpClient", AbstractHttpClientAccess.class)
      };
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }
  }

  public static class JDBCTracer extends BaseTracer {

    @Override
    public String getName() {
      return "jdbc";
    }

    @Override
    public String[] getUnloggers() {
      return new String[] {
          JDBCDriverPointcut.class.getName(),
          JDBCStatementPointcut.class.getName(),
          JDBCPreparedStatementPointcut.class.getName(),
          JDBCConnectionPrepareStatementPointcut.class.getName()
      };
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[] {
          new MixinTransform("java.sql.Connection", SpmConnectionAccess.class),
          new MixinTransform("java.sql.PreparedStatement", SpmPreparedStatementAccess.class)
      };
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }

  }

  public static class ServletTracer extends BaseTracer {

    @Override
    public String getName() {
      return "servlet";
    }

    @Override
    public String[] getUnloggers() {
      return new String[] {
          ServletPointcut.class.getName(),
          GetRequestDispatcherPointcut.class.getName(),
          RequestDispatcherPointcut.class.getName()
      };
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[] {
          new MixinTransform("javax.servlet.http.HttpServletRequest", SpmHttpServletRequestAccess.class),
          new MixinTransform("javax.servlet.http.HttpServletResponse", SpmHttpServletResponseAccess.class),
          new MixinTransform("javax.servlet.RequestDispatcher", SpmRequestDispatcherAccess.class),
          new MixinTransform("javax.servlet.AsyncEvent", SpmAsyncEventAccess.class),
          new MixinTransform("javax.servlet.GenericServlet", SpmGenericServletAccess.class),
          new MixinTransform("javax.servlet.ServletConfig", SpmServletConfigAccess.class)
      };
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }
  }

  public static class TracedMethodsTracer extends BaseTracer {

    @Override
    public String getName() {
      return "annotation";
    }

    @Override
    public String[] getUnloggers() {
      return new String[] { TracedMethodPointcut.class.getName() };
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[0];
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }

  }

  public static class SpringFrameworkTracer extends BaseTracer {
    @Override
    public String getName() {
      return "spring-framework";
    }

    @Override
    public String[] getUnloggers() {
      return new String[] { SpringRequestMappingPointcut.class.getName() };
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[0];
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }
  }

  public static class JpaTracer extends BaseTracer {
    @Override
    public String getName() {
      return "jpa";
    }

    @Override
    public String[] getUnloggers() {
      return new String[] {
          JpaEntityManagerQueryPointcut.class.getName(),
          JpaEntityManagerFindPointcut.class.getName(),
          JpaEntityManagerPersistPointcut.class.getName(),
          JpaQueryPointcut.class.getName()
      };
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[] {
          new MixinTransform("javax.persistence.Query", SpmQueryAccess.class)
      };
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }
  }

  public static class JaxRsTracer extends BaseTracer {
    @Override
    public String[] getUnloggers() {
      return new String[] {
          JaxRsRequestMappingPointcut.class.getName(),
          ResponseWriterPointcut.class.getName()
      };
    }

    @Override
    public String getName() {
      return "jax-rs";
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[0];
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }
  }

  public static class JettyTracer extends BaseTracer {
    @Override
    public String getName() {
      return "jetty";
    }

    @Override
    public String[] getUnloggers() {
      return new String[] {
          Jetty73HttpConnectionPointcut.class.getName(),
          HttpConnectionPointcut.class.getName(),
          AbstractHttpConnectionPointcut.class.getName()
      };
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[0];
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }
  }

  public static class TomcatTracer extends BaseTracer {
    @Override
    public String getName() {
      return "tomcat";
    }

    @Override
    public String[] getUnloggers() {
      return new String[] {
          ResponsePointcut.class.getName()
      };
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[0];
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }
  }

  public static class ESTransportClient extends BaseTracer {
    @Override
    public String getName() {
      return "es-transport-client";
    }

    @Override
    public String[] getUnloggers() {
      return new String[] {
          ActionRequestBuilderExecutePointcut.class.getName(),
          AdapterActionFuturePointcut.class.getName(),
          PlainListenableActionFuturePointcut.class.getName(),
          SearchRequestPointcut.class.getName(),
          ActionRequestBuilderCtorPointcut.class.getName()
      };
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[] {
          new MixinTransform("org.elasticsearch.action.support.AdapterActionFuture", SpmListenableActionFutureAccess.class),
          new MixinTransform("org.elasticsearch.action.index.IndexRequest", SpmIndexRequestAccess.class),
          new MixinTransform("org.elasticsearch.action.ActionRequestBuilder", SpmActionRequestBuilderAccess.class),
          new MixinTransform("org.elasticsearch.support.replication.ShardReplicationOperationRequest", SpmShardReplicationOperationRequestAccess.class),
          new MixinTransform("org.elasticsearch.action.delete.DeleteRequest", SpmDeleteRequestAccess.class),
          new MixinTransform("org.elasticsearch.action.search.SearchRequest", SpmSearchRequestAccess.class),
          new MixinTransform("org.elasticsearch.action.update.UpdateRequest", SpmUpdateRequestAccess.class),
          new MixinTransform("org.elasticsearch.action.get.GetRequest", SpmGetRequestAccess.class),
          new MixinTransform("org.elasticsearch.action.bulk.BulkRequest", SpmBulkRequestAccess.class),
          new MixinTransform("org.elasticsearch.action.support.single.instance.InstanceShardOperationRequest", SpmInstanceShardOperationRequestAccess.class),
          new MixinTransform("org.elasticsearch.action.support.single.shard.SingleShardOperationRequest", SpmSingleShardOperationRequestAccess.class)
      };
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }
  }

  public static class Solrj7Tracer extends BaseTracer {
    @Override
    public String[] getUnloggers() {
      return new String[] {
          com.sematext.spm.client.tracing.agent.pointcuts.solrj7.HttpSolrClientCtorPointcut.class.getName(),
          ConcurrentUpdateSolrClientPointcut.class.getName()
      };
    }

    @Override
    public String getName() {
      return "solr-7-client";
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[]{
          new MixinTransform("org.apache.solr.common.params.SolrParams", SolrParamsAccess.class),
          new MixinTransform("org.apache.solr.client.solrj.impl.HttpSolrClient", HttpSolrClientAccess.class),
          new MixinTransform("org.apache.solr.client.solrj.SolrRequest", SolrRequestAccess.class),
          new MixinTransform("org.apache.solr.client.solrj.request.UpdateRequest", UpdateRequestAccess.class),
          new MixinTransform("org.apache.solr.client.solrj.response.SolrResponseBase", SolrResponseBaseAccess.class)
      };
    }
  
    @Override
    public boolean enabled(Config config) {
      return true;
    }
  }
    
  public static class Solrj6Tracer extends BaseTracer {

    @Override
    public String[] getUnloggers() {
      return new String[] {
          com.sematext.spm.client.tracing.agent.pointcuts.solrj6.HttpSolrClientCtorPointcut.class.getName(),
          ConcurrentUpdateSolrClientPointcut.class.getName()

      };
    }

    @Override
    public String getName() {
      return "solr-6-client";
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[] {
          new MixinTransform("org.apache.solr.common.params.SolrParams", SolrParamsAccess.class),
          new MixinTransform("org.apache.solr.client.solrj.impl.HttpSolrClient", HttpSolrClientAccess.class),
          new MixinTransform("org.apache.solr.client.solrj.SolrRequest", SolrRequestAccess.class),
          new MixinTransform("org.apache.solr.client.solrj.request.UpdateRequest", UpdateRequestAccess.class),
          new MixinTransform("org.apache.solr.client.solrj.response.SolrResponseBase", SolrResponseBaseAccess.class)
      };
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }
  }

  public static class Solrj5Tracer extends BaseTracer {
    @Override
    public String[] getUnloggers() {
      return new String[] {
          HttpSolrClientCtorPointcut.class.getName(),
          ConcurrentUpdateSolrClientPointcut.class.getName()
      };
    }

    @Override
    public String getName() {
      return "solr-52-client";
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[] {
          new MixinTransform("org.apache.solr.common.params.SolrParams", SolrParamsAccess.class),
          new MixinTransform("org.apache.solr.client.solrj.impl.HttpSolrClient", HttpSolrClientAccess.class),
          new MixinTransform("org.apache.solr.client.solrj.SolrRequest", SolrRequestAccess.class),
          new MixinTransform("org.apache.solr.client.solrj.request.UpdateRequest", UpdateRequestAccess.class),
          new MixinTransform("org.apache.solr.client.solrj.response.SolrResponseBase", SolrResponseBaseAccess.class)
      };
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }
  }

  public static class Solrj4Tracer extends BaseTracer {
    @Override
    public String[] getUnloggers() {
      return new String[] {
          HttpSolrServerCtorPointcut.class.getName(),
          ConcurrentUpdateSolrServerPointcut.class.getName()
      };
    }

    @Override
    public String getName() {
      return "solr-4-client";
    }

    @Override
    public TracingTransform[] getStructuralTransforms() {
      return new TracingTransform[] {
          new MixinTransform("org.apache.solr.common.params.SolrParams", SolrParamsAccess.class),
          new MixinTransform("org.apache.solr.client.solrj.impl.HttpSolrServer", HttpSolrServerAccess.class),
          new MixinTransform("org.apache.solr.client.solrj.SolrRequest", SolrRequestAccess.class),
          new MixinTransform("org.apache.solr.client.solrj.request.UpdateRequest", UpdateRequestAccess.class),
          new MixinTransform("org.apache.solr.client.solrj.response.SolrResponseBase", SolrResponseBaseAccess.class)
      };
    }

    @Override
    public boolean enabled(Config config) {
      return true;
    }
  }
}
