package se.alipsa.matrix.spreadsheet.fastods.reader

import groovy.transform.CompileStatic
import se.alipsa.matrix.spreadsheet.fastods.FastOdsException
import se.alipsa.matrix.spreadsheet.fastods.Sheet

import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.OPENDOCUMENT_MIMETYPE

@CompileStatic
abstract class OdsDataReader {

  enum ReaderImpl {
    EVENT, STREAM
  }
  static OdsDataReader create(ReaderImpl impl = ReaderImpl.STREAM) {
    switch (impl) {
      case ReaderImpl.EVENT -> new OdsEventDataReader()
      case ReaderImpl.STREAM -> new OdsStreamDataReader()
      default -> throw new IllegalArgumentException("Unknown reader implementation")
    }
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