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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Writes logs to file
 */
public class RollingBytesFileLogger extends BaseRollingFileLogger<byte[]> {
  private BufferedOutputStream os;

  @Override
  protected void writeInternal(byte[] logLine) {
    try {
      os.write(logLine);
      os.flush();
    } catch (IOException e) {
      System.err.println(RollingBytesFileLogger.class + " ERROR: Unable write file stream.");
      e.printStackTrace(System.err);
    }
  }

  @Override
  protected long logLineLength(byte[] logLine) {
    return logLine.length;
  }

  @Override
  protected void logThrowable(Throwable throwable) {
    throwable.printStackTrace(System.err);
  }

  @Override
  protected void closeWritter() {
    if (os != null) {
      try {
        os.close();
      } catch (IOException e) {
        System.err.println(RollingBytesFileLogger.class + " ERROR: Unable to close file stream.");
        e.printStackTrace(System.err);
      }
    }
  }

  @Override
  protected void createWritter(File logFile) throws IOException {
    FileOutputStream fs = new FileOutputStream(logFile);
    os = new BufferedOutputStream(fs);
  }
}
