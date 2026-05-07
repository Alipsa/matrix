package se.alipsa.matrix.spreadsheet.fastods.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reads the zip content of an ods stream.
 * Based on com.github.miachm.sods.Uncompressor
 */
public class Uncompressor implements Closeable {
  private final ZipInputStream zip;

  public Uncompressor(InputStream in) {
    this.zip = new ZipInputStream(in);
  }

  @Override
  public void close() throws IOException {
    zip.close();
  }

  public InputStream getInputStream() {
    return new UncompressorInputStream(zip);
  }

  public String nextFile() throws IOException {
    ZipEntry entry = zip.getNextEntry();
    if (entry != null)
      return entry.getName();
    else
      return null;
  }
}
