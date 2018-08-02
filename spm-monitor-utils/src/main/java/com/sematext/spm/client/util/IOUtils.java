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
package com.sematext.spm.client.util;

/*CHECKSTYLE:OFF*/
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Copy of Apache commons io IOUtils class.
 */
public class IOUtils {
  // NOTE: This class is focussed on InputStream, OutputStream, Reader and
  // Writer. Each method should take at least one of these as a parameter,
  // or return one of them.

  /**
   * The system line separator string.
   */
  public static final String LINE_SEPARATOR;

  static {
    // avoid security issues
    StringWriter buf = new StringWriter(4);
    PrintWriter out = new PrintWriter(buf);
    out.println();
    LINE_SEPARATOR = buf.toString();
  }

  /**
   * The default buffer size to use.
   */
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

  /**
   * Instances should NOT be constructed in standard programming.
   */
  public IOUtils() {
    super();
  }

  // -----------------------------------------------------------------------

  /**
   * Unconditionally close an <code>InputStream</code>.
   * <p>
   * Equivalent to {@link InputStream#close()}, except any exceptions will be ignored. This is typically used in finally
   * blocks.
   *
   * @param input the InputStream to close, may be null or already closed
   */
  public static void closeQuietly(InputStream input) {
    try {
      if (input != null) {
        input.close();
      }
    } catch (IOException ioe) {
      // ignore
    }
  }

  /**
   * Unconditionally close an <code>OutputStream</code>.
   * <p>
   * Equivalent to {@link OutputStream#close()}, except any exceptions will be ignored. This is typically used in
   * finally blocks.
   *
   * @param output the OutputStream to close, may be null or already closed
   */
  public static void closeQuietly(OutputStream output) {
    try {
      if (output != null) {
        output.close();
      }
    } catch (IOException ioe) {
      // ignore
    }
  }

  /**
   * Get the contents of a <code>String</code> as a <code>byte[]</code> using the default character encoding of the
   * platform.
   * <p>
   * This is the same as {@link String#getBytes()}.
   *
   * @param input the <code>String</code> to convert
   * @return the requested byte array
   * @deprecated Use {@link String#getBytes()}
   */
  public static byte[] toByteArray(String input) {
    return input.getBytes();
  }

  public static byte[] toByteArray(InputStream is) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buff = new byte[256];
    int len;
    while ((len = is.read(buff)) > 0) {
      baos.write(buff, 0, len);
    }
    return baos.toByteArray();
  }

  // read char[]
  // -----------------------------------------------------------------------

  // read toString
  // -----------------------------------------------------------------------

  /**
   * Get the contents of an <code>InputStream</code> as a String using the default character encoding of the platform.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedInputStream</code>.
   *
   * @param input the <code>InputStream</code> to read from
   * @return the requested String
   * @throws NullPointerException if the input is null
   * @throws IOException          if an I/O error occurs
   */
  public static String toString(InputStream input) throws IOException {
    StringWriter sw = new StringWriter();
    copy(input, sw);
    return sw.toString();
  }

  /**
   * Get the contents of an <code>InputStream</code> as a String using the specified character encoding.
   * <p>
   * Character encoding names can be found at <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedInputStream</code>.
   *
   * @param input    the <code>InputStream</code> to read from
   * @param encoding the encoding to use, null means platform default
   * @return the requested String
   * @throws NullPointerException if the input is null
   * @throws IOException          if an I/O error occurs
   */
  public static String toString(InputStream input, String encoding) throws IOException {
    StringWriter sw = new StringWriter();
    copy(input, sw, encoding);
    return sw.toString();
  }

  /**
   * Get the contents of a <code>Reader</code> as a String.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedReader</code>.
   *
   * @param input the <code>Reader</code> to read from
   * @return the requested String
   * @throws NullPointerException if the input is null
   * @throws IOException          if an I/O error occurs
   */
  public static String toString(Reader input) throws IOException {
    StringWriter sw = new StringWriter();
    copy(input, sw);
    return sw.toString();
  }

  /**
   * Get the contents of a <code>byte[]</code> as a String using the default character encoding of the platform.
   *
   * @param input the byte array to read from
   * @return the requested String
   * @throws NullPointerException if the input is null
   * @throws IOException          if an I/O error occurs (never occurs)
   * @deprecated Use {@link String#String(byte[])}
   */
  public static String toString(byte[] input) {
    return new String(input);
  }

  /**
   * Get the contents of a <code>byte[]</code> as a String using the specified character encoding.
   * <p>
   * Character encoding names can be found at <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   *
   * @param input    the byte array to read from
   * @param encoding the encoding to use, null means platform default
   * @return the requested String
   * @throws NullPointerException if the input is null
   * @throws IOException          if an I/O error occurs (never occurs)
   * @deprecated Use {@link String#String(byte[], String)}
   */
  public static String toString(byte[] input, String encoding) throws IOException {
    if (encoding == null) {
      return new String(input);
    } else {
      return new String(input, encoding);
    }
  }

  // readLines
  // -----------------------------------------------------------------------

  /**
   * Get the contents of an <code>InputStream</code> as a list of Strings, one entry per line, using the default
   * character encoding of the platform.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedInputStream</code>.
   *
   * @param input the <code>InputStream</code> to read from, not null
   * @return the list of Strings, never null
   * @throws NullPointerException if the input is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static List readLines(InputStream input) throws IOException {
    InputStreamReader reader = new InputStreamReader(input);
    return readLines(reader);
  }

  /**
   * Get the contents of an <code>InputStream</code> as a list of Strings, one entry per line, using the specified
   * character encoding.
   * <p>
   * Character encoding names can be found at <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedInputStream</code>.
   *
   * @param input    the <code>InputStream</code> to read from, not null
   * @param encoding the encoding to use, null means platform default
   * @return the list of Strings, never null
   * @throws NullPointerException if the input is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static List readLines(InputStream input, String encoding) throws IOException {
    if (encoding == null) {
      return readLines(input);
    } else {
      InputStreamReader reader = new InputStreamReader(input, encoding);
      return readLines(reader);
    }
  }

  /**
   * Get the contents of a <code>Reader</code> as a list of Strings, one entry per line.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedReader</code>.
   *
   * @param input the <code>Reader</code> to read from, not null
   * @return the list of Strings, never null
   * @throws NullPointerException if the input is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static List readLines(Reader input) throws IOException {
    BufferedReader reader = new BufferedReader(input);
    List list = new ArrayList();
    String line = reader.readLine();
    while (line != null) {
      list.add(line);
      line = reader.readLine();
    }
    return list;
  }

  // -----------------------------------------------------------------------

  // write byte[]
  // -----------------------------------------------------------------------

  /**
   * Writes bytes from a <code>byte[]</code> to an <code>OutputStream</code>.
   *
   * @param data   the byte array to write, do not modify during output, null ignored
   * @param output the <code>OutputStream</code> to write to
   * @throws NullPointerException if output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void write(byte[] data, OutputStream output) throws IOException {
    if (data != null) {
      output.write(data);
    }
  }

  /**
   * Writes bytes from a <code>byte[]</code> to chars on a <code>Writer</code> using the default character encoding of
   * the platform.
   * <p>
   * This method uses {@link String#String(byte[])}.
   *
   * @param data   the byte array to write, do not modify during output, null ignored
   * @param output the <code>Writer</code> to write to
   * @throws NullPointerException if output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void write(byte[] data, Writer output) throws IOException {
    if (data != null) {
      output.write(new String(data));
    }
  }

  /**
   * Writes bytes from a <code>byte[]</code> to chars on a <code>Writer</code> using the specified character encoding.
   * <p>
   * Character encoding names can be found at <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method uses {@link String#String(byte[], String)}.
   *
   * @param data     the byte array to write, do not modify during output, null ignored
   * @param output   the <code>Writer</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void write(byte[] data, Writer output, String encoding) throws IOException {
    if (data != null) {
      if (encoding == null) {
        write(data, output);
      } else {
        output.write(new String(data, encoding));
      }
    }
  }

  // write char[]
  // -----------------------------------------------------------------------

  /**
   * Writes chars from a <code>char[]</code> to a <code>Writer</code> using the default character encoding of the
   * platform.
   *
   * @param data   the char array to write, do not modify during output, null ignored
   * @param output the <code>Writer</code> to write to
   * @throws NullPointerException if output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void write(char[] data, Writer output) throws IOException {
    if (data != null) {
      output.write(data);
    }
  }

  /**
   * Writes chars from a <code>char[]</code> to bytes on an <code>OutputStream</code>.
   * <p>
   * This method uses {@link String#String(char[])} and {@link String#getBytes()}.
   *
   * @param data   the char array to write, do not modify during output, null ignored
   * @param output the <code>OutputStream</code> to write to
   * @throws NullPointerException if output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void write(char[] data, OutputStream output) throws IOException {
    if (data != null) {
      output.write(new String(data).getBytes());
    }
  }

  /**
   * Writes chars from a <code>char[]</code> to bytes on an <code>OutputStream</code> using the specified character
   * encoding.
   * <p>
   * Character encoding names can be found at <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method uses {@link String#String(char[])} and {@link String#getBytes(String)}.
   *
   * @param data     the char array to write, do not modify during output, null ignored
   * @param output   the <code>OutputStream</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void write(char[] data, OutputStream output, String encoding) throws IOException {
    if (data != null) {
      if (encoding == null) {
        write(data, output);
      } else {
        output.write(new String(data).getBytes(encoding));
      }
    }
  }

  // write String
  // -----------------------------------------------------------------------

  /**
   * Writes chars from a <code>String</code> to a <code>Writer</code>.
   *
   * @param data   the <code>String</code> to write, null ignored
   * @param output the <code>Writer</code> to write to
   * @throws NullPointerException if output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void write(String data, Writer output) throws IOException {
    if (data != null) {
      output.write(data);
    }
  }

  /**
   * Writes chars from a <code>String</code> to bytes on an <code>OutputStream</code> using the default character
   * encoding of the platform.
   * <p>
   * This method uses {@link String#getBytes()}.
   *
   * @param data   the <code>String</code> to write, null ignored
   * @param output the <code>OutputStream</code> to write to
   * @throws NullPointerException if output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void write(String data, OutputStream output) throws IOException {
    if (data != null) {
      output.write(data.getBytes());
    }
  }

  /**
   * Writes chars from a <code>String</code> to bytes on an <code>OutputStream</code> using the specified character
   * encoding.
   * <p>
   * Character encoding names can be found at <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method uses {@link String#getBytes(String)}.
   *
   * @param data     the <code>String</code> to write, null ignored
   * @param output   the <code>OutputStream</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void write(String data, OutputStream output, String encoding) throws IOException {
    if (data != null) {
      if (encoding == null) {
        write(data, output);
      } else {
        output.write(data.getBytes(encoding));
      }
    }
  }

  // write StringBuffer
  // -----------------------------------------------------------------------

  /**
   * Writes chars from a <code>StringBuffer</code> to a <code>Writer</code>.
   *
   * @param data   the <code>StringBuffer</code> to write, null ignored
   * @param output the <code>Writer</code> to write to
   * @throws NullPointerException if output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void write(StringBuffer data, Writer output) throws IOException {
    if (data != null) {
      output.write(data.toString());
    }
  }

  /**
   * Writes chars from a <code>StringBuffer</code> to bytes on an <code>OutputStream</code> using the default character
   * encoding of the platform.
   * <p>
   * This method uses {@link String#getBytes()}.
   *
   * @param data   the <code>StringBuffer</code> to write, null ignored
   * @param output the <code>OutputStream</code> to write to
   * @throws NullPointerException if output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void write(StringBuffer data, OutputStream output) throws IOException {
    if (data != null) {
      output.write(data.toString().getBytes());
    }
  }

  /**
   * Writes chars from a <code>StringBuffer</code> to bytes on an <code>OutputStream</code> using the specified
   * character encoding.
   * <p>
   * Character encoding names can be found at <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method uses {@link String#getBytes(String)}.
   *
   * @param data     the <code>StringBuffer</code> to write, null ignored
   * @param output   the <code>OutputStream</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void write(StringBuffer data, OutputStream output, String encoding) throws IOException {
    if (data != null) {
      if (encoding == null) {
        write(data, output);
      } else {
        output.write(data.toString().getBytes(encoding));
      }
    }
  }

  // writeLines
  // -----------------------------------------------------------------------

  // copy from InputStream
  // -----------------------------------------------------------------------

  /**
   * Copy bytes from an <code>InputStream</code> to an <code>OutputStream</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedInputStream</code>.
   * <p>
   * Large streams (over 2GB) will return a bytes copied value of <code>-1</code> after the copy has completed since the
   * correct number of bytes cannot be returned as an int. For large streams use the
   * <code>copyLarge(InputStream, OutputStream)</code> method.
   *
   * @param input  the <code>InputStream</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @return the number of bytes copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException          if an I/O error occurs
   * @throws ArithmeticException  if the byte count is too large
   * @since Commons IO 1.1
   */
  public static int copy(InputStream input, OutputStream output) throws IOException {
    long count = copyLarge(input, output);
    if (count > Integer.MAX_VALUE) {
      return -1;
    }
    return (int) count;
  }

  /**
   * Copy bytes from a large (over 2GB) <code>InputStream</code> to an <code>OutputStream</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedInputStream</code>.
   *
   * @param input  the <code>InputStream</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @return the number of bytes copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.3
   */
  public static long copyLarge(InputStream input, OutputStream output) throws IOException {
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Copy bytes from an <code>InputStream</code> to chars on a <code>Writer</code> using the default character encoding
   * of the platform.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedInputStream</code>.
   * <p>
   * This method uses {@link InputStreamReader}.
   *
   * @param input  the <code>InputStream</code> to read from
   * @param output the <code>Writer</code> to write to
   * @throws NullPointerException if the input or output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void copy(InputStream input, Writer output) throws IOException {
    InputStreamReader in = new InputStreamReader(input);
    copy(in, output);
  }

  /**
   * Copy bytes from an <code>InputStream</code> to chars on a <code>Writer</code> using the specified character
   * encoding.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedInputStream</code>.
   * <p>
   * Character encoding names can be found at <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * This method uses {@link InputStreamReader}.
   *
   * @param input    the <code>InputStream</code> to read from
   * @param output   the <code>Writer</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if the input or output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void copy(InputStream input, Writer output, String encoding) throws IOException {
    if (encoding == null) {
      copy(input, output);
    } else {
      InputStreamReader in = new InputStreamReader(input, encoding);
      copy(in, output);
    }
  }

  // copy from Reader
  // -----------------------------------------------------------------------

  /**
   * Copy chars from a <code>Reader</code> to a <code>Writer</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedReader</code>.
   * <p>
   * Large streams (over 2GB) will return a chars copied value of <code>-1</code> after the copy has completed since the
   * correct number of chars cannot be returned as an int. For large streams use the
   * <code>copyLarge(Reader, Writer)</code> method.
   *
   * @param input  the <code>Reader</code> to read from
   * @param output the <code>Writer</code> to write to
   * @return the number of characters copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException          if an I/O error occurs
   * @throws ArithmeticException  if the character count is too large
   * @since Commons IO 1.1
   */
  public static int copy(Reader input, Writer output) throws IOException {
    long count = copyLarge(input, output);
    if (count > Integer.MAX_VALUE) {
      return -1;
    }
    return (int) count;
  }

  /**
   * Copy chars from a large (over 2GB) <code>Reader</code> to a <code>Writer</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedReader</code>.
   *
   * @param input  the <code>Reader</code> to read from
   * @param output the <code>Writer</code> to write to
   * @return the number of characters copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.3
   */
  public static long copyLarge(Reader input, Writer output) throws IOException {
    char[] buffer = new char[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Copy chars from a <code>Reader</code> to bytes on an <code>OutputStream</code> using the default character encoding
   * of the platform, and calling flush.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedReader</code>.
   * <p>
   * Due to the implementation of OutputStreamWriter, this method performs a flush.
   * <p>
   * This method uses {@link OutputStreamWriter}.
   *
   * @param input  the <code>Reader</code> to read from
   * @param output the <code>OutputStream</code> to write to
   * @throws NullPointerException if the input or output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void copy(Reader input, OutputStream output) throws IOException {
    OutputStreamWriter out = new OutputStreamWriter(output);
    copy(input, out);
    // XXX Unless anyone is planning on rewriting OutputStreamWriter, we
    // have to flush here.
    out.flush();
  }

  /**
   * Copy chars from a <code>Reader</code> to bytes on an <code>OutputStream</code> using the specified character
   * encoding, and calling flush.
   * <p>
   * This method buffers the input internally, so there is no need to use a <code>BufferedReader</code>.
   * <p>
   * Character encoding names can be found at <a href="http://www.iana.org/assignments/character-sets">IANA</a>.
   * <p>
   * Due to the implementation of OutputStreamWriter, this method performs a flush.
   * <p>
   * This method uses {@link OutputStreamWriter}.
   *
   * @param input    the <code>Reader</code> to read from
   * @param output   the <code>OutputStream</code> to write to
   * @param encoding the encoding to use, null means platform default
   * @throws NullPointerException if the input or output is null
   * @throws IOException          if an I/O error occurs
   * @since Commons IO 1.1
   */
  public static void copy(Reader input, OutputStream output, String encoding) throws IOException {
    if (encoding == null) {
      copy(input, output);
    } else {
      OutputStreamWriter out = new OutputStreamWriter(output, encoding);
      copy(input, out);
      // XXX Unless anyone is planning on rewriting OutputStreamWriter,
      // we have to flush here.
      out.flush();
    }
  }

  // content equals
  // -----------------------------------------------------------------------

}

/*CHECKSTYLE:ON*/
