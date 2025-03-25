package se.alipsa.matrix.spreadsheet.fastods.reader;

import groovy.transform.CompileStatic;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

/**
 * Wrapper to the input steam, disallowing the use of close
 * Based on com.github.miachm.sods.UncompressorInputStream
 */
@CompileStatic
class UncompressorInputStream extends InputStream {
  private final ZipInputStream inputStream;

  UncompressorInputStream(ZipInputStream inputStream) {
    this.inputStream = inputStream;
  }

  @Override
  public int read() throws IOException {
    return inputStream.read();
  }

  @Override
  public int read(byte[] b, int offset, int length) throws IOException {
    return inputStream.read(b, offset, length);
  }
}
