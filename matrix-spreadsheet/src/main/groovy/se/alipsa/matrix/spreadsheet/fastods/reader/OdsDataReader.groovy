package se.alipsa.matrix.spreadsheet.fastods.reader

import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.OPENDOCUMENT_MIMETYPE

import se.alipsa.matrix.spreadsheet.fastods.FastOdsException
import se.alipsa.matrix.spreadsheet.fastods.Sheet

import java.nio.charset.StandardCharsets

/**
 * Base class for reading sheet data from ODS files.
 */
abstract class OdsDataReader {

  /**
   * Create the default ODS data reader implementation.
   *
   * @return a stream-based ODS data reader
   */
  static OdsDataReader create() {
    new OdsStreamDataReader()
  }

  Sheet readOds(InputStream is, Object sheet, Integer startRow, Integer endRow, Integer startCol, Integer endCol) {
    try (Uncompressor unc = new Uncompressor(is)) {
      String entry = unc.nextFile()
      while (entry != null) {
        if (entry == 'content.xml') {
          return processContent(unc.inputStream, sheet, startRow, endRow, startCol, endCol)
        } else if (entry == 'mimetype') {
          checkMimeType(unc)
        }
        entry = unc.nextFile()
      }
    }
    throw new FastOdsException('No content.xml found in the ODS file')
  }

  private static void checkMimeType(Uncompressor uncompressor) throws IOException {
    byte[] expected = OPENDOCUMENT_MIMETYPE.getBytes(StandardCharsets.US_ASCII)
    byte[] buff = uncompressor.getInputStream().readNBytes(expected.length)
    String mimetype = new String(buff, StandardCharsets.US_ASCII)
    if (mimetype != OPENDOCUMENT_MIMETYPE) {
      throw new NotAnOdsException("This file doesn't look like an ODS file. Mimetype: $mimetype")
    }
  }

  abstract Sheet processContent(InputStream is, Object sheet, Integer startRow, Integer endRow, Integer startCol, Integer endCol)

}
