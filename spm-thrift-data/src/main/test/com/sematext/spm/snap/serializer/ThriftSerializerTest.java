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

package com.sematext.spm.client.snap.serializer;

import org.junit.Test;

import com.sematext.spm.client.snap.thrift.ESSnapshot;
import com.sematext.spm.client.snap.thrift.MetaSnapshot;
import com.sematext.spm.client.snap.thrift.Type;

import junit.framework.Assert;

public class ThriftSerializerTest {
  @Test
  public void testCommonSnapSerialization() throws Exception {
    ThriftSerializer<MetaSnapshot> serializer = new ThriftSerializer<MetaSnapshot>();

    MetaSnapshot snap = new MetaSnapshot();
    snap.setTimestamp(System.currentTimeMillis());
    snap.setType(Type.ES);
    ESSnapshot esSnapshot = new ESSnapshot();
    esSnapshot.putToRequestsResponses("request", "response");
    snap.setEsSnapshot(esSnapshot);

    byte[] bytes = serializer.serialize(snap);
    MetaSnapshot deserializedSnap = new MetaSnapshot();
    serializer.deserialize(deserializedSnap, bytes);

    Assert.assertEquals(snap.getTimestamp(), deserializedSnap.getTimestamp());
    Assert.assertEquals(snap.getEsSnapshot(), deserializedSnap.getEsSnapshot());
    Assert.assertEquals(snap.getEsSnapshot().getRequestsResponses().get("request"), snap.getEsSnapshot()
        .getRequestsResponses().get("request"));
  }
}
