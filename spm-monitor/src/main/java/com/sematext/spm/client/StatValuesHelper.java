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

import java.io.File;
import java.util.Properties;

import com.sematext.spm.client.sender.SenderUtil;
import com.sematext.spm.client.tag.TagUtils;
import com.sematext.spm.client.util.Tuple;

public final class StatValuesHelper {
  private static final Log LOG = LogFactory.getLog(StatValuesHelper.class);

  private StatValuesHelper() {
  }

  public static void fillEnvTags(StatValues statValues, File propsFile) {
    try {
      statValues.getTags().put("os.host", SenderUtil.calculateHostParameterValue());
    } catch (Throwable thr) {
      LOG.warn("Can't resolve os.host value, setting to unknown", thr);
      statValues.getTags().put("os.host", "unknown");
    }

    try {
      String containerHostname = MonitorUtil.getContainerHostname(propsFile, SenderUtil.isInContainer());
      addTag(statValues, "container.hostname", containerHostname);
    } catch (Throwable thr) {
      LOG.warn("Can't resolve container.hostname value, skipping", thr);
    }

    try {
      if (SenderUtil.isInContainer()) {
        addTag(statValues, "container.name", SenderUtil.getContainerName());
        addTag(statValues, "container.id", SenderUtil.getContainerId());
        addTag(statValues, "container.image.name", SenderUtil.getContainerImageName());
        addTag(statValues, "container.image.tag", SenderUtil.getContainerImageTag());
        addTag(statValues, "container.image.digest", SenderUtil.getContainerImageDigest());
      }
    } catch (Throwable thr) {
      LOG.warn("Can't resolve container tags, leaving empty", thr);
    }

    try {
      if (SenderUtil.isInKubernetes()) {
        addTag(statValues, "kubernetes.pod.name", SenderUtil.getK8sPodName());
        addTag(statValues, "kubernetes.namespace", SenderUtil.getK8sNamespace());
        addTag(statValues, "kubernetes.cluster.name", SenderUtil.getK8sCluster());
      }
    } catch (Throwable thr) {
      LOG.warn("Can't resolve kubernetes tags, skipping", thr);
    }
  }

  public static void fillConfigTags(StatValues statValues, Properties monitorProperties) {
    for (Tuple<String, String> tag : TagUtils.getConfigTags(monitorProperties)) {
      addTag(statValues, tag.getFirst(), tag.getSecond());
    }
  }
  
  private static void addTag(StatValues statValues, String name, String value) {
    if (value != null) {
      statValues.getTags().put(name, value);
    }
  }
}
