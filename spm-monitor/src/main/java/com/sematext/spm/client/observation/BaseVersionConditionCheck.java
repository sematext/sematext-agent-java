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
package com.sematext.spm.client.observation;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

public abstract class BaseVersionConditionCheck extends BeanConditionCheck {
  private static final Log LOG = LogFactory.getLog(BaseVersionConditionCheck.class);
  private static final Map<String, VersionInfo> PARSED_VERSION_INFOS = new UnifiedMap<String, VersionInfo>();
  private static final long VERSION_CACHE_INTERVAL = 1 * 60 * 1000;

  // caches of read versions where key is className
  private static final Map<String, Long> LAST_VERSION_READ_TIMES = new UnifiedMap<String, Long>();
  private static final Map<String, String> LAST_READ_VERSIONS = new UnifiedMap<String, String>();

  @Override
  protected boolean clauseSatisfies(String readVersionNumber, String expectedValue) {
    // expected version numbers in format of: 7, 7.1.0, 0.14.55, 23.1.16, 0.17-1.33.9, *-1.0, 1.0-*
    // expectedValue will be parsed into "from" and "to" part
    // where all non-number chars will be used as separators for major, minor...

    expectedValue = expectedValue.trim();
    VersionInfo allowedVersion = PARSED_VERSION_INFOS.get(expectedValue);
    if (allowedVersion == null) {
      allowedVersion = new VersionInfo();
      allowedVersion.from = parseVersion(expectedValue.contains("-") ?
                                             expectedValue.substring(0, expectedValue.indexOf("-")) :
                                             expectedValue);
      allowedVersion.to = parseVersion(expectedValue.contains("-") ?
                                           expectedValue.substring(expectedValue.indexOf("-") + 1) :
                                           expectedValue);
      PARSED_VERSION_INFOS.put(expectedValue, allowedVersion);
    }

    List<Long> parsedReadVersionNumber = parseVersion(readVersionNumber);

    if (parsedReadVersionNumber == null) {
      LOG.warn("Version was read as : " + readVersionNumber
                   + ", but since it can't be parsed, version check will be skipped");
      return false;
    } else {
      return versionMatches(parsedReadVersionNumber, allowedVersion);
    }
  }

  private boolean versionMatches(List<Long> parsedReadVersionNumber, VersionInfo allowedVersion) {
    boolean greaterThanFrom = false;
    boolean lowerThanTo = false;

    if (allowedVersion.from == null) {
      greaterThanFrom = true;
    } else {
      for (int i = 0; i < parsedReadVersionNumber.size(); i++) {
        Long ver = parsedReadVersionNumber.get(i);
        Long allowedVer = allowedVersion.from.size() > i ? allowedVersion.from.get(i) : null;

        if (allowedVer == null || ver == null) {
          // anything is ok in case of *
          continue;
        } else {
          if (ver > allowedVer) {
            break;
          } else if (ver == allowedVer) {
            // in case it is the last version part, check if "from" definition contains more, in which case version check should fail
            if (i == parsedReadVersionNumber.size() - 1 && i < allowedVersion.from.size() - 1 &&
                !allRemainingAreNull(allowedVersion.from, i + 1)) {
              return false;
            }
            continue;
          } else {
            return false;
          }
        }
      }
      greaterThanFrom = true;
    }
    if (allowedVersion.to == null) {
      lowerThanTo = true;
    } else {
      for (int i = 0; i < parsedReadVersionNumber.size(); i++) {
        Long ver = parsedReadVersionNumber.get(i);
        Long allowedVer = allowedVersion.to.size() > i ? allowedVersion.to.get(i) : null;

        if (allowedVer == null || ver == null) {
          // anything is ok in case of *
          continue;
        } else {
          if (ver < allowedVer) {
            break;
          } else if (ver == allowedVer) {
            continue;
          } else {
            return false;
          }
        }
      }
      lowerThanTo = true;
    }

    return greaterThanFrom && lowerThanTo;
  }

  private boolean allRemainingAreNull(List<Long> from, int i) {
    for (int j = i; j < from.size(); j++) {
      if (from.get(j) != null) {
        return false;
      }
    }
    return true;
  }

  private List<Long> parseVersion(String version) {
    try {
      version = version != null ? version.trim() : version;
      if (version == null || "".equals(version)) {
        return null;
      }

      List<Long> versionNumbers = new ArrayList<Long>();
      String[] subVersions = version.split("\\.");
      for (String subVersion : subVersions) {
        subVersion = subVersion.trim();
        if (subVersion.equals("")) {
          continue;
        }
        if (subVersion.equals("*")) {
          versionNumbers.add(null);
        } else {
          Long part = null;
          try {
            part = Long.parseLong(subVersion);
          } catch (NumberFormatException nfe) {
            // all values like "v1", "rc4", "beta9" etc... will be considered as null (not part of check)
            part = null;
          }
          versionNumbers.add(part);
        }
      }
      return versionNumbers;
    } catch (Throwable thr) {
      LOG.error("Can't parse version expressions: " + version + " - version check will be skipped", thr);
      return null;
    }
  }

  @Override
  protected final String getConditionValue() {
    synchronized (LAST_READ_VERSIONS) {
      String key = getClass().getName();
      long currentTime = System.currentTimeMillis();
      long lastVersionReadTime = LAST_VERSION_READ_TIMES.containsKey(key) ? LAST_VERSION_READ_TIMES.get(key) : 0l;

      if (lastVersionReadTime + VERSION_CACHE_INTERVAL <= currentTime) {
        LAST_READ_VERSIONS.put(key, readVersion());
        LAST_VERSION_READ_TIMES.put(key, currentTime);
      }

      return LAST_READ_VERSIONS.get(key);
    }
  }

  protected abstract String readVersion();
}

class VersionInfo {
  List<Long> from;
  List<Long> to;
}
