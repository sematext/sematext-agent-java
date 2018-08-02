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
package com.sematext.spm.client.tracing.agent.model.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.tracing.agent.model.ESAction;
import com.sematext.spm.client.tracing.agent.model.ESAction.OperationType;
import com.sematext.spm.client.tracing.agent.model.InetAddress;
import com.sematext.spm.client.util.Utils;

public class ESAnnotation {
  public static enum RequestType {
    INDEX_BULK(true, OperationType.INDEX),
    UPDATE_BULK(true, OperationType.UPDATE),
    DELETE_BULK(true, OperationType.DELETE),
    BULK(true, null),
    INDEX(false, OperationType.INDEX),
    DELETE(false, OperationType.DELETE),
    SEARCH(false, OperationType.SEARCH),
    GET(false, OperationType.GET),
    UPDATE(false, OperationType.UPDATE);

    final boolean bulk;
    final OperationType operationType;

    RequestType(boolean bulk, OperationType operationType) {
      this.bulk = bulk;
      this.operationType = operationType;
    }

    public static RequestType find(boolean bulk, OperationType type) {
      for (final RequestType opType : RequestType.values()) {
        if (opType.bulk == bulk && opType.operationType == type) {
          return opType;
        }
      }
      throw new IllegalArgumentException(
          "Can't find operation type corresponding to { bulk = " + bulk + ", type = " + type + "}");
    }
  }

  private List<ESAction> actions;
  private List<InetAddress> addresses;
  private String index;
  private RequestType requestType;
  private boolean bulk;

  public String getIndex() {
    return index;
  }

  public void setIndex(String index) {
    this.index = index;
  }

  public List<ESAction> getActions() {
    return actions;
  }

  public void setActions(List<ESAction> actions) {
    this.actions = actions;
  }

  public List<InetAddress> getAddresses() {
    return addresses;
  }

  public void setAddresses(List<InetAddress> addresses) {
    this.addresses = addresses;
  }

  public RequestType getRequestType() {
    return requestType;
  }

  public void setRequestType(RequestType requestType) {
    this.requestType = requestType;
  }

  public boolean isBulk() {
    return bulk;
  }

  public void setBulk(boolean bulk) {
    this.bulk = bulk;
  }

  private static class OperationTypeIndexAndType {
    private final OperationType operationType;
    private final String index;
    private final String type;

    OperationTypeIndexAndType(OperationType operationType, String index, String type) {
      this.operationType = operationType;
      this.index = index;
      this.type = type;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      OperationTypeIndexAndType that = (OperationTypeIndexAndType) o;

      if (index != null ? !index.equals(that.index) : that.index != null) return false;
      if (operationType != that.operationType) return false;
      if (type != null ? !type.equals(that.type) : that.type != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = operationType != null ? operationType.hashCode() : 0;
      result = 31 * result + (index != null ? index.hashCode() : 0);
      result = 31 * result + (type != null ? type.hashCode() : 0);
      return result;
    }
  }

  public static ESAnnotation make(List<InetAddress> addresses, boolean bulk, List<ESAction> actions) {
    if (actions.isEmpty()) {
      throw new IllegalArgumentException("Expected at least one action.");
    }

    final ESAnnotation annotation = new ESAnnotation();
    annotation.setAddresses(addresses);
    annotation.setBulk(bulk);
    if (!bulk) {
      annotation.setIndex(actions.get(0).getIndex());
      annotation.setRequestType(RequestType.find(false, actions.get(0).getOperationType()));
    } else {
      final Iterator<ESAction> iter = actions.iterator();
      ESAction action = iter.next();
      String index = action.getIndex();
      OperationType operationType = action.getOperationType();
      while (iter.hasNext()) {
        action = iter.next();
        if (!Utils.equal(action.getIndex(), index)) {
          index = null;
        }
        if (!Utils.equal(action.getOperationType(), operationType)) {
          operationType = null;
        }
      }
      annotation.setIndex(index);
      annotation.setRequestType(RequestType.find(true, operationType));
    }

    if (bulk) {
      final Map<OperationTypeIndexAndType, Integer> counts = new HashMap<OperationTypeIndexAndType, Integer>();
      for (ESAction action : actions) {
        OperationTypeIndexAndType t = new OperationTypeIndexAndType(action.getOperationType(), action.getIndex(), action
            .getType());
        Integer count = counts.get(t);
        if (count == null) {
          count = 0;
        }
        count++;
        counts.put(t, count);
      }
      final List<ESAction> groupedActions = new ArrayList<ESAction>();
      for (final Map.Entry<OperationTypeIndexAndType, Integer> entry : counts.entrySet()) {
        final OperationTypeIndexAndType key = entry.getKey();
        groupedActions.add(new ESAction(key.operationType, key.index, key.type, null, entry.getValue()));

        annotation.setActions(groupedActions);
      }
    } else {
      annotation.setActions(actions);
    }

    return annotation;
  }
}
