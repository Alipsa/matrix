package se.alipsa.matrix.spreadsheet.fastods

import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.OFFICE_URN
import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.TABLE_URN

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil
import se.alipsa.matrix.spreadsheet.SpreadsheetWriteUtil
import se.alipsa.matrix.spreadsheet.XmlSecurityUtil
import se.alipsa.matrix.spreadsheet.ZipUtil

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader
import javax.xml.stream.XMLStreamWriter

/**
 * Appends Matrix data to an existing ODS workbook by rewriting its content.xml entry.
 */
class FOdsAppender {

  private static final Logger log = Logger.getLogger(FOdsAppender)
  private static final String MIMETYPE = 'application/vnd.oasis.opendocument.spreadsheet'
  private static final String ENTRY_MIMETYPE = 'mimetype'
  private static final String ENTRY_CONTENT_XML = 'content.xml'
  private static final String DEFAULT_START_POSITION = 'A1'
  private static final String ENCODING_UTF8 = 'UTF-8'
  private static final String EL_TABLE = 'table'
  private static final String EL_TABLE_COLUMN = 'table-column'
  private static final String EL_TABLE_ROW = 'table-row'

  static List<String> appendOrReplaceSheets(File file, List<Matrix> data, List<String> sheetNames) {
    return appendOrReplaceSheets(file, data, sheetNames, null)
  }

  static List<String> appendOrReplaceSheets(File file, List<Matrix> data, List<String> sheetNames, List<String> startPositions) {
    if (!file.exists() || file.length() == 0) {
      return FOdsExporter.exportOdsSheets(file, data, sheetNames, startPositions)
    }
    if (data.size() != sheetNames.size()) {
      throw new IllegalArgumentException('Matrices and sheet names lists must have the same size')
    }
    Map<String, Matrix> requested = SpreadsheetWriteUtil.buildRequestedMap(data, sheetNames)
    Map<String, String> positions = buildPositionMap(sheetNames, startPositions)
    File tmp = File.createTempFile('matrix-ods', '.ods', file.parentFile)
    boolean moved = false
    try {
      try (ZipFile zip = new ZipFile(file); FileOutputStream fos = new FileOutputStream(tmp); ZipOutputStream zos = new ZipOutputStream(fos)) {
        writeMimetype(zip, zos)
        copyEntriesReplacingContentXml(zip, zos, requested, positions)
      }
      Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
      moved = true
      return requested.keySet().toList()
    } finally {
      if (!moved && tmp.exists()) {
        tmp.delete()
      }
    }
  }

  private static void copyEntriesReplacingContentXml(ZipFile zip, ZipOutputStream zos, Map<String, Matrix> requested, Map<String, String> positions) {
    Enumeration<? extends ZipEntry> entries = zip.entries()
    boolean contentWritten = false
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement()
      String name = entry.name
      if (name == ENTRY_MIMETYPE) {
        continue
      }
      if (name == ENTRY_CONTENT_XML) {
        writeContentXml(zip.getInputStream(entry), zos, requested, positions)
        contentWritten = true
        continue
      }
      ZipUtil.copyEntry(zip, entry, zos)
    }
    if (!contentWritten) {
      writeContentXml(null, zos, requested, positions)
    }
  }

  private static Map<String, String> buildPositionMap(List<String> sheetNames, List<String> startPositions) {
    List<String> positions = startPositions ?: Collections.nCopies(sheetNames.size(), DEFAULT_START_POSITION)
    if (sheetNames.size() != positions.size()) {
      throw new IllegalArgumentException('Sheet names and start positions lists must have the same size')
    }
    List<String> uniqueNames = SpreadsheetUtil.createUniqueSheetNames(sheetNames)
    Map<String, String> result = [:]
    for (int i = 0; i < uniqueNames.size(); i++) {
      result.put(uniqueNames.get(i), positions.get(i) ?: DEFAULT_START_POSITION)
    }
    result
  }

  private static void writeMimetype(ZipFile zip, ZipOutputStream zos) {
    byte[] bytes = readMimetype(zip)
    CRC32 crc = new CRC32()
    crc.update(bytes)
    ZipEntry entry = new ZipEntry(ENTRY_MIMETYPE)
    entry.method = ZipEntry.STORED
    entry.size = bytes.length
    entry.compressedSize = bytes.length
    entry.crc = crc.value
    zos.putNextEntry(entry)
    zos.write(bytes)
    zos.closeEntry()
  }

  private static byte[] readMimetype(ZipFile zip) {
    ZipEntry entry = zip.getEntry(ENTRY_MIMETYPE)
    if (entry == null) {
      return MIMETYPE.getBytes(StandardCharsets.UTF_8)
    }
    byte[] bytes = zip.getInputStream(entry).withCloseable { InputStream is -> is.bytes }
    String mime = new String(bytes, StandardCharsets.UTF_8).trim()
    if (mime != MIMETYPE) {
      log.warn("Unexpected ODS mimetype '$mime', rewriting as '$MIMETYPE'.")
      return MIMETYPE.getBytes(StandardCharsets.UTF_8)
    }
    return bytes
  }

  private static void writeContentXml(InputStream input, ZipOutputStream zos, Map<String, Matrix> requested, Map<String, String> positions) {
    ZipEntry out = new ZipEntry(ENTRY_CONTENT_XML)
    zos.putNextEntry(out)
    if (input == null) {
      writeGeneratedContentXml(zos, requested, positions)
      return
    }
    rewriteContentXml(input, zos, requested, positions)
  }

  private static void writeGeneratedContentXml(ZipOutputStream zos, Map<String, Matrix> requested, Map<String, String> positions) {
    List<String> names = requested.keySet().toList()
    List<String> startPositions = names.collect { positions.get(it) ?: DEFAULT_START_POSITION }
    String content = OdsXmlWriter.buildContentXml(requested.values().toList(), names, startPositions)
    zos.write(content.getBytes(StandardCharsets.UTF_8))
    zos.closeEntry()
  }

  private static void rewriteContentXml(InputStream input, ZipOutputStream zos, Map<String, Matrix> requested, Map<String, String> positions) {
    XMLInputFactory inFactory = XmlSecurityUtil.newSecureInputFactory()
    XMLOutputFactory outFactory = XMLOutputFactory.newInstance()
    XMLStreamReader reader = null
    XMLStreamWriter writer = null
    try {
      reader = inFactory.createXMLStreamReader(input)
      writer = outFactory.createXMLStreamWriter(zos, ENCODING_UTF8)
      writer.writeStartDocument(ENCODING_UTF8, '1.0')
      copyAndReplaceTables(reader, writer, requested, positions)
    } finally {
      if (writer != null) {
        writer.flush()
        writer.close()
      }
      if (reader != null) {
        reader.close()
      }
      zos.closeEntry()
    }
  }

  private static void copyAndReplaceTables(XMLStreamReader reader, XMLStreamWriter writer, Map<String, Matrix> requested, Map<String, String> positions) {
    Set<String> replaced = [] as Set
    BaseTemplateCapture capture = new BaseTemplateCapture()
    while (reader.hasNext()) {
      int event = reader.next()
      if (event == XMLStreamConstants.START_ELEMENT && reader.localName == EL_TABLE
          && handleTableStart(reader, writer, requested, positions, replaced, capture)) {
        continue
      }
      capture.trackEvent(reader, event)
      if (event == XMLStreamConstants.END_ELEMENT && reader.localName == 'spreadsheet' && OFFICE_URN == reader.namespaceURI) {
        writeRemainingTables(writer, requested, positions, replaced, capture.template)
        writer.writeEndElement()
        continue
      }
      copyEvent(reader, writer, event)
    }
  }

  private static boolean handleTableStart(XMLStreamReader reader, XMLStreamWriter writer, Map<String, Matrix> requested,
                                           Map<String, String> positions, Set<String> replaced, BaseTemplateCapture capture) {
    String name = reader.getAttributeValue(TABLE_URN, 'name')
    List<OdsXmlWriter.TableAttribute> tableAttributes = readAttributes(reader)
    capture.maybeStartCapturing(tableAttributes)
    if (name == null || !requested.containsKey(name)) {
      return false
    }
    OdsXmlWriter.TableTemplate template = readTableTemplateAndSkip(reader, tableAttributes)
    capture.useAsTemplateIfAbsent(template)
    capture.reset()
    String startPosition = positions.get(name) ?: DEFAULT_START_POSITION
    OdsXmlWriter.writeTable(writer, requested.get(name), name, template, startPosition)
    replaced.add(name)
    true
  }

  private static void writeRemainingTables(XMLStreamWriter writer, Map<String, Matrix> requested, Map<String, String> positions,
                                            Set<String> replaced, OdsXmlWriter.TableTemplate baseTemplate) {
    requested.each { String name, Matrix matrix ->
      if (!replaced.contains(name)) {
        String startPosition = positions.get(name) ?: DEFAULT_START_POSITION
        OdsXmlWriter.writeTable(writer, matrix, name, baseTemplate, startPosition)
      }
    }
  }

  /**
   * Tracks the first table with attributes seen while copying content.xml, so its
   * structure (attributes + column definitions) can be reused as a template for
   * requested sheets that don't already exist in the source document.
   */
  private static class BaseTemplateCapture {
    OdsXmlWriter.TableTemplate template
    boolean capturingBase = false
    boolean capturingColumns = false
    int baseDepth = 0
    List<OdsXmlWriter.TableAttribute> baseAttributes
    List<OdsXmlWriter.TableColumn> baseColumns

    void maybeStartCapturing(List<OdsXmlWriter.TableAttribute> tableAttributes) {
      if (template == null && tableAttributes != null && !tableAttributes.isEmpty()) {
        capturingBase = true
        capturingColumns = true
        baseDepth = 0
        baseAttributes = tableAttributes
        baseColumns = []
      }
    }

    void useAsTemplateIfAbsent(OdsXmlWriter.TableTemplate candidate) {
      if (template == null) {
        template = candidate
      }
    }

    void reset() {
      capturingBase = false
      capturingColumns = false
      baseDepth = 0
    }

    void trackEvent(XMLStreamReader reader, int event) {
      if (!capturingBase) {
        return
      }
      if (event == XMLStreamConstants.START_ELEMENT) {
        if (reader.localName == EL_TABLE) {
          baseDepth++
        } else if (reader.localName == EL_TABLE_COLUMN && capturingColumns && baseDepth == 1) {
          baseColumns.add(new OdsXmlWriter.TableColumn(readAttributes(reader)))
        } else if (reader.localName == EL_TABLE_ROW && baseDepth == 1) {
          capturingColumns = false
        }
      } else if (event == XMLStreamConstants.END_ELEMENT && reader.localName == EL_TABLE) {
        baseDepth--
        if (baseDepth == 0) {
          template = new OdsXmlWriter.TableTemplate(baseAttributes, baseColumns)
          capturingBase = false
          capturingColumns = false
        }
      }
    }
  }

  private static List<OdsXmlWriter.TableAttribute> readAttributes(XMLStreamReader reader) {
    if (reader.attributeCount == 0) {
      return []
    }
    List<OdsXmlWriter.TableAttribute> attributes = []
    for (int i = 0; i < reader.attributeCount; i++) {
      String localName = reader.getAttributeLocalName(i)
      String value = reader.getAttributeValue(i)
      String namespace = reader.getAttributeNamespace(i)
      String prefix = reader.getAttributePrefix(i)
      attributes.add(new OdsXmlWriter.TableAttribute(prefix, namespace, localName, value))
    }
    return attributes
  }

  private static OdsXmlWriter.TableTemplate readTableTemplateAndSkip(XMLStreamReader reader, List<OdsXmlWriter.TableAttribute> tableAttributes) {
    List<OdsXmlWriter.TableColumn> columns = []
    boolean capturingColumns = true
    int depth = 1
    while (reader.hasNext() && depth > 0) {
      int event = reader.next()
      if (event == XMLStreamConstants.START_ELEMENT && reader.localName == EL_TABLE) {
        depth++
      } else if (event == XMLStreamConstants.START_ELEMENT && reader.localName == EL_TABLE_COLUMN && capturingColumns && depth == 1) {
        columns.add(new OdsXmlWriter.TableColumn(readAttributes(reader)))
      } else if (event == XMLStreamConstants.START_ELEMENT && reader.localName == EL_TABLE_ROW && depth == 1) {
        capturingColumns = false
      } else if (event == XMLStreamConstants.END_ELEMENT && reader.localName == EL_TABLE) {
        depth--
      }
    }
    return new OdsXmlWriter.TableTemplate(tableAttributes, columns)
  }

  private static void copyEvent(XMLStreamReader reader, XMLStreamWriter writer, int event) {
    switch (event) {
      case XMLStreamConstants.START_ELEMENT -> {
        String prefix = reader.prefix ?: ''
        String namespace = reader.namespaceURI ?: ''
        writer.writeStartElement(prefix ?: '', reader.localName, namespace ?: '')
        for (int i = 0; i < reader.namespaceCount; i++) {
          String nsPrefix = reader.getNamespacePrefix(i) ?: ''
          String nsUri = reader.getNamespaceURI(i) ?: ''
          writer.writeNamespace(nsPrefix, nsUri)
        }
        for (int i = 0; i < reader.attributeCount; i++) {
          String attrPrefix = reader.getAttributePrefix(i)
          String attrNamespace = reader.getAttributeNamespace(i)
          String attrLocal = reader.getAttributeLocalName(i)
          String attrValue = reader.getAttributeValue(i)
          if (attrNamespace) {
            writer.writeAttribute(attrPrefix ?: '', attrNamespace, attrLocal, attrValue)
          } else {
            writer.writeAttribute(attrLocal, attrValue)
          }
        }
      }
      case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> writer.writeCharacters(reader.text)
      case XMLStreamConstants.END_ELEMENT -> writer.writeEndElement()
      case XMLStreamConstants.PROCESSING_INSTRUCTION -> {
        String target = reader.getPITarget()
        String data = reader.getPIData()
        writer.writeProcessingInstruction(target, data)
      }
      case XMLStreamConstants.COMMENT -> writer.writeComment(reader.text)
      default -> {}
    }
  }

}
