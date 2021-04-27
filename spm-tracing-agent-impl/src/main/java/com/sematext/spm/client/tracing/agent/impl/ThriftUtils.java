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
package com.sematext.spm.client.tracing.agent.impl;

import org.apache.thrift.TDeserializer;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TTransportException;

public final class ThriftUtils {
  private ThriftUtils() {
  }

  private static final ThreadLocal<TSerializer> BINARY_PROTOCOL_SERIALIZER = new ThreadLocal<TSerializer>() {
    @Override
    protected TSerializer initialValue() {
      try {
        return new TSerializer(new TBinaryProtocol.Factory());
      } catch (TTransportException tte) {
        throw new RuntimeException(tte);
      }
    }
  };

  public static TSerializer binaryProtocolSerializer() {
    return BINARY_PROTOCOL_SERIALIZER.get();
  }

  private static final ThreadLocal<TDeserializer> THRIFT_DESERIALIZER = new ThreadLocal<TDeserializer>() {
    @Override
    protected TDeserializer initialValue() {
      try {
        return new TDeserializer(new TBinaryProtocol.Factory());
      } catch (TTransportException tte) {
        throw new RuntimeException(tte);
      }
    }
  };

  public static TDeserializer binaryProtocolDeserializer() {
    return THRIFT_DESERIALIZER.get();
  }

}
