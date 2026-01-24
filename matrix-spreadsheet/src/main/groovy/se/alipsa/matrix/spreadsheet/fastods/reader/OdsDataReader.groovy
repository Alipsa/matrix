package se.alipsa.matrix.spreadsheet.fastods.reader

import groovy.transform.CompileStatic
import se.alipsa.matrix.spreadsheet.fastods.FastOdsException
import se.alipsa.matrix.spreadsheet.fastods.Sheet

import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.OPENDOCUMENT_MIMETYPE

@CompileStatic
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
        } else if (entry == "mimetype") {
          checkMimeType(unc)
        }
        entry = unc.nextFile()
      }
    }
    throw new FastOdsException("No content.xml found in the ODS file")
  }

  private static void checkMimeType(Uncompressor uncompressor) throws IOException {
    byte[] buff = new byte[OPENDOCUMENT_MIMETYPE.getBytes().length]
    uncompressor.getInputStream().read(buff)

    String mimetype = new String(buff);
    if (!mimetype.equals(OPENDOCUMENT_MIMETYPE))
      throw new NotAnOdsException("This file doesn't look like an ODS file. Mimetype: " + mimetype);
  }

  abstract Sheet processContent(InputStream is, Object sheet, Integer startRow, Integer endRow, Integer startCol, Integer endCol);

}
