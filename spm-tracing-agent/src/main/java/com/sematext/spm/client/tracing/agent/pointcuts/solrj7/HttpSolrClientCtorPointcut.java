/*
 *    Copyright (c) Sematext International
 *    All Rights Reserved
 *
 *    THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Sematext International
 *    The copyright notice above does not evidence any
 *    actual or intended publication of such source code.
 */
package com.sematext.spm.client.tracing.agent.pointcuts.solrj7;

import com.sematext.spm.client.tracing.agent.httpclient4.CloseableHttpClientAccess;
import com.sematext.spm.client.tracing.agent.solrj5.HttpSolrClientAccess;
import com.sematext.spm.client.tracing.agent.solrj5.SolrHttpClient;
import com.sematext.spm.client.unlogger.LoggerContext;
import com.sematext.spm.client.unlogger.UnloggableLogger;
import com.sematext.spm.client.unlogger.annotations.LoggerPointcuts;

@LoggerPointcuts(name = "http-solr7-client-ctor-pointcut", constructors = {
        "org.apache.solr.client.solrj.impl.HttpSolrClient(org.apache.solr.client.solrj.impl.HttpSolrClient$Builder builder)"
})
public class HttpSolrClientCtorPointcut implements UnloggableLogger {
    @Override
    public void logBefore(LoggerContext context) { }

    @Override
    public void logAfter(LoggerContext context, Object returnValue) {
        final HttpSolrClientAccess httpSolrClient = (HttpSolrClientAccess) context.getThat();
        final CloseableHttpClientAccess httpClient = (CloseableHttpClientAccess) httpSolrClient._$spm_tracing$_getHttpClient();
        SolrHttpClient.markSolrClient(httpClient);
    }

    @Override
    public void logThrow(LoggerContext context, Throwable throwable) { }
}
