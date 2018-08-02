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
package com.sematext.spm.client.tracing.agent.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

public final class LogChecker {
  private static final Log LOG = LogFactory.getLog(LogChecker.class);

  public static void main(String[] args) throws Exception {
    final String path = args[0];
    final DataInputStream dais = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));

    byte[] lastPayload = null;
    Integer lastLength = null;
    long position = 0;
    try {
      while (true) {
        try {
          lastPayload = null;
          lastLength = dais.readInt();
          position += 4;
          lastPayload = new byte[lastLength];
          int read = dais.read(lastPayload);
          if (read != lastLength) {
            LOG.info("Length = " + lastLength + ", available = " + read + ".");
            throw new EOFException();
          }
          position += read;
          LOG.info("Position = " + position + ", Length = " + lastLength);
          lastLength = null;
        } catch (EOFException e) {
          break;
        }
      }
    } finally {
      dais.close();
    }

    LOG.info(position + ", lastLength = " + lastLength + ", lastPayload = " + lastPayload);
  }
}
