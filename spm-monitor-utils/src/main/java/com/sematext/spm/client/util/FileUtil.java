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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.sematext.spm.client.Log;
import com.sematext.spm.client.LogFactory;

/**
 * Provides handy methods to work with files
 */
public final class FileUtil {
  private static final Log LOG = LogFactory.getLog(FileUtil.class);

  private FileUtil() {
  }

  public static String readAsString(InputStream is) {
    String configText;
    InputStreamReader sr = null;
    try {
      sr = new InputStreamReader(is);
      char[] buff = new char[32768];
      try {
        int size = sr.read(buff, 0, 32768);
        configText = new String(buff, 0, size);
      } catch (IOException e) { // indicates developer's mistake
        LOG.error(e);
        throw new RuntimeException(e);
      }
    } finally {
      if (sr != null) {
        try {
          sr.close();
        } catch (IOException e) {
          LOG.error(e);
          // DO NOTHING
        }
      }
    }

    return configText;
  }

  /*CHECKSTYLE:OFF*/
  // the following methods were copied from apache commons io FileUtils class
  // -----------------------------------------------------------------------

  /**
   * Reads the contents of a file into a String. The file is always closed.
   *
   * @param file     the file to read, must not be <code>null</code>
   * @param encoding the encoding to use, <code>null</code> means platform default
   * @return the file contents, never <code>null</code>
   * @throws java.io.IOException                  in case of an I/O error
   * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
   */
  public static String readFileToString(File file, String encoding) throws IOException {
    InputStream in = null;
    try {
      in = openInputStream(file);
      return IOUtils.toString(in, encoding);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Reads the contents of a file into a String using the default encoding for the VM. The file is always closed.
   *
   * @param file the file to read, must not be <code>null</code>
   * @return the file contents, never <code>null</code>
   * @throws java.io.IOException in case of an I/O error
   * @since Commons IO 1.3.1
   */
  public static String readFileToString(File file) throws IOException {
    return readFileToString(file, null);
  }

  /**
   * Opens a {@link java.io.FileInputStream} for the specified file, providing better error messages than simply calling
   * <code>new FileInputStream(file)</code>.
   * <p>
   * At the end of the method either the stream will be successfully opened, or an exception will have been thrown.
   * <p>
   * An exception is thrown if the file does not exist. An exception is thrown if the file object exists but is a
   * directory. An exception is thrown if the file exists but cannot be read.
   *
   * @param file the file to open for input, must not be <code>null</code>
   * @return a new {@link java.io.FileInputStream} for the specified file
   * @throws java.io.FileNotFoundException if the file does not exist
   * @throws java.io.IOException           if the file object is a directory
   * @throws java.io.IOException           if the file cannot be read
   * @since Commons IO 1.3
   */
  public static FileInputStream openInputStream(File file) throws IOException {
    if (file.exists()) {
      if (file.isDirectory()) {
        throw new IOException("File '" + file + "' exists but is a directory");
      }
      if (file.canRead() == false) {
        throw new IOException("File '" + file + "' cannot be read");
      }
    } else {
      throw new FileNotFoundException("File '" + file + "' does not exist");
    }
    return new FileInputStream(file);
  }

  public static void writeStringToFile(File file, String data, String encoding) throws IOException {
    OutputStream out = null;
    try {
      out = openOutputStream(file);
      IOUtils.write(data, out, encoding);
    } finally {
      IOUtils.closeQuietly(out);
    }
  }

  public static FileOutputStream openOutputStream(File file) throws IOException {
    if (file.exists()) {
      if (file.isDirectory()) {
        throw new IOException("File '" + file + "' exists but is a directory");
      }
      if (file.canWrite() == false) {
        throw new IOException("File '" + file + "' cannot be written to");
      }
    } else {
      File parent = file.getParentFile();
      if (parent != null && parent.exists() == false) {
        if (parent.mkdirs() == false) {
          throw new IOException("File '" + file + "' could not be created");
        }
      }
    }
    return new FileOutputStream(file);
  }

  /**
   * Writes a String to a file creating the file if it does not exist using the default encoding for the VM.
   *
   * @param file the file to write
   * @param data the content to write to the file
   * @throws java.io.IOException in case of an I/O error
   */
  public static void writeStringToFile(File file, String data) throws IOException {
    writeStringToFile(file, data, null);
  }

  /**
   * Reads the contents of a file line by line to a List of Strings. The file is always closed.
   *
   * @param file     the file to read, must not be <code>null</code>
   * @param encoding the encoding to use, <code>null</code> means platform default
   * @return the list of Strings representing each line in the file, never <code>null</code>
   * @throws java.io.IOException                  in case of an I/O error
   * @throws java.io.UnsupportedEncodingException if the encoding is not supported by the VM
   * @since Commons IO 1.1
   */
  public static List readLines(File file, String encoding) throws IOException {
    InputStream in = null;
    try {
      in = openInputStream(file);
      return IOUtils.readLines(in, encoding);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  public static List readLines(File file) throws IOException {
    return readLines(file, null);
  }

  public static long sizeOfDirectory(File directory) {
    if (!directory.exists()) {
      String message = directory + " does not exist";
      throw new IllegalArgumentException(message);
    }

    if (!directory.isDirectory()) {
      String message = directory + " is not a directory";
      throw new IllegalArgumentException(message);
    }

    long size = 0;

    File[] files = directory.listFiles();
    if (files == null) { // null if security restricted
      return 0L;
    }
    for (int i = 0; i < files.length; i++) {
      File file = files[i];

      if (file.isDirectory()) {
        size += sizeOfDirectory(file);
      } else {
        size += file.length();
      }
    }

    return size;
  }

  public static String path(Iterable<String> segments) {
    final StringBuilder path = new StringBuilder();
    final Iterator<String> iter = segments.iterator();
    while (iter.hasNext()) {
      path.append(iter.next());
      if (iter.hasNext()) {
        path.append(File.separator);
      }
    }
    return path.toString();
  }

  public static String path(String... segments) {
    return path(Arrays.asList(segments));
  }

  /**
   * Does things like:
   * - removing duplicate '/' or '\' characters from the path
   *
   * @param path
   * @return
   */
  public static String normalizePath(String path) {
    if (File.separator.equals("/")) {
      return path.replaceAll(File.separator + "+", File.separator);
    } else if (File.separator.equals("\\")) {
      // windows a bit different regex
      return path.replaceAll("[\\\\]+", "\\\\");
    } else {
      throw new IllegalArgumentException("Unsupported path separator " + File.separator);
    }
  }

  public static void write(String content, File file) throws IOException {
    FileOutputStream os = null;
    try {
      os = new FileOutputStream(file);
      IOUtils.write(content, os);
    } finally {
      IOUtils.closeQuietly(os);
    }
  }

  public static String readFully(File file) throws IOException {
    FileInputStream is = null;
    try {
      is = new FileInputStream(file);
      return IOUtils.toString(is);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  public static void forceMkdirs(File directory) throws IOException {
    if (directory.exists() && !directory.isDirectory()) {
      throw new IOException(directory + " already exists and it is not a directory.");
    } else if (!directory.exists()) {
      if (!directory.mkdirs()) {
        throw new IOException("Can't create directory " + directory + ".");
      }
    }
  }

  public static void forceDelete(File file) throws IOException {
    if (file.isDirectory()) {
      deleteDirectory(file);
    } else {
      boolean filePresent = file.exists();
      if (!file.delete()) {
        if (!filePresent) {
          throw new FileNotFoundException("File does not exist: " + file);
        }
        String message =
            "Unable to delete file: " + file;
        throw new IOException(message);
      }
    }
  }

  public static void deleteDirectory(File directory) throws IOException {
    if (!directory.exists()) {
      return;
    }

    cleanDirectory(directory);
    if (!directory.delete()) {
      String message =
          "Unable to delete directory " + directory + ".";
      throw new IOException(message);
    }
  }

  public static void cleanDirectory(File directory) throws IOException {
    if (!directory.exists()) {
      String message = directory + " does not exist";
      throw new IllegalArgumentException(message);
    }

    if (!directory.isDirectory()) {
      String message = directory + " is not a directory";
      throw new IllegalArgumentException(message);
    }

    File[] files = directory.listFiles();
    if (files == null) {  // null if security restricted
      throw new IOException("Failed to list contents of " + directory);
    }

    IOException exception = null;
    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      try {
        forceDelete(file);
      } catch (IOException ioe) {
        exception = ioe;
      }
    }

    if (null != exception) {
      throw exception;
    }
  }
}
/*CHECKSTYLE:ON*/
