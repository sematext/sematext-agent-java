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
package com.sematext.spm.client.tracing.agent.es.transport;

import com.sematext.spm.client.instrumentation.Delegate;
import com.sematext.spm.client.instrumentation.Getter;
import com.sematext.spm.client.instrumentation.Setter;

public interface SpmSearchRequestAccess {
  @Delegate(method = "types")
  String[] _$spm_tracing$_getTypes();

  @Delegate(method = "indices")
  String[] _$spm_tracing$_getIndices();

  @Getter(value = "_$spm_tracing$_searchRequestBuilder")
  Object _$spm_tracing$_getSearchRequestBuilder();

  @Setter(value = "_$spm_tracing$_searchRequestBuilder")
  void _$spm_tracing$_setSearchRequestBuilder(Object builder);
}
