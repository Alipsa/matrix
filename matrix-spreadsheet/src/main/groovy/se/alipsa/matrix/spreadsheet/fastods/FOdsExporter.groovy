package se.alipsa.matrix.spreadsheet.fastods

import groovy.transform.CompileStatic
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

import java.nio.charset.StandardCharsets
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@CompileStatic
class FOdsExporter {

  private static final Logger logger = LogManager.getLogger()
  private static final String MIMETYPE = "application/vnd.oasis.opendocument.spreadsheet"

  static String exportOds(String filePath, Matrix dataFrame) {
    return exportOds(new File(filePath), dataFrame)
  }

  static String exportOds(File file, Matrix dataFrame) {
    String sheetName = SpreadsheetUtil.createValidSheetName(dataFrame.matrixName)
    return exportOdsSheets(file, [dataFrame], [sheetName])[0]
  }

  static String exportOds(File file, Matrix dataFrame, String sheetName) {
    return exportOdsSheets(file, [dataFrame], [sheetName])[0]
  }

  static String exportOds(String filePath, Matrix dataFrame, String sheetName) {
    return exportOdsSheets(new File(filePath), [dataFrame], [sheetName])[0]
  }

  static List<String> exportOdsSheets(String filePath, List<Matrix> data, List<String> sheetNames) {
    return exportOdsSheets(new File(filePath), data, sheetNames)
  }

  static List<String> exportOdsSheets(File file, List<Matrix> data, List<String> sheetNames) {
    if (file.exists() && file.length() > 0) {
      throw new IllegalArgumentException("Appending to an external file is not supported, remove it first")
    }
    if (data.size() != sheetNames.size()) {
      throw new IllegalArgumentException("Matrices and sheet names lists must have the same size")
    }
    List<String> actualSheetNames = sheetNames.collect { SpreadsheetUtil.createValidSheetName(it) }
    String contentXml = buildContentXml(data, actualSheetNames)
    String stylesXml = buildStylesXml()
    String metaXml = buildMetaXml()
    String manifestXml = buildManifestXml()
    writeOds(file, contentXml, stylesXml, metaXml, manifestXml)
    return actualSheetNames
  }

  private static void writeOds(File file, String contentXml, String stylesXml, String metaXml, String manifestXml) {
    logger.info("Writing spreadsheet to {}", file.getAbsolutePath())
    try (FileOutputStream fos = new FileOutputStream(file); ZipOutputStream zos = new ZipOutputStream(fos)) {
      writeMimetype(zos)
      writeEntry(zos, "content.xml", contentXml)
      writeEntry(zos, "styles.xml", stylesXml)
      writeEntry(zos, "meta.xml", metaXml)
      writeEntry(zos, "META-INF/manifest.xml", manifestXml)
    }
  }

  private static void writeMimetype(ZipOutputStream zos) {
    byte[] bytes = MIMETYPE.getBytes(StandardCharsets.UTF_8)
    CRC32 crc = new CRC32()
    crc.update(bytes)
    ZipEntry entry = new ZipEntry("mimetype")
    entry.method = ZipEntry.STORED
    entry.size = bytes.length
    entry.compressedSize = bytes.length
    entry.crc = crc.value
    zos.putNextEntry(entry)
    zos.write(bytes)
    zos.closeEntry()
  }

  private static void writeEntry(ZipOutputStream zos, String name, String content) {
    ZipEntry entry = new ZipEntry(name)
    zos.putNextEntry(entry)
    zos.write(content.getBytes(StandardCharsets.UTF_8))
    zos.closeEntry()
  }

  private static String buildContentXml(List<Matrix> data, List<String> sheetNames) {
    OdsXmlWriter.buildContentXml(data, sheetNames)
  }

  private static String buildStylesXml() {
    return '''<?xml version="1.0" encoding="UTF-8"?>
<office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" office:version="1.2">
  <office:styles/>
  <office:automatic-styles/>
  <office:master-styles/>
</office:document-styles>
'''
  }

  private static String buildMetaXml() {
    return '''<?xml version="1.0" encoding="UTF-8"?>
<office:document-meta xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
  xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0" office:version="1.2">
  <office:meta>
    <meta:generator>matrix-spreadsheet</meta:generator>
  </office:meta>
</office:document-meta>
'''
  }

  private static String buildManifestXml() {
    return '''<?xml version="1.0" encoding="UTF-8"?>
<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2">
  <manifest:file-entry manifest:media-type="application/vnd.oasis.opendocument.spreadsheet" manifest:full-path="/"/>
  <manifest:file-entry manifest:media-type="text/xml" manifest:full-path="content.xml"/>
  <manifest:file-entry manifest:media-type="text/xml" manifest:full-path="styles.xml"/>
  <manifest:file-entry manifest:media-type="text/xml" manifest:full-path="meta.xml"/>
</manifest:manifest>
'''
  }

}
