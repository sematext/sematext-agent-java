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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

public final class BinarySequentialLog {
  private static final Log LOG = LogFactory.getLog(BinarySequentialLog.class);

  private final String baseDir;
  private final String prefix;
  private final String suffix;
  private final long maxSize;
  private final int retentionCount;
  private int seqNo;
  private DataOutputStream daos;
  private long size;
  private final ArrayDeque<File> files;

  private BinarySequentialLog(String baseDir, String prefix, String suffix, long maxSize, int retentionCount, int seqNo,
                              ArrayDeque<File> files) {
    this.baseDir = baseDir;
    this.prefix = prefix;
    this.suffix = suffix;
    this.maxSize = maxSize;
    this.retentionCount = retentionCount;
    this.seqNo = seqNo;
    this.files = files;
  }

  private static void stderr(String msg) {
    LOG.error(msg);
  }

  private File seqFile() {
    return seqFile(seqNo);
  }

  private File seqFile(int seqNo) {
    return new File(baseDir, prefix + "-" + seqNo + suffix);
  }

  private void cleanup() {
    if (daos == null || size < maxSize) {
      return;
    }

    try {
      daos.close();
    } catch (IOException e) {
      stderr("Can't close file " + seqFile() + ".");
    }

    daos = null;

    while (files.size() > retentionCount) {
      File file = files.pop();
      try {
        if (!file.delete()) {
          stderr("Can't delete file " + file + ".");
        }
      } catch (Exception e) {
        stderr("Can't delete file " + file + ".");
      }
    }

    seqNo++;
  }

  public synchronized void write(byte[] b, int off, int len) {
    cleanup();

    if (daos == null) {
      BufferedOutputStream bos = null;
      try {
        bos = new BufferedOutputStream(new FileOutputStream(seqFile()), 1024);
        files.addLast(seqFile());
      } catch (IOException e) {
        stderr("Can't create file " + seqFile() + ".");
        return;
      }
      daos = new DataOutputStream(bos);

      size = 0;
    }

    try {
      daos.writeInt(len);
      daos.write(b, off, len);

      size += 4;
      size += len;
    } catch (IOException e) {
      stderr("Can't write to log " + seqFile() + ".");
      try {
        daos.close();
      } catch (IOException e1) {
        stderr("Can't close file " + seqFile() + ".");
      }
      daos = null;
      seqNo++;
    }
  }

  public synchronized void write(byte[] b) {
    write(b, 0, b.length);
  }

  // protected for tests
  synchronized void flush() {
    if (daos != null) {
      try {
        daos.flush();
      } catch (IOException e) {
        stderr("Can't flush file.");
      }
    }
  }

  public static BinarySequentialLog create(String baseDir, String prefix, String suffix, long maxSize,
                                           int retentionCount) {
    final List<Integer> seqNumbers = new ArrayList<Integer>();
    final File dir = new File(baseDir);
    if (dir.isDirectory()) {
      for (File file : dir.listFiles()) {
        if (!file.isFile()) {
          continue;
        }
        final String name = file.getName();
        if (name.startsWith(prefix) && name.endsWith(suffix)) {
          final String seqNumber = name.substring(prefix.length() + 1, name.length() - suffix.length());
          int seqNo;
          try {
            seqNo = Integer.parseInt(seqNumber);
          } catch (NumberFormatException e) {
            continue;
          }
          seqNumbers.add(seqNo);
        }
      }
    }
    Collections.sort(seqNumbers);
    final ArrayDeque<File> existing = new ArrayDeque<File>();
    for (Integer seqNo : seqNumbers) {
      existing.addLast(new File(baseDir, prefix + "-" + seqNo + suffix));
    }
    int maxSeqNo = -1;
    if (!seqNumbers.isEmpty()) {
      maxSeqNo = seqNumbers.get(seqNumbers.size() - 1);
    }
    return new BinarySequentialLog(baseDir, prefix, suffix, maxSize, retentionCount, maxSeqNo + 1, existing);
  }

}
