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

package com.sematext.spm.client.docs;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sematext.spm.client.ConfigurationFailedException;
import com.sematext.spm.client.config.CollectorFileConfig;
import com.sematext.spm.client.config.MetricConfig;
import com.sematext.spm.client.config.ObservationDefinitionConfig;
import com.sematext.spm.client.yaml.YamlConfigLoader;

/**
 * This class reads the YAML and metaInfo files from sematext-agent-integrations repo dir and generate CSV used to create
 * markdown table for metric definitions in www.sematext.com/docs
 */
public class GenerateMetricsDocs {
  private static final String SEMATEXT_INTEGRATIONS_REPO_DIR = "<sematext-agent-integrations repo dir>";
  private static final String OUT_DIR = "/tmp/md/";

  @Test
  public void generateDocs() throws Exception {

    if (!new File(SEMATEXT_INTEGRATIONS_REPO_DIR).exists()) {
      throw new Exception("Provide a valid path for sematext-agent-integrations repo");
    }

    File[] appDirs = new File(SEMATEXT_INTEGRATIONS_REPO_DIR).listFiles(new FileFilter() {
      @Override public boolean accept(File pathname) {
        return pathname.isDirectory();
      }
    });
    for (File appDir : appDirs) {
      System.out.println("Processing " + appDir);
      File outDir = new File(OUT_DIR);
      if (!outDir.exists()) {
        Assert.assertEquals(outDir.mkdir(), true);
      }
      FileWriter writer = new FileWriter(new File(OUT_DIR, appDir.getName() + ".csv"));
      CSVPrinter printer = CSVFormat.EXCEL
          .withHeader("Metric Name<br> Key *(Type)* *(Unit)*", "Description").print(writer);

      Map<String, MetricConfig> metrics = getMetricConfigsFromYAML(appDir);
      for (Map.Entry<String, MetricConfig> entry : metrics.entrySet()) {
        MetricConfig metric = entry.getValue();
        if (metric.isSend()) {
          String type = metric.getType();
          String mType = getMetricType(type);
          String nType = getNumericType(type);
          if (mType == null || nType == null) {
            System.err.println(String
                                   .format("Null Type for %s , type = %s, mType = %s, nType = %s", metric
                                       .getName(), type, mType, nType));
          } else {
            if (metric.getLabel() == null || metric.getLabel().isEmpty()) {
              System.err.println("Empty label for " + metric.getNamespace() + "." + metric.getName());
            }
            try {
              String key = metric.getNamespace() + "." + metric.getName();
              String kb = metric.getLabel() + "<br>**" + key + "** <br>*(" + nType + " " + mType + ")*";
              if (metric.getUnit() != null && !metric.getUnit().isEmpty()) {
                kb = kb + " *(" + metric.getUnit() + ")*";
              }

              String desc = "";
              if (metric.getDescription() != null) {
                desc = metric.getDescription();
              }

              printer.printRecord(kb, desc);
            } catch (IllegalArgumentException e) {
              System.err.println(e.getMessage() + " : " + "Error while processing " + metric.getName());
            }
          }
        }
      }
      printer.flush();
      printer.close();
    }
  }

  private String getMetricType(String type) {
    String mType = null;
    if (type.contains("counter")) {
      mType = "counter";
    } else if (type.contains("gauge")) {
      mType = "gauge";
    }
    return mType;
  }

  private String getNumericType(String type) {
    String nType = null;
    if (type.equals("counter") ||
        type.equals("long_gauge") ||
        type.equals("gauge")) {
      nType = "long";
    } else if (type.contains("double")) {
      nType = "double";
    }
    return nType;
  }

  private Map<String, MetricConfig> getMetricConfigsFromYAML(File appDir) throws ConfigurationFailedException {
    File[] yamlFiles = appDir.listFiles(new FilenameFilter() {
      @Override public boolean accept(File dir, String name) {
        return name.endsWith("yaml") || name.endsWith("yml");
      }
    });
    Arrays.sort(yamlFiles, new Comparator<File>() {
      @Override public int compare(File o1, File o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    Map<String, MetricConfig> typeInfoMap = new LinkedHashMap<String, MetricConfig>();
    if (yamlFiles.length > 0) {
      for (File yamlFile : yamlFiles) {
        CollectorFileConfig config = YamlConfigLoader.load(yamlFile);
        List<ObservationDefinitionConfig> obs = config.getObservation();
        for (ObservationDefinitionConfig ob : obs) {
          String ns = ob.getMetricNamespace();
          List<MetricConfig> metrics = ob.getMetric();
          for (MetricConfig metricConfig : metrics) {
            typeInfoMap.put(ns + metricConfig.getName(), metricConfig);
          }
        }
      }
    }

    return typeInfoMap;
  }
}
