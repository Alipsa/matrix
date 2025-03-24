package se.alipsa.matrix.spreadsheet.fastods.reader

import se.alipsa.matrix.spreadsheet.fastods.Spreadsheet

import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.OPENDOCUMENT_MIMETYPE

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

  Spreadsheet readOds(InputStream is, Map<Object, List<Integer>> sheets) {
    Spreadsheet spreadsheet = null
    try (Uncompressor unc = new Uncompressor(is)) {
      String entry = unc.nextFile()
      while (entry != null) {
        if (entry == 'content.xml') {
          spreadsheet = processContent(unc.inputStream,sheets)
          break
        } else if (entry == "mimetype") {
          checkMimeType(unc)
        }
        entry = unc.nextFile()
      }
    }
    spreadsheet
  }

  private static void checkMimeType(Uncompressor uncompressor) throws IOException {
    byte[] buff = new byte[OPENDOCUMENT_MIMETYPE.getBytes().length]
    uncompressor.getInputStream().read(buff)

    String mimetype = new String(buff);
    if (!mimetype.equals(OPENDOCUMENT_MIMETYPE))
      throw new NotAnOdsException("This file doesn't look like an ODS file. Mimetype: " + mimetype);
  }

  abstract Spreadsheet processContent(InputStream is, Map<Object, List<Integer>> sheets);

}