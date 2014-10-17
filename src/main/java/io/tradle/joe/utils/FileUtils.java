package io.tradle.joe.utils;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Random;

/**
 * From MultiBit-hd: https://github.com/bitcoin-solutions/multibit-hd/blob/develop/mbhd-brit/src/main/java/org/multibit/hd/brit/utils/FileUtils.java
 * 
 * <p>Utility to provide the following to file system:</p>
 * <ul>
 * <li>Handling temporary files</li>
 * <li>Reading and writing files</li>
 * </ul>
 *
 * @since 0.0.1
 *
 * TODO Consider using Guava and NIO equivalents for these operations
 * Perhaps pull down the file utilities from mbhd-core to replace these
 */
public class FileUtils {

  private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

  private static final int MAX_FILE_SIZE = 1024 * 1024 * 1024; // Do not read files greater than 1 gigabyte.

  /**
   * Utilities have private constructor
   */
  private FileUtils() {
  }

  public static byte[] readFile(File file) throws IOException {
    if (file == null) {
      throw new IllegalArgumentException("File must be provided");
    }

    if (file.length() > MAX_FILE_SIZE) {
      throw new IOException("File '" + file.getAbsolutePath() + "' is too large to input");
    }

    byte[] buffer = new byte[(int) file.length()];
    InputStream ios = null;
    try {
      ios = new FileInputStream(file);
      if (ios.read(buffer) == -1) {
        throw new IOException("EOF reached while trying to read the whole file");
      }
    } finally {
      try {
        if (ios != null) {
          ios.close();
        }
      } catch (IOException e) {
        log.error(e.getClass().getName() + " " + e.getMessage());
      }
    }

    return buffer;
  }

  /**
   * Write a file from the inputstream to the outputstream
   */
  public static void writeFile(InputStream in, OutputStream out)
    throws IOException {
    byte[] buffer = new byte[1024];
    int len;

    while ((len = in.read(buffer)) >= 0)
      out.write(buffer, 0, len);

    in.close();
    out.close();
  }
}
