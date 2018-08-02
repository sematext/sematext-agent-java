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

import org.apache.flume.ChannelException;
import org.apache.flume.Event;
import org.apache.flume.agent.embedded.EmbeddedSource;
import org.apache.flume.event.SimpleEvent;

import java.util.List;

import com.sematext.spm.client.Sender.SenderType;

public class TagsSender {
  private static final Log LOG = LogFactory.getLog(TagsSender.class);

  private Serializer serializer;

  public TagsSender() {
    serializer = Serializer.INFLUX;
  }

  public void sendTags(List<StatValues> tagsStats) {
    for (StatValues sv : tagsStats) {
      // timestamp send as -1 to force its omission
      String tagEvent = serializer.serialize(sv.getMetricNamespace(), sv.getAppToken(), sv.getMetrics(),
                                             sv.getTags(), -1);
      sendTagEvent(tagEvent);
    }
  }

  private void sendTagEvent(String tagEvent) {
    try {
      EmbeddedSource source = getSource();
      if (source == null) {
        LOG.warn("Tags source is still null, can't write metrics tags");
        return;
      }
      Event newEvent = new SimpleEvent();
      newEvent.setBody(tagEvent.getBytes());
      source.put(newEvent);
    } catch (ChannelException ce) {
      // handling channel errors, like channel-full
      // in this case we will stop further writing to the channel
      LOG.error("Failed to add metrics tags to flume channel", ce);
    } catch (Throwable thr) {
      LOG.error("Exception while processing metrics tags", thr);
    }
  }

  private EmbeddedSource getSource() {
    EmbeddedSource source = Sender.getSource(SenderType.TAGS);
    return source;
  }
}
