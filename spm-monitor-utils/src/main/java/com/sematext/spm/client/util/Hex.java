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
package com.sematext.spm.client.util;

public final class Hex {
  private Hex() {
  }

  public static String hexDump(byte[] bytes) {
    StringBuilder builder = new StringBuilder();
    for (byte b : bytes) {
      builder.append(String.format("%02X", b));
    }
    return builder.toString();
  }

  private static byte byteOrd(byte hexByte) {
    char c = (char) hexByte;
    if (c >= 'A' && c <= 'F') {
      return (byte) ((c - 'A') + 10);
    } else if (c >= 'a' && c <= 'f') {
      return (byte) ((c - 'a') + 10);
    } else {
      return (byte) (c - '0');
    }
  }

  public static byte[] fromHex(String hex) {
    byte[] hexBytes = hex.getBytes();
    byte[] bytes = new byte[hex.length() / 2];
    int k = 0;
    for (int i = 0; i < hexBytes.length - 1; i += 2) {
      byte b1 = hexBytes[i], b2 = hexBytes[i + 1];
      bytes[k++] = (byte) (byteOrd(b1) * 16 + byteOrd(b2));
    }
    return bytes;
  }

}
