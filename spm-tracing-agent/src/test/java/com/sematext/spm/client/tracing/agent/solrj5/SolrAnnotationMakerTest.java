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

package com.sematext.spm.client.tracing.agent.solrj5;

import org.junit.Test;

import com.sematext.spm.client.tracing.agent.model.SolrAnnotation;
import com.sematext.spm.client.tracing.agent.model.SolrAnnotation.RequestType;

import junit.framework.TestCase;

public class SolrAnnotationMakerTest extends TestCase {
  @Test
  public void testExtractCollectionAndRequestTypeFromURL() throws Exception {
    SolrAnnotation annotation = SolrAnnotationMaker.fromHTTPRequest("http://localhost:8983/solr/collection-1/update");

    assertNotNull(annotation);
    assertEquals(RequestType.UPDATE, annotation.getRequestType());
    assertEquals("collection-1", annotation.getCollection());
  }

  @Test
  public void testIgnoreMissingCollectionPathSegment() throws Exception {
    SolrAnnotation annotation = SolrAnnotationMaker.fromHTTPRequest("http://localhost:8983/update");

    assertNotNull(annotation);
    assertEquals(RequestType.UPDATE, annotation.getRequestType());
    assertEquals(null, annotation.getCollection());
  }

  @Test
  public void sanity() throws Exception {
    assertNull(SolrAnnotationMaker.fromHTTPRequest("http://localhost:8983/"));
    assertNull(SolrAnnotationMaker.fromHTTPRequest("http://localhost:8983/unknown"));
    assertNull(SolrAnnotationMaker.fromHTTPRequest("http://localhost:8983/admin"));
    assertNull(SolrAnnotationMaker.fromHTTPRequest("http://localhost&&8983/admin"));
  }
}
