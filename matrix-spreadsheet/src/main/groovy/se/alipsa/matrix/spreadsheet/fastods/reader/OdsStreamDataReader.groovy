package se.alipsa.matrix.spreadsheet.fastods.reader

import groovy.transform.CompileStatic
import se.alipsa.matrix.spreadsheet.fastods.Sheet

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

import static se.alipsa.matrix.core.ValueConverter.asBigDecimal
import static se.alipsa.matrix.core.ValueConverter.asInteger
import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.*

/**
 * Minimal stream reader that discards styles and only reads the content
 * into a list of rows. It is faster than the event reader so we use it as
 * the default one.
 */
@CompileStatic
final class OdsStreamDataReader extends OdsDataReader {

  static StringBuilder text = new StringBuilder()

  static OdsStreamDataReader create() {
    new OdsStreamDataReader()
  }

  Sheet processContent(final InputStream is, Object sheet, Integer startRow, Integer endRow, Integer startCol, Integer endCol) {
    final XMLInputFactory factory = XMLInputFactory.newInstance()
    Integer sheetCount = 1
    final XMLStreamReader reader = factory.createXMLStreamReader(is)
    try {
      while (reader.hasNext()) {
        reader.next()
        if (reader.isStartElement() && reader.localName == 'table') {
          String sheetName = reader.getAttributeValue(tableUrn, 'name')
          if (sheet == sheetName || sheet == sheetCount) {
            Sheet s = processSheet(reader, startRow, endRow, startCol, endCol)
            s.name = sheet == sheetName ? sheetName : sheetCount.toString()
            return s
          }
          sheetCount++
        }
      }
    } finally {
      reader.close()
    }
    return null
  }

  final static Sheet processSheet(final XMLStreamReader reader, final int startRow, final int endRow, int startColumn, int endColumn) {
    Sheet sheet = new Sheet()
    int rowCount = 1
    while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'table')) {
      if (reader.isStartElement() && reader.localName == 'table-row') {
        int repeatRows = asInteger(reader.getAttributeValue(tableUrn, 'number-rows-repeated') ?: 1)
        for (int i = 0; i < repeatRows; i++) {
          if (rowCount >= startRow && rowCount <= endRow) {
            sheet.add(processRow(reader, startColumn, endColumn))
          }
          rowCount++
        }
      }
      reader.next()
    }
    return sheet
  }

  final static List<Object> processRow(final XMLStreamReader reader, final int startColumn, final int endColumn) {
    List<Object> row = new ArrayList<>()
    int columnCount = 1
    while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'table-row')) {
      if (reader.isStartElement() && reader.localName == 'table-cell') {
        int repeatColumns = asInteger(reader.getAttributeValue(tableUrn, 'number-columns-repeated') ?: 1)
        for (int i = 0; i < repeatColumns; i++) {
          if (columnCount >= startColumn && columnCount <= endColumn) {
            row.add(extractValue(reader))
          }
          columnCount++
        }
      }
      reader.next()
    }
    return row
  }

  final static Object extractValue(final XMLStreamReader reader) {
    Object value = null
    String valueType = reader.getAttributeValue(officeUrn, 'value-type')
    if (valueType != null) {
      String attrValue
      value = switch (valueType) {
        case 'boolean' -> Boolean.parseBoolean(reader.getAttributeValue(officeUrn, 'boolean-value'))
        case 'float', 'percentage', 'currency' -> asBigDecimal(reader.getAttributeValue(officeUrn, 'value'))
        case 'date' -> {
          attrValue = reader.getAttributeValue(officeUrn, 'date-value')
          yield attrValue.length() == 10 ? LocalDate.parse(attrValue) : LocalDateTime.parse(attrValue)
        }
        case 'time' -> Duration.parse(reader.getAttributeValue(officeUrn, 'time-value'))
        default -> {
          text.setLength(0)
          while (reader.hasNext()) {
            reader.next()
            if (reader.isCharacters()) {
              text.append(reader.getText())
            } else if (reader.isStartElement() && reader.localName == 's') {
              int numSpaces = asInteger(reader.getAttributeValue(textUrn, 'c')) ?: 1
              text.append(' '.repeat(numSpaces))
            }
            if (reader.isEndElement() && reader.localName == 'p') {
              break
            }
          }
          yield text.toString()
        }
      }
    }
    return value
  }
}