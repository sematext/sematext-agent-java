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
package com.sematext.spm.client.util.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.sematext.spm.client.util.FileUtil;
import com.sematext.spm.client.util.IOUtils;

public final class TmpFS {
  private final Set<File> createdFiles = new HashSet<File>();

  private TmpFS() {
  }

  public File createFile(final String content) {
    final File file;
    try {
      file = File.createTempFile("tmppfs", "file");
      createdFiles.add(file);

      OutputStream os = null;
      try {
        os = new FileOutputStream(file);
        IOUtils.write(content, os);
      } finally {
        IOUtils.closeQuietly(os);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Can't create temp file.", e);
    }
    return file;
  }

  public void createFile(File file) {
    try {
      file.createNewFile();
      createdFiles.add(file);
    } catch (IOException e) {
      throw new IllegalStateException("Can't create file.", e);
    }
  }

  public File createFile(File parent, String name) {
    final File file = new File(parent, name);
    try {
      if (!file.createNewFile()) {
        throw new IOException("Not created.");
      }
      createdFiles.add(file);
      return file;
    } catch (IOException e) {
      throw new IllegalStateException("Can't create file.", e);
    }
  }

  public File createFile(File parent, String name, String content) {
    OutputStream os = null;
    try {
      final File file = createFile(parent, name);
      try {
        os = new FileOutputStream(file);
        IOUtils.write(content, os);
        return file;
      } finally {
        IOUtils.closeQuietly(os);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Can't create file.", e);
    }
  }

  public File getTempDirectory() {
    return new File(System.getProperty("java.io.tmpdir"));
  }

  public File createDirectory(File parent, String name) {
    try {
      final File directory = new File(parent, name);
      FileUtil.forceMkdirs(directory);
      createdFiles.add(directory);
      return directory;
    } catch (IOException e) {
      throw new IllegalStateException("Can't create directory.", e);
    }
  }

  public File createDirectory() {
    try {
      final File directory = new File(getTempDirectory(), UUID.randomUUID().toString());
      FileUtil.forceMkdirs(directory);
      createdFiles.add(directory);
      return directory;
    } catch (IOException e) {
      throw new IllegalStateException("Can't create directory.", e);
    }
  }

  public void move(File src, File dst) {
    src.renameTo(dst);
    createdFiles.remove(src);
    createdFiles.add(dst);
  }

  public void cleanup() {
    for (final File file : createdFiles) {
      try {
        FileUtil.forceDelete(file);
      } catch (IOException e) {
        //ignore
      }
    }
    createdFiles.clear();
  }

  public static TmpFS fs() {
    return new TmpFS();
  }
}
