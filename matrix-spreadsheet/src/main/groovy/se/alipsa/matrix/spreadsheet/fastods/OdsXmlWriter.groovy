package se.alipsa.matrix.spreadsheet.fastods

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

import javax.xml.stream.XMLStreamWriter
import javax.xml.stream.XMLOutputFactory
import java.io.StringWriter
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections

import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.officeUrn
import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.tableUrn
import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.textUrn

@CompileStatic
class OdsXmlWriter {

  static String buildContentXml(List<Matrix> data, List<String> sheetNames) {
    return buildContentXml(data, sheetNames, null)
  }

  static String buildContentXml(List<Matrix> data, List<String> sheetNames, List<String> startPositions) {
    XMLOutputFactory factory = XMLOutputFactory.newInstance()
    StringWriter out = new StringWriter()
    XMLStreamWriter writer = factory.createXMLStreamWriter(out)
    writeDocument(writer, data, sheetNames, startPositions)
    writer.close()
    out.toString()
  }

  static void writeDocument(XMLStreamWriter writer, List<Matrix> data, List<String> sheetNames, List<String> startPositions = null) {
    writer.writeStartDocument("UTF-8", "1.0")
    writer.setPrefix("office", officeUrn)
    writer.setPrefix("table", tableUrn)
    writer.setPrefix("text", textUrn)
    writer.writeStartElement("office", "document-content", officeUrn)
    writer.writeNamespace("office", officeUrn)
    writer.writeNamespace("table", tableUrn)
    writer.writeNamespace("text", textUrn)
    writer.writeAttribute("office", officeUrn, "version", "1.2")
    writer.writeStartElement("office", "body", officeUrn)
    writer.writeStartElement("office", "spreadsheet", officeUrn)
    List<String> positions = startPositions ?: Collections.nCopies(data.size(), "A1")
    if (positions.size() != data.size()) {
      throw new IllegalArgumentException("Matrices and start positions lists must have the same size")
    }
    for (int i = 0; i < data.size(); i++) {
      writeTable(writer, data.get(i), sheetNames.get(i), null, positions.get(i))
    }
    writer.writeEndElement() // office:spreadsheet
    writer.writeEndElement() // office:body
    writer.writeEndElement() // office:document-content
    writer.writeEndDocument()
  }

  /**
   * Write a table element for the given matrix using default table attributes.
   *
   * @param writer the XML stream writer
   * @param matrix the matrix to serialize
   * @param sheetName the sheet name to use
   */
  static void writeTable(XMLStreamWriter writer, Matrix matrix, String sheetName) {
    writeTable(writer, matrix, sheetName, null, "A1")
  }

  /**
   * Write a table element for the given matrix, reusing a base template when supplied.
   *
   * @param writer the XML stream writer
   * @param matrix the matrix to serialize
   * @param sheetName the sheet name to use
   * @param template optional table template for styling
   */
  static void writeTable(XMLStreamWriter writer, Matrix matrix, String sheetName, TableTemplate template) {
    writeTable(writer, matrix, sheetName, template, "A1")
  }

  /**
   * Write a table element for the given matrix at a specific start position.
   *
   * @param writer the XML stream writer
   * @param matrix the matrix to serialize
   * @param sheetName the sheet name to use
   * @param template optional table template for styling
   * @param startPosition the top-left cell for the header row (e.g. "B3")
   */
  static void writeTable(XMLStreamWriter writer, Matrix matrix, String sheetName, TableTemplate template, String startPosition) {
    String safeName = SpreadsheetUtil.createValidSheetName(sheetName)
    SpreadsheetUtil.CellPosition position = SpreadsheetUtil.parseCellPosition(startPosition)
    writer.writeStartElement("table", "table", tableUrn)
    writer.writeAttribute("table", tableUrn, "name", safeName)
    if (template?.tableAttributes != null) {
      template.tableAttributes.findAll { !it.isTableNameAttribute() }.each { TableAttribute attr ->
        writer.writeAttribute(attr.prefix ?: "", attr.namespace ?: "", attr.localName, attr.value)
      }
    }
    if (template?.columns != null) {
      writeTableColumns(writer, template.columns)
    }
    writeEmptyRows(writer, position.row - 1)
    writeHeaderRow(writer, matrix.columnNames(), position.column)
    List<Column> columns = matrix.columns()
    int rowCount = matrix.rowCount()
    int colCount = columns.size()
    for (int r = 0; r < rowCount; r++) {
      writer.writeStartElement("table", "table-row", tableUrn)
      writeLeadingEmptyCells(writer, position.column)
      for (int c = 0; c < colCount; c++) {
        Object value = columns.get(c).get(r)
        writeCell(writer, value)
      }
      writer.writeEndElement()
    }
    writer.writeEndElement()
  }

  private static void writeHeaderRow(XMLStreamWriter writer, List<String> names) {
    writer.writeStartElement("table", "table-row", tableUrn)
    writeLeadingEmptyCells(writer, 1)
    names.each { String name ->
      writeStringCell(writer, name)
    }
    writer.writeEndElement()
  }

  private static void writeHeaderRow(XMLStreamWriter writer, List<String> names, int startCol) {
    writer.writeStartElement("table", "table-row", tableUrn)
    writeLeadingEmptyCells(writer, startCol)
    names.each { String name ->
      writeStringCell(writer, name)
    }
    writer.writeEndElement()
  }

  private static void writeEmptyRows(XMLStreamWriter writer, int count) {
    if (count < 1) {
      return
    }
    writer.writeEmptyElement("table", "table-row", tableUrn)
    writer.writeAttribute("table", tableUrn, "number-rows-repeated", String.valueOf(count))
  }

  private static void writeLeadingEmptyCells(XMLStreamWriter writer, int startCol) {
    int count = startCol - 1
    if (count < 1) {
      return
    }
    writer.writeEmptyElement("table", "table-cell", tableUrn)
    writer.writeAttribute("table", tableUrn, "number-columns-repeated", String.valueOf(count))
  }

  private static void writeCell(XMLStreamWriter writer, Object value) {
    if (value == null) {
      writer.writeEmptyElement("table", "table-cell", tableUrn)
      return
    }
    if (value instanceof Boolean) {
      String v = value.toString()
      writer.writeStartElement("table", "table-cell", tableUrn)
      writer.writeAttribute("office", officeUrn, "value-type", "boolean")
      writer.writeAttribute("office", officeUrn, "boolean-value", v)
      writeText(writer, v)
      writer.writeEndElement()
      return
    }
    if (value instanceof Number) {
      String v = ValueConverter.asBigDecimal(value).toPlainString()
      writer.writeStartElement("table", "table-cell", tableUrn)
      writer.writeAttribute("office", officeUrn, "value-type", "float")
      writer.writeAttribute("office", officeUrn, "value", v)
      writeText(writer, v)
      writer.writeEndElement()
      return
    }
    if (value instanceof LocalDate) {
      String v = ((LocalDate) value).format(DateTimeFormatter.ISO_LOCAL_DATE)
      writeDateCell(writer, v)
      return
    }
    if (value instanceof LocalDateTime) {
      String v = ((LocalDateTime) value).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      writeDateCell(writer, v)
      return
    }
    if (value instanceof ZonedDateTime) {
      String v = ((ZonedDateTime) value).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      writeDateCell(writer, v)
      return
    }
    if (value instanceof OffsetDateTime) {
      String v = ((OffsetDateTime) value).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      writeDateCell(writer, v)
      return
    }
    if (value instanceof Date) {
      String v = ((Date) value).toInstant().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      writeDateCell(writer, v)
      return
    }
    if (value instanceof Duration) {
      String v = value.toString()
      writer.writeStartElement("table", "table-cell", tableUrn)
      writer.writeAttribute("office", officeUrn, "value-type", "time")
      writer.writeAttribute("office", officeUrn, "time-value", v)
      writeText(writer, v)
      writer.writeEndElement()
      return
    }
    writeStringCell(writer, String.valueOf(value))
  }

  private static void writeDateCell(XMLStreamWriter writer, String value) {
    writer.writeStartElement("table", "table-cell", tableUrn)
    writer.writeAttribute("office", officeUrn, "value-type", "date")
    writer.writeAttribute("office", officeUrn, "date-value", value)
    writeText(writer, value)
    writer.writeEndElement()
  }

  private static void writeStringCell(XMLStreamWriter writer, String value) {
    writer.writeStartElement("table", "table-cell", tableUrn)
    writer.writeAttribute("office", officeUrn, "value-type", "string")
    writeText(writer, value)
    writer.writeEndElement()
  }

  private static void writeText(XMLStreamWriter writer, String value) {
    writer.writeStartElement("text", "p", textUrn)
    writer.writeCharacters(value == null ? "" : value)
    writer.writeEndElement()
  }

  private static void writeTableColumns(XMLStreamWriter writer, List<TableColumn> columns) {
    columns.each { TableColumn column ->
      writer.writeStartElement("table", "table-column", tableUrn)
      column.attributes?.each { TableAttribute attr ->
        if (attr.namespace) {
          writer.writeAttribute(attr.prefix ?: "", attr.namespace ?: "", attr.localName, attr.value)
        } else {
          writer.writeAttribute(attr.localName, attr.value)
        }
      }
      writer.writeEndElement()
    }
  }

  /**
   * Represents a table attribute captured from an existing ODS table.
   */
  static class TableAttribute {
    final String prefix
    final String namespace
    final String localName
    final String value

    TableAttribute(String prefix, String namespace, String localName, String value) {
      this.prefix = prefix
      this.namespace = namespace
      this.localName = localName
      this.value = value
    }

    boolean isTableNameAttribute() {
      return localName == "name" && namespace == tableUrn
    }
  }

  /**
   * Represents a table column element captured from an existing ODS table.
   */
  static class TableColumn {
    final List<TableAttribute> attributes

    TableColumn(List<TableAttribute> attributes) {
      this.attributes = attributes
    }
  }

  /**
   * Template capturing table-level attributes and column definitions.
   */
  static class TableTemplate {
    final List<TableAttribute> tableAttributes
    final List<TableColumn> columns

    TableTemplate(List<TableAttribute> tableAttributes, List<TableColumn> columns) {
      this.tableAttributes = tableAttributes
      this.columns = columns
    }
  }
}
