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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import com.sematext.spm.client.tracing.agent.model.ESAction;
import com.sematext.spm.client.tracing.agent.model.ESAction.OperationType;
import com.sematext.spm.client.tracing.agent.model.annotation.ESAnnotation.RequestType;

@RunWith(DataProviderRunner.class)
public class ESAnnotationTest {

  @DataProvider
  public static Object[][] bulkCasesProvider() {
    final List<Object[]> cases = new ArrayList<Object[]>();
    EnumSet<OperationType> bulkOperations = EnumSet.of(OperationType.DELETE, OperationType.UPDATE, OperationType.INDEX);
    for (OperationType operationType : bulkOperations) {
      cases.add(new Object[] {
          asList(new ESAction(operationType, "index", null, null), new ESAction(operationType, "index", null, null)),
          RequestType.valueOf(operationType.name() + "_BULK"),
          "index"
      });
      cases.add(new Object[] {
          asList(new ESAction(operationType, "index", null, null), new ESAction(operationType, "index-1", null, null)),
          RequestType.valueOf(operationType.name() + "_BULK"),
          null
      });
      EnumSet<OperationType> complementActions = EnumSet.copyOf(bulkOperations);
      complementActions.remove(operationType);
      cases.add(new Object[] {
          asList(new ESAction(operationType, "index", null, null), new ESAction(complementActions.iterator()
                                                                                    .next(), "index-1", null, null)),
          RequestType.BULK,
          null
      });
    }
    return cases.toArray(new Object[cases.size()][]);
  }

  @Test
  @UseDataProvider("bulkCasesProvider")
  public void testMakeShouldDetectSpecialBulkCases(List<ESAction> actions, RequestType type, String index)
      throws Exception {
    final ESAnnotation annotation = ESAnnotation.make(Collections.EMPTY_LIST, true, actions);
    assertEquals(annotation.getIndex(), index);
    assertEquals(annotation.getRequestType(), type);
  }

  @Test
  public void testShouldGroupBulkActionsByOpTypeIndexAndType() throws Exception {
    List<ESAction> actions = Arrays.asList(
        new ESAction(OperationType.DELETE, "index-1", "type-1", null),
        new ESAction(OperationType.DELETE, "index-1", "type-1", null),
        new ESAction(OperationType.DELETE, "index-1", "type-2", null),
        new ESAction(OperationType.INDEX, "index-3", "type-2", null),
        new ESAction(OperationType.INDEX, "index-1", "type-2", null),
        new ESAction(OperationType.INDEX, "index-1", "type-2", null)
    );

    final ESAnnotation annotation = ESAnnotation.make(Collections.EMPTY_LIST, true, actions);

    assertEquals(annotation.getActions().size(), 4);
    assertTrue(annotation.getActions().contains(new ESAction(OperationType.DELETE, "index-1", "type-1", null, 2)));
    assertTrue(annotation.getActions().contains(new ESAction(OperationType.DELETE, "index-1", "type-2", null, 1)));
    assertTrue(annotation.getActions().contains(new ESAction(OperationType.INDEX, "index-3", "type-2", null, 1)));
    assertTrue(annotation.getActions().contains(new ESAction(OperationType.INDEX, "index-1", "type-2", null, 2)));
  }
}
