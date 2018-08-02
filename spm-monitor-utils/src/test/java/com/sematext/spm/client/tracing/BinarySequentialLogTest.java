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

package com.sematext.spm.client.tracing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;

import com.sematext.spm.client.util.test.TmpFS;

public class BinarySequentialLogTest {
  @Test
  public void testShouldCreateLogWithZeroSeqNoIfDirectoryIsEmpty() throws Exception {
    final TmpFS fs = TmpFS.fs();
    try {
      File dir = fs.createDirectory();
      BinarySequentialLog log = BinarySequentialLog.create(dir.getAbsolutePath(), "tracing", ".bin", 128, 1);
      log.write(new byte[] { 1, 2, 3, 4 });
      log.flush();
      assertTrue(new File(dir, "tracing-0.bin").isFile());
    } finally {
      fs.cleanup();
    }
  }

  @Test
  public void testShouldCreateLogWithNextSeqNoIfPreviousLogsExist() throws Exception {
    final TmpFS fs = TmpFS.fs();
    try {
      File dir = fs.createDirectory();
      fs.createFile(dir, "tracing-1.bin");
      BinarySequentialLog log = BinarySequentialLog.create(dir.getAbsolutePath(), "tracing", ".bin", 128, 1);
      log.write(new byte[] { 1, 2, 3, 4 });
      log.flush();
      assertTrue(new File(dir, "tracing-2.bin").isFile());
    } finally {
      fs.cleanup();
    }
  }

  private static final FilenameFilter TRACING_LOG_FILTER = new FilenameFilter() {
    @Override
    public boolean accept(File dir, String name) {
      return name.startsWith("tracing-") && name.endsWith(".bin");
    }
  };

  @Test
  public void testShouldKeepGivenCountOfLogFilesAfterCleanup() throws Exception {
    final TmpFS fs = TmpFS.fs();
    try {
      File dir = fs.createDirectory();
      BinarySequentialLog log = BinarySequentialLog.create(dir.getAbsolutePath(), "tracing", ".bin", 8, 1);
      log.write(new byte[] { 1, 2, 3, 4 }); //tracing-0.bin
      log.write(new byte[] { 1, 2, 3, 4 }); //tracing-1.bin
      log.write(new byte[] { 1, 2, 3, 4 }); //tracing-2.bin

      assertEquals(2, dir.list(TRACING_LOG_FILTER).length);

      assertTrue(new File(dir, "tracing-1.bin").isFile());
      assertTrue(new File(dir, "tracing-2.bin").isFile());
    } finally {
      fs.cleanup();
    }
  }

  @Test
  public void testShoulKeepGivenCountOfLogFilesAfterCleanupWithPreexisting() throws Exception {
    final TmpFS fs = TmpFS.fs();
    try {
      File dir = fs.createDirectory();
      for (int i = 0; i < 5; i++) {
        fs.createFile(dir, "tracing-" + i + ".bin");
      }
      BinarySequentialLog log = BinarySequentialLog.create(dir.getAbsolutePath(), "tracing", ".bin", 16, 1);
      log.write(new byte[] { 1, 2, 3, 4 }); //tracing-5.bin
      log.write(new byte[] { 1, 2, 3, 4 }); //tracing-5.bin
      log.write(new byte[] { 1, 2, 3, 4 }); //tracing-6.bin
      log.write(new byte[] { 1, 2, 3, 4 }); //tracing-6.bin
      log.write(new byte[] { 1, 2, 3, 4 }); //tracing-7.bin
      log.write(new byte[] { 1, 2, 3, 4 }); //tracing-7.bin

      assertEquals(2, dir.list(TRACING_LOG_FILTER).length);

      assertTrue(new File(dir, "tracing-6.bin").isFile());
      assertTrue(new File(dir, "tracing-7.bin").isFile());
    } finally {
      fs.cleanup();
    }
  }
}
