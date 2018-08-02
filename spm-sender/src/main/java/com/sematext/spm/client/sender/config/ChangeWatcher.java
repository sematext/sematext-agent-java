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
package com.sematext.spm.client.sender.config;

import java.io.File;

public final class ChangeWatcher {

  private ChangeWatcher() {
  }

  public static interface Watch {
    boolean isChanged();
  }

  public static final class FilesChangeWatch implements Watch {
    private final Iterable<File> toWatch;
    private String dependentConfigFilePath;
    private long toWatchLastModified = -1;

    private FilesChangeWatch(Iterable<File> toWatch, String dependentConfigFilePath) {
      this.toWatch = toWatch;
      this.dependentConfigFilePath = dependentConfigFilePath;

      if (dependentConfigFilePath == null) {
        // in this case find "biggest" last modified
        for (final File file : toWatch) {
          if (file.exists()) {
            if (file.lastModified() > toWatchLastModified) {
              toWatchLastModified = file.lastModified();
            }
          }
        }
      }
    }

    @Override
    public boolean isChanged() {
      if (dependentConfigFilePath != null) {
        File tmp = new File(dependentConfigFilePath);
        if (tmp.exists()) {
          for (final File file : toWatch) {
            if (file.exists()) {
              if (file.lastModified() > tmp.lastModified()) {
                return true;
              }
            }
          }
          return false;
        } else {
          return true;
        }
      } else {
        // different logic, just check if any of toWatch files changed since last check
        boolean modified = false;
        for (final File file : toWatch) {
          if (file.exists()) {
            if (file.lastModified() > toWatchLastModified) {
              toWatchLastModified = file.lastModified();
              modified = true;
            }
          }
        }
        return modified;
      }
    }
  }

  /**
   * Watches changes on files. There are two modes:
   * - comparison with dependentConfigFilePath - in this case we compare each file lastModified with dependent file's last modified
   * This is preferred approach, however, sometimes there is no "dependent" file to check, hence:
   * - comparison of files with lastModified timestamp recorded in previous check
   *
   * @param files
   * @param dependentConfigFilePath
   * @return
   */
  public static Watch files(Iterable<File> files, String dependentConfigFilePath) {
    return new FilesChangeWatch(files, dependentConfigFilePath);
  }

  public static Watch or(final Watch a, final Watch b) {
    return new Watch() {
      @Override
      public boolean isChanged() {
        final boolean aChanged = a.isChanged();
        final boolean bChanged = b.isChanged();
        return aChanged || bChanged;
      }
    };
  }

}
