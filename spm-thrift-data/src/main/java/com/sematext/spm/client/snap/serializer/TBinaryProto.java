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
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TList;
import org.apache.thrift.protocol.TType;
import org.apache.thrift.transport.TIOStreamTransport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class TBinaryProto {
  private TBinaryProto() {
  }

  public static <T extends TBase> void read(T obj, byte[] bytes) {
    read(obj, new ByteArrayInputStream(bytes));
  }

  public static <T extends TBase> void read(T obj, InputStream is) {
    try {
      TBinaryProtocol proto = new TBinaryProtocol(new TIOStreamTransport(is));
      obj.read(proto);
    } catch (TException e) {
      throw new IllegalStateException("Can't read thrift object.", e);
    }
  }

  public static <T extends TBase> List<T> readList(Class<T> klass, byte[] bytes) {
    return readList(klass, new ByteArrayInputStream(bytes));
  }

  public static <T extends TBase> List<T> readList(Class<T> klass, InputStream is) {
    try {
      TBinaryProtocol proto = new TBinaryProtocol(new TIOStreamTransport(is));
      TList thriftList = proto.readListBegin();
      List<T> list = new ArrayList<T>();
      for (int i = 0; i < thriftList.size; i++) {
        T obj = klass.newInstance();
        list.add(obj);
        obj.read(proto);
      }
      proto.readListEnd();
      return list;
    } catch (Exception e) {
      throw new IllegalStateException("Can't read thrift object.", e);
    }
  }

  public static <T extends TBase> byte[] toByteArray(T obj) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      TBinaryProtocol proto = new TBinaryProtocol(new TIOStreamTransport(baos));
      obj.write(proto);
    } catch (TException e) {
      throw new IllegalStateException("Can't write thrift object.", e);
    }
    return baos.toByteArray();
  }

  public static <T extends TBase> byte[] toByteArray(List<T> list) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      TBinaryProtocol proto = new TBinaryProtocol(new TIOStreamTransport(baos));
      proto.writeListBegin(new TList(TType.STRUCT, list.size()));
      for (T obj : list) {
        obj.write(proto);
      }
      proto.writeListEnd();
    } catch (TException e) {
      throw new IllegalStateException("Can't write thrift object.", e);
    }
    return baos.toByteArray();
  }
}
