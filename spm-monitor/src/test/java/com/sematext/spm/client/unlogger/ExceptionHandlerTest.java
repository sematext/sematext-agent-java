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
package com.sematext.spm.client.unlogger;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import com.sematext.spm.client.unlogger.errors.ExceptionHandler;
import com.sematext.spm.client.unlogger.errors.ExceptionHandler.ExceptionChain;

public class ExceptionHandlerTest {

  @Test
  public void oneUnhandledTest() {
    ExceptionHandler exceptionHandler = new ExceptionHandler();
    exceptionHandler.storeAsUnhandled(new Throwable());
    List<ExceptionChain> chains = exceptionHandler.collapseChains();
    Assert.assertEquals(1, chains.size());
    Assert.assertFalse(chains.get(0).isHandled());
  }

  @Test
  public void oneHandledTest() {
    ExceptionHandler exceptionHandler = new ExceptionHandler();
    exceptionHandler.store(new Throwable());
    List<ExceptionChain> chains = exceptionHandler.collapseChains();
    Assert.assertEquals(1, chains.size());
    Assert.assertTrue(chains.get(0).isHandled());
  }

  @Test
  public void oneUnhandledChainTest() {
    ExceptionHandler exceptionHandler = new ExceptionHandler();
    Throwable initial = new Throwable();
    exceptionHandler.store(initial);
    exceptionHandler.storeAsUnhandled(new Throwable(initial));
    List<ExceptionChain> chains = exceptionHandler.collapseChains();
    Assert.assertEquals(1, chains.size());
    Assert.assertFalse(chains.get(0).isHandled());
  }

  @Test
  public void oneUnhandledChainPlusOneHandledTest() {
    ExceptionHandler exceptionHandler = new ExceptionHandler();
    Throwable initialX = new Throwable();
    exceptionHandler.store(initialX);
    Throwable initialY = new Throwable();
    exceptionHandler.store(initialY);
    exceptionHandler.storeAsUnhandled(new Throwable(initialY));
    List<ExceptionChain> chains = exceptionHandler.collapseChains();
    Assert.assertEquals(2, chains.size());
    Assert.assertFalse(chains.get(0).isHandled());
    Assert.assertTrue(chains.get(1).isHandled());
  }

}
