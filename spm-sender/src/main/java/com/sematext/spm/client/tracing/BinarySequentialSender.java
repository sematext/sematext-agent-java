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

import org.apache.flume.ChannelException;
import org.apache.flume.Event;
import org.apache.flume.agent.embedded.EmbeddedSource;
import org.apache.flume.event.SimpleEvent;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;
import com.sematext.spm.client.Sender;
import com.sematext.spm.client.Sender.SenderType;
import com.sematext.spm.client.StatsLoggingRegulator;

public class BinarySequentialSender {
  private static final Log LOG = LogFactory.getLog(BinarySequentialSender.class);

  private BinarySequentialLog log;

  public BinarySequentialSender(BinarySequentialLog log) {
    this.log = log;
  }

  public synchronized void write(byte[] b) {
    try {
      EmbeddedSource source = getSource();

      Event newEvent = new SimpleEvent();
      newEvent.setBody(b);
      source.put(newEvent);
    } catch (ChannelException ce) {
      // handling channel errors, like channel-full
      LOG.error("Failed to add stats line to flume channel, skipping writing of remaining lines", ce);
    }

    if (StatsLoggingRegulator.shouldLogStats()) {
      log.write(b);
    }
  }

  private EmbeddedSource getSource() {
    return Sender.getSource(SenderType.TRACING);
  }
}
