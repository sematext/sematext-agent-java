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

package com.sematext.spm.client.yaml;

import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;

public class ConfigValidator {

  private String configRoot;
  private int configFileCount = 0;
  private boolean error = false;

  public ConfigValidator(String configRoot) {
    this.configRoot = configRoot;
  }

  public static void main(String[] args) {

    if (args.length == 1) {
      String configRoot = args[0];

      ConfigValidator configValidator = new ConfigValidator(configRoot);
      configValidator.validate();
      if (!configValidator.error()) {
        System.out.println(MessageFormat.format("All {0} config files are valid.",
                                                configValidator.getConfigFileCount()));
        System.exit(0);
      } else {
        System.exit(1);
      }
    } else {
      System.err.println("Please specify the location of config files.");
    }
  }

  public boolean error() {
    return error;
  }

  public void validate() {

    File[] configDirs = new File(configRoot).listFiles();
    if (configDirs != null) {
      for (File configDir : configDirs) {
        //list all ymls in the dir
        File[] configFiles = configDir.listFiles(new FilenameFilter() {
          @Override public boolean accept(File dir, String name) {
            return name.endsWith(".yaml") || name.endsWith(".yml");
          }
        });

        if (configFiles != null) {

          for (File configFile : configFiles) {
            try {
              YamlConfigLoader.load(configFile);
            } catch (Exception e) {
              final String errorMessageFormat = "Error in {0}: {1}";
              System.err.println(MessageFormat.format(errorMessageFormat, configFile, e.getMessage()));
              error = true;
            }
          }
        }
      }
    }
  }

  private int getConfigFileCount() {
    return configFileCount;
  }

}
