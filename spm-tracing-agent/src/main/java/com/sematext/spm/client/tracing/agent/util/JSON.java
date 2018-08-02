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
package com.sematext.spm.client.tracing.agent.util;

import java.io.IOException;
import java.io.Writer;

public final class JSON {
  private JSON() {
  }

  /* Simple JSON writer without validation */
  public static class JSONWriter {
    private static final String QUOTE = "\"";
    private final Writer writer;
    private boolean startObject = true;

    public JSONWriter(final Writer to) {
      this.writer = to;
    }

    public void writeObjectStart() {
      begin();
      write("{");
    }

    public void writeKey(String key) {
      begin();
      string(key);
      write(":");
    }

    public void writeInt(int value) {
      write(String.valueOf(value));
      end();
    }

    public void writeLong(long value) {
      write(String.valueOf(value));
      end();
    }

    public void writeString(String value) {
      string(value);
      end();
    }

    public void writeKeyValue(String key, String value) {
      if (value != null) {
        writeKey(key);
        writeString(value);
      }
    }

    public void writeKeyValue(String key, int value) {
      writeKey(key);
      writeInt(value);
    }

    public void writeKeyValue(String key, long value) {
      writeKey(key);
      writeLong(value);
    }

    public void writeArrayStart() {
      begin();
      write("[");
    }

    public void writeArrayEnd() {
      write("]");
      end();
    }

    public void writeObjectEnd() {
      write("}");
      end();
    }

    private void begin() {
      if (!startObject) {
        write(",");
      }
      startObject = true;
    }

    private void end() {
      startObject = false;
    }

    private void string(String v) {
      write(QUOTE + escape(v) + QUOTE);
    }

    private static String escape(String val) {
      if (val == null) {
        return val;
      }
      // TODO replace with something faster - look at Gson's JsonWriter
      return val.replaceAll("\"", "\\\\\"").replaceAll("\n", "\\\\n");
    }

    private void write(String str) {
      try {
        writer.write(str);
      } catch (IOException e) {
        throw new RuntimeException("Can't write.", e);
      }
    }
  }

}
