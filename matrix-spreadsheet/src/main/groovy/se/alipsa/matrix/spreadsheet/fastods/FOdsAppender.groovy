package se.alipsa.matrix.spreadsheet.fastods

import groovy.transform.CompileStatic
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader
import javax.xml.stream.XMLStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.officeUrn
import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.tableUrn

@CompileStatic
class FOdsAppender {

  private static final Logger logger = LogManager.getLogger()
  private static final String MIMETYPE = "application/vnd.oasis.opendocument.spreadsheet"

  static List<String> appendOrReplaceSheets(File file, List<Matrix> data, List<String> sheetNames) {
    if (!file.exists() || file.length() == 0) {
      return FOdsExporter.exportOdsSheets(file, data, sheetNames)
    }
    if (data.size() != sheetNames.size()) {
      throw new IllegalArgumentException("Matrices and sheet names lists must have the same size")
    }
    Map<String, Matrix> requested = buildRequestedMap(data, sheetNames)
    File tmp = File.createTempFile("matrix-ods", ".ods", file.parentFile)
    try (ZipFile zip = new ZipFile(file); FileOutputStream fos = new FileOutputStream(tmp); ZipOutputStream zos = new ZipOutputStream(fos)) {
      writeMimetype(zip, zos)
      Enumeration<? extends ZipEntry> entries = zip.entries()
      boolean contentWritten = false
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement()
        String name = entry.name
        if (name == "mimetype") {
          continue
        }
        if (name == "content.xml") {
          writeContentXml(zip.getInputStream(entry), zos, requested)
          contentWritten = true
          continue
        }
        copyEntry(zip, entry, zos)
      }
      if (!contentWritten) {
        writeContentXml(null, zos, requested)
      }
    }
    Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    return requested.keySet().toList()
  }

  private static Map<String, Matrix> buildRequestedMap(List<Matrix> data, List<String> sheetNames) {
    Map<String, Matrix> requested = new LinkedHashMap<>()
    for (int i = 0; i < data.size(); i++) {
      String name = SpreadsheetUtil.createValidSheetName(sheetNames.get(i))
      requested.put(name, data.get(i))
    }
    requested
  }

  private static void writeMimetype(ZipFile zip, ZipOutputStream zos) {
    byte[] bytes = readMimetype(zip)
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

  private static byte[] readMimetype(ZipFile zip) {
    ZipEntry entry = zip.getEntry("mimetype")
    if (entry == null) {
      return MIMETYPE.getBytes(StandardCharsets.UTF_8)
    }
    return zip.getInputStream(entry).bytes
  }

  private static void copyEntry(ZipFile zip, ZipEntry entry, ZipOutputStream zos) {
    ZipEntry out = new ZipEntry(entry.name)
    zos.putNextEntry(out)
    zos.write(zip.getInputStream(entry).bytes)
    zos.closeEntry()
  }

  private static void writeContentXml(InputStream input, ZipOutputStream zos, Map<String, Matrix> requested) {
    ZipEntry out = new ZipEntry("content.xml")
    zos.putNextEntry(out)
    if (input == null) {
      String content = OdsXmlWriter.buildContentXml(requested.values().toList(), requested.keySet().toList())
      zos.write(content.getBytes(StandardCharsets.UTF_8))
      zos.closeEntry()
      return
    }
    XMLInputFactory inFactory = XMLInputFactory.newInstance()
    XMLOutputFactory outFactory = XMLOutputFactory.newInstance()
    XMLStreamReader reader = inFactory.createXMLStreamReader(input)
    XMLStreamWriter writer = outFactory.createXMLStreamWriter(zos, "UTF-8")
    writer.writeStartDocument("UTF-8", "1.0")

    Set<String> replaced = new HashSet<>()
    OdsXmlWriter.TableTemplate baseTemplate = null
    boolean capturingBase = false
    boolean capturingColumns = false
    int baseDepth = 0
    List<OdsXmlWriter.TableAttribute> baseAttributes = null
    List<OdsXmlWriter.TableColumn> baseColumns = null
    while (reader.hasNext()) {
      int event = reader.next()
      if (event == XMLStreamConstants.START_ELEMENT && reader.localName == "table") {
        String name = reader.getAttributeValue(tableUrn, "name")
        List<OdsXmlWriter.TableAttribute> tableAttributes = readAttributes(reader)
        if (baseTemplate == null && tableAttributes != null && !tableAttributes.isEmpty()) {
          capturingBase = true
          capturingColumns = true
          baseDepth = 0
          baseAttributes = tableAttributes
          baseColumns = []
        }
        if (name != null && requested.containsKey(name)) {
          OdsXmlWriter.TableTemplate template = readTableTemplateAndSkip(reader, tableAttributes)
          if (baseTemplate == null) {
            baseTemplate = template
          }
          capturingBase = false
          capturingColumns = false
          baseDepth = 0
          OdsXmlWriter.writeTable(writer, requested.get(name), name, template)
          replaced.add(name)
          continue
        }
      }
      if (capturingBase) {
        if (event == XMLStreamConstants.START_ELEMENT && reader.localName == "table") {
          baseDepth++
        } else if (event == XMLStreamConstants.START_ELEMENT && reader.localName == "table-column" && capturingColumns && baseDepth == 1) {
          baseColumns.add(new OdsXmlWriter.TableColumn(readAttributes(reader)))
        } else if (event == XMLStreamConstants.START_ELEMENT && reader.localName == "table-row" && baseDepth == 1) {
          capturingColumns = false
        } else if (event == XMLStreamConstants.END_ELEMENT && reader.localName == "table") {
          baseDepth--
          if (baseDepth == 0) {
            baseTemplate = new OdsXmlWriter.TableTemplate(baseAttributes, baseColumns)
            capturingBase = false
            capturingColumns = false
          }
        }
      }
      if (event == XMLStreamConstants.END_ELEMENT && reader.localName == "spreadsheet" && officeUrn == reader.namespaceURI) {
        requested.each { String name, Matrix matrix ->
          if (!replaced.contains(name)) {
            OdsXmlWriter.writeTable(writer, matrix, name, baseTemplate)
          }
        }
        writer.writeEndElement()
        continue
      }
      copyEvent(reader, writer, event)
    }
    writer.flush()
    writer.close()
    reader.close()
    zos.closeEntry()
  }

  private static void skipElement(XMLStreamReader reader, String elementName) {
    int depth = 1
    while (reader.hasNext() && depth > 0) {
      int event = reader.next()
      if (event == XMLStreamConstants.START_ELEMENT && reader.localName == elementName) {
        depth++
      } else if (event == XMLStreamConstants.END_ELEMENT && reader.localName == elementName) {
        depth--
      }
    }
  }

  private static List<OdsXmlWriter.TableAttribute> readAttributes(XMLStreamReader reader) {
    if (reader.attributeCount == 0) {
      return null
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
      if (event == XMLStreamConstants.START_ELEMENT && reader.localName == "table") {
        depth++
      } else if (event == XMLStreamConstants.START_ELEMENT && reader.localName == "table-column" && capturingColumns && depth == 1) {
        columns.add(new OdsXmlWriter.TableColumn(readAttributes(reader)))
      } else if (event == XMLStreamConstants.START_ELEMENT && reader.localName == "table-row" && depth == 1) {
        capturingColumns = false
      } else if (event == XMLStreamConstants.END_ELEMENT && reader.localName == "table") {
        depth--
      }
    }
    return new OdsXmlWriter.TableTemplate(tableAttributes, columns)
  }

  private static void copyEvent(XMLStreamReader reader, XMLStreamWriter writer, int event) {
    switch (event) {
      case XMLStreamConstants.START_ELEMENT -> {
        String prefix = reader.prefix ?: ""
        String namespace = reader.namespaceURI ?: ""
        writer.writeStartElement(prefix, reader.localName, namespace)
        for (int i = 0; i < reader.namespaceCount; i++) {
          writer.writeNamespace(reader.getNamespacePrefix(i), reader.getNamespaceURI(i))
        }
        for (int i = 0; i < reader.attributeCount; i++) {
          String attrPrefix = reader.getAttributePrefix(i)
          String attrNamespace = reader.getAttributeNamespace(i)
          String attrLocal = reader.getAttributeLocalName(i)
          String attrValue = reader.getAttributeValue(i)
          if (attrNamespace) {
            writer.writeAttribute(attrPrefix ?: "", attrNamespace, attrLocal, attrValue)
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
