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
package com.sematext.spm.client.tracing.agent.stats;

import com.sematext.spm.client.tracing.agent.model.Call;
import com.sematext.spm.client.tracing.agent.model.PartialTransaction;

public class RequestComponentStatistics
    extends Statistics<RequestComponentStatistics.RequestComponent, RequestComponentMetric>
    implements StatisticsProcessor {
  public static class RequestComponent {
    private final String request;
    private final String component;

    public RequestComponent(String request, String component) {
      this.request = request;
      this.component = component;
    }

    public String getRequest() {
      return request;
    }

    public String getComponent() {
      return component;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      RequestComponent requestComponent = (RequestComponent) o;

      if (component != null ? !component.equals(requestComponent.component) : requestComponent.component != null)
        return false;
      if (request != null ? !request.equals(requestComponent.request) : requestComponent.request != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = request != null ? request.hashCode() : 0;
      result = 31 * result + (component != null ? component.hashCode() : 0);
      return result;
    }
  }

  private final MutableVarProvider varProvider;

  public RequestComponentStatistics(MutableVarProvider varProvider) {
    this.varProvider = varProvider;
  }

  @Override
  protected RequestComponentMetric newMetric(RequestComponent requestComponent) {
    return new RequestComponentMetric(requestComponent, varProvider);
  }

  @Override
  public void process(PartialTransaction transaction) {
    for (Call call : transaction.getCalls()) {
      final RequestComponentStatistics.RequestComponent component =
          new RequestComponentStatistics.RequestComponent(transaction.getRequest(), call.getCallTag().name());
      update(component, transaction, call);
    }
  }
}
