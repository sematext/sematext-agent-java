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
package com.sematext.spm.client.snap.serializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;

public final class ZipUtils {
  private static final Logger LOG = LoggerFactory.getLogger(ZipUtils.class);

  private ZipUtils() {
  }

  public static byte[] zip(byte[] raw) {
    //TODO reuse all these streams, store in threadlocal static members
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    DeflaterOutputStream zipStream = new DeflaterOutputStream(bos);
    DataOutputStream s = new DataOutputStream(zipStream);
    try {
      s.write(raw);
    } catch (Exception e) {
      LOG.error("Can't zip bytes", e);
      return null;
    } finally {
      try {
        s.close();
      } catch (IOException e) {
        LOG.error("Can't zip bytes", e);
        return null;
      }
    }
    return bos.toByteArray();
  }

}
