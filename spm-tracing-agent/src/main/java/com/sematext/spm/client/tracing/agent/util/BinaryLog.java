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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

public class BinaryLog {
  private static final Log LOG = LogFactory.getLog(BinaryLog.class);
  private DataOutputStream daos;
  private FileDescriptor fd;
  private final String baseDir;
  private final String fileName;
  private final long maxSize;
  private final int maxBackups;
  private long size;
  private final File file;

  public BinaryLog(String baseDir, String fileName, long maxSize, int maxBackups) {
    this.baseDir = baseDir;
    this.fileName = fileName;
    this.maxSize = maxSize;
    this.maxBackups = maxBackups;
    this.file = new File(baseDir, fileName);
    this.size = file.length();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if (daos != null) {
          try {
            fd.sync();
            daos.close();
          } catch (IOException e) {
            /* */
          }
        }
      }
    });
  }

  private static void stderr(String msg) {
    LOG.error(msg);
  }

  private void maybeRollup() {
    if (daos == null || size <= maxSize) {
      return;
    }

    final File oldestLog = new File(baseDir, fileName + "." + (maxBackups - 1));
    if (oldestLog.exists()) {
      if (!oldestLog.delete()) {
        stderr("Can't delete oldest file: " + oldestLog + ".");
      }
    }

    for (int i = maxBackups - 2; i >= 0; i--) {
      final File rotatedFile = new File(baseDir, fileName + "." + i);
      if (rotatedFile.exists()) {
        int k = i + 1;
        if (!rotatedFile.renameTo(new File(baseDir, fileName + "." + k))) {
          stderr("Can't rotate file: " + rotatedFile + ".");
        }
      }
    }

    try {
      daos.close();
      daos = null;
    } catch (IOException e) {
      stderr("Can't close file " + file + " descriptor.");
    }

    if (!file.renameTo(new File(baseDir, fileName + ".0"))) {
      stderr("Can't rotate file: " + file + ".");
    }
  }

  public synchronized void write(byte[] b, int off, int len) {
    maybeRollup();

    if (daos == null) {
      try {
        final FileOutputStream fos = new FileOutputStream(file);
        fd = fos.getFD();
        daos = new DataOutputStream(new BufferedOutputStream(fos, 1024));
        size = file.length();
      } catch (IOException e) {
        stderr(": Can't create output stream for " + file + ".");
        return;
      }
    }

    try {
      daos.writeInt(len);
      daos.write(b, off, len);
      size += b.length;
    } catch (IOException e) {
      stderr("Can't write to log.");

      if (daos != null) {
        try {
          daos.close();
        } catch (IOException e1) { /* */ }
        daos = null;
      }
    }
  }

  public synchronized void write(byte[] b) {
    write(b, 0, b.length);
  }
}
