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
package com.sematext.spm.client;

import org.apache.thrift.TBase;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.snap.serializer.ThriftSerializer;
import com.sematext.spm.client.snap.serializer.ZipUtils;

public class BinarySerializer implements StatValuesSerializer<byte[]> {
  private ThriftSerializer serializer = new ThriftSerializer();

  @Override
  public byte[] serialize(List<Object> statValues) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    DataOutputStream s = new DataOutputStream(bos);

    Iterator<Object> it = statValues.iterator();
    while (it.hasNext()) {
      Object value = it.next();
      try {
        byte[] serializedValue = serializer.serialize((TBase) value);

        s.write(serializedValue);

      } catch (Exception e) {
        e.printStackTrace(System.err);
      } finally {
        try {
          s.close();
        } catch (IOException e) {
          e.printStackTrace(System.err);
        }
      }
    }

    byte[] compressedSnapshot = ZipUtils.zip(bos.toByteArray());

    return ByteBuffer.allocate(Integer.SIZE / Byte.SIZE + compressedSnapshot.length).
        putInt(compressedSnapshot.length).put(compressedSnapshot).array();
  }

  @Override
  public byte[] serialize(String metricNamespace, String appToken, Map<String, Object> metrics,
                          Map<String, String> tags, long timestamp) {
    throw new UnsupportedOperationException("Not supported for this type of data");
  }

  @Override public byte[] serializeMetainfo(String appToken, MetricMetainfo metaInfo) {
    throw new UnsupportedOperationException("Not supported for this type of data");
  }

  @Override
  public boolean shouldGeneratePrefix() {
    return true;
  }
}
