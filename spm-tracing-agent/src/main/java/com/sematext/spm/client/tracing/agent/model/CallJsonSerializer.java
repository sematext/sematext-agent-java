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
package com.sematext.spm.client.tracing.agent.model;

import java.io.StringWriter;

import com.sematext.spm.client.tracing.agent.util.JSON;

public final class CallJsonSerializer {
  private CallJsonSerializer() {
  }

  public static String toJson(Call call) {
    final StringWriter json = new StringWriter();
    final JSON.JSONWriter writer = new JSON.JSONWriter(json);
    writer.writeObjectStart();
    writer.writeKeyValue("parentCallId", call.getParentCallId());
    writer.writeKeyValue("callId", call.getCallId());
    writer.writeKeyValue("level", call.getLevel());
    writer.writeKeyValue("signature", call.getSignature());
    writer.writeKeyValue("startTimestamp", call.getStartTimestamp());
    writer.writeKeyValue("endTimestamp", call.getEndTimestamp());
    writer.writeObjectEnd();
    return json.toString();
  }
}
