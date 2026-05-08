package se.alipsa.matrix.spreadsheet.fastods

import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.officeUrn
import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.tableUrn
import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.textUrn

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

/**
 * Generates the content.xml markup for ODS files from Matrix data using StAX.
 */
class OdsXmlWriter {

  private static final String NS_OFFICE = 'office'
  private static final String NS_TABLE = 'table'
  private static final String NS_TEXT = 'text'
  private static final String EL_TABLE_CELL = 'table-cell'
  private static final String EL_TABLE_ROW = 'table-row'
  private static final String ATTR_VALUE_TYPE = 'value-type'
  private static final String DEFAULT_START = 'A1'

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

  @SuppressWarnings('UnnecessaryObjectReferences')
  static void writeDocument(XMLStreamWriter writer, List<Matrix> data, List<String> sheetNames, List<String> startPositions = null) {
    writer.writeStartDocument('UTF-8', '1.0')
    writer.setPrefix(NS_OFFICE, officeUrn)
    writer.setPrefix(NS_TABLE, tableUrn)
    writer.setPrefix(NS_TEXT, textUrn)
    writer.writeStartElement(NS_OFFICE, 'document-content', officeUrn)
    writer.writeNamespace(NS_OFFICE, officeUrn)
    writer.writeNamespace(NS_TABLE, tableUrn)
    writer.writeNamespace(NS_TEXT, textUrn)
    writer.writeAttribute(NS_OFFICE, officeUrn, 'version', '1.2')
    writer.writeStartElement(NS_OFFICE, 'body', officeUrn)
    writer.writeStartElement(NS_OFFICE, 'spreadsheet', officeUrn)
    List<String> positions = startPositions ?: Collections.nCopies(data.size(), DEFAULT_START)
    if (positions.size() != data.size()) {
      throw new IllegalArgumentException('Matrices and start positions lists must have the same size')
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
    writeTable(writer, matrix, sheetName, null, DEFAULT_START)
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
    writeTable(writer, matrix, sheetName, template, DEFAULT_START)
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
    writer.writeStartElement(NS_TABLE, NS_TABLE, tableUrn)
    writer.writeAttribute(NS_TABLE, tableUrn, 'name', safeName)
    if (template?.tableAttributes != null) {
      template.tableAttributes.findAll { !it.isTableNameAttribute() }.each { TableAttribute attr ->
        writer.writeAttribute(attr.prefix ?: '', attr.namespace ?: '', attr.localName, attr.value)
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
      writeDataRow(writer, columns, colCount, r, position.column)
    }
    writer.writeEndElement()
  }

  private static void writeHeaderRow(XMLStreamWriter writer, List<String> names) {
    writer.writeStartElement(NS_TABLE, EL_TABLE_ROW, tableUrn)
    writeLeadingEmptyCells(writer, 1)
    names.each { String name ->
      writeStringCell(writer, name)
    }
    writer.writeEndElement()
  }

  private static void writeDataRow(XMLStreamWriter writer, List<Column> columns, int colCount, int rowIndex, int startCol) {
    writer.writeStartElement(NS_TABLE, EL_TABLE_ROW, tableUrn)
    writeLeadingEmptyCells(writer, startCol)
    for (int c = 0; c < colCount; c++) {
      writeCell(writer, columns.get(c).get(rowIndex))
    }
    writer.writeEndElement()
  }

  private static void writeHeaderRow(XMLStreamWriter writer, List<String> names, int startCol) {
    writer.writeStartElement(NS_TABLE, EL_TABLE_ROW, tableUrn)
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
    writer.writeEmptyElement(NS_TABLE, EL_TABLE_ROW, tableUrn)
    writer.writeAttribute(NS_TABLE, tableUrn, 'number-rows-repeated', String.valueOf(count))
  }

  private static void writeLeadingEmptyCells(XMLStreamWriter writer, int startCol) {
    int count = startCol - 1
    if (count < 1) {
      return
    }
    writer.writeEmptyElement(NS_TABLE, EL_TABLE_CELL, tableUrn)
    writer.writeAttribute(NS_TABLE, tableUrn, 'number-columns-repeated', String.valueOf(count))
  }

  private static void writeCell(XMLStreamWriter writer, Object value) {
    if (value == null) {
      writer.writeEmptyElement(NS_TABLE, EL_TABLE_CELL, tableUrn)
      return
    }
    switch (value) {
      case Boolean -> {
        String v = value as String
        writer.writeStartElement(NS_TABLE, EL_TABLE_CELL, tableUrn)
        writer.writeAttribute(NS_OFFICE, officeUrn, ATTR_VALUE_TYPE, 'boolean')
        writer.writeAttribute(NS_OFFICE, officeUrn, 'boolean-value', v)
        writeText(writer, v)
        writer.writeEndElement()
      }
      case Number -> {
        String v = ValueConverter.asBigDecimal(value).toPlainString()
        writer.writeStartElement(NS_TABLE, EL_TABLE_CELL, tableUrn)
        writer.writeAttribute(NS_OFFICE, officeUrn, ATTR_VALUE_TYPE, 'float')
        writer.writeAttribute(NS_OFFICE, officeUrn, 'value', v)
        writeText(writer, v)
        writer.writeEndElement()
      }
      case LocalDate -> writeDateCell(writer, ((LocalDate) value).format(DateTimeFormatter.ISO_LOCAL_DATE))
      case LocalDateTime -> writeDateCell(writer, ((LocalDateTime) value).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
      case ZonedDateTime -> writeDateCell(writer, ((ZonedDateTime) value).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
      case OffsetDateTime -> writeDateCell(writer, ((OffsetDateTime) value).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
      case Date -> writeDateCell(writer, ((Date) value).toInstant().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
      case Duration -> {
        String v = value as String
        writer.writeStartElement(NS_TABLE, EL_TABLE_CELL, tableUrn)
        writer.writeAttribute(NS_OFFICE, officeUrn, ATTR_VALUE_TYPE, 'time')
        writer.writeAttribute(NS_OFFICE, officeUrn, 'time-value', v)
        writeText(writer, v)
        writer.writeEndElement()
      }
      default -> writeStringCell(writer, String.valueOf(value))
    }
  }

  private static void writeDateCell(XMLStreamWriter writer, String value) {
    writer.writeStartElement(NS_TABLE, EL_TABLE_CELL, tableUrn)
    writer.writeAttribute(NS_OFFICE, officeUrn, ATTR_VALUE_TYPE, 'date')
    writer.writeAttribute(NS_OFFICE, officeUrn, 'date-value', value)
    writeText(writer, value)
    writer.writeEndElement()
  }

  private static void writeStringCell(XMLStreamWriter writer, String value) {
    writer.writeStartElement(NS_TABLE, EL_TABLE_CELL, tableUrn)
    writer.writeAttribute(NS_OFFICE, officeUrn, ATTR_VALUE_TYPE, 'string')
    writeText(writer, value)
    writer.writeEndElement()
  }

  private static void writeText(XMLStreamWriter writer, String value) {
    writer.writeStartElement(NS_TEXT, 'p', textUrn)
    writer.writeCharacters(value == null ? '' : value)
    writer.writeEndElement()
  }

  private static void writeTableColumns(XMLStreamWriter writer, List<TableColumn> columns) {
    columns.each { TableColumn column ->
      writer.writeStartElement(NS_TABLE, 'table-column', tableUrn)
      column.attributes?.each { TableAttribute attr ->
        if (attr.namespace) {
          writer.writeAttribute(attr.prefix ?: '', attr.namespace ?: '', attr.localName, attr.value)
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
      return localName == 'name' && namespace == tableUrn
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
