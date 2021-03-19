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
package com.sematext.spm.client.sender.util;

import org.apache.flume.Context;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.sink.AbstractSink;

/** kept as no-op just for backward compatibility */
public class HttpBinaryPostSink extends AbstractSink implements DynamicUrlParamSink {
  @Override
  public Status process() throws EventDeliveryException {
    return Status.READY;
  }

  @Override
  public void configure(Context arg0) {
  }

  @Override
  public void updateAdditionalUrlParam(String key, String value) {
  }

  @Override
  public long getLastEventTakeTimestamp() {
    return System.currentTimeMillis();
  }
}
