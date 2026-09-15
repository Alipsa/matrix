package spreadsheet

import java.nio.charset.StandardCharsets
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Builds minimal ODS files from hand-written content XML for regression tests. */
class OdsTestUtil {

  static final String MIMETYPE = 'application/vnd.oasis.opendocument.spreadsheet'
  static final String NS = 'xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" ' +
      'xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0" ' +
      'xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" ' +
      'xmlns:dc="http://purl.org/dc/elements/1.1/"'

  /** Wrap table markup in a complete document-content element. */
  static String contentXml(String tablesXml) {
    """<?xml version="1.0" encoding="UTF-8"?>
<office:document-content $NS office:version="1.2">
  <office:body><office:spreadsheet>$tablesXml</office:spreadsheet></office:body>
</office:document-content>"""
  }

  /** Create a temporary ODS containing the mimetype and supplied content XML. */
  static File createOds(String contentXml) {
    File file = File.createTempFile('matrix-test', '.ods')
    file.delete()
    file.deleteOnExit()
    byte[] mime = MIMETYPE.getBytes(StandardCharsets.UTF_8)
    CRC32 crc = new CRC32()
    crc.update(mime)
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
      zos.with {
        ZipEntry entry = new ZipEntry('mimetype')
        entry.method = ZipEntry.STORED
        entry.size = mime.length
        entry.compressedSize = mime.length
        entry.crc = crc.value
        putNextEntry(entry)
        write(mime)
        closeEntry()
        putNextEntry(new ZipEntry('content.xml'))
        write(contentXml.getBytes(StandardCharsets.UTF_8))
        closeEntry()
      }
    }
    file
  }

  /** Read a ZIP entry as UTF-8 text. */
  static String readEntry(File zip, String name) {
    ZipFile z = new ZipFile(zip)
    try {
      return new String(z.getInputStream(z.getEntry(name)).bytes, StandardCharsets.UTF_8)
    } finally {
      z.close()
    }
  }

}
