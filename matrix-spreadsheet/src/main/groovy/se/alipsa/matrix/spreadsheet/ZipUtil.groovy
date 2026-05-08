package se.alipsa.matrix.spreadsheet

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Shared zip helpers for spreadsheet appenders.
 */
final class ZipUtil {

  private ZipUtil() {
    // utility class
  }

  /**
   * Copy a zip entry to the output stream.
   *
   * @param zip the input zip file
   * @param entry the entry to copy
   * @param zos the output zip stream
   */
  static void copyEntry(ZipFile zip, ZipEntry entry, ZipOutputStream zos) {
    ZipEntry out = new ZipEntry(entry.name)
    zos.putNextEntry(out)
    zip.getInputStream(entry).withCloseable { InputStream is ->
      zos.write(is.bytes)
    }
    zos.closeEntry()
  }

}
