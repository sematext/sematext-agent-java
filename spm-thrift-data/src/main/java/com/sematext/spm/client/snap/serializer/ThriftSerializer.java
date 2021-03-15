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

import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransportException;

public class ThriftSerializer<T extends TBase> implements Serializer<T> {
  private final TDeserializer deserializer;
  private final TSerializer serializer;

  public ThriftSerializer() {
    try {
      deserializer = new TDeserializer(new TBinaryProtocol.Factory());
      serializer = new TSerializer(new TBinaryProtocol.Factory());
    } catch (TTransportException tte) {
      throw new RuntimeException(tte);
    }
  }
  
  @Override
  public byte[] serialize(T object) throws Exception {
    synchronized (serializer) {
      return serializer.serialize(object);
    }
  }

  @Override
  public T deserialize(T emptyObject, byte[] bytes) throws Exception {
    synchronized (deserializer) {
      deserializer.deserialize(emptyObject, bytes);
    }

    return emptyObject;
  }
}
