package se.alipsa.matrix.spreadsheet.fastods.reader

import groovy.transform.CompileStatic
import se.alipsa.matrix.spreadsheet.fastods.Sheet
import se.alipsa.matrix.spreadsheet.fastods.Spreadsheet

import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.*
import static se.alipsa.matrix.core.ValueConverter.asBigDecimal
import static se.alipsa.matrix.core.ValueConverter.asInteger

/**
 * Minimal event reader that discards styles and only reads the content
 * into a list of rows.
 */
@CompileStatic
class OdsEventDataReader extends OdsDataReader {

  static StringBuilder text = new StringBuilder()

  static OdsEventDataReader create() {
    new OdsEventDataReader()
  }

  Sheet processContent(InputStream is, Object sheet, Integer startRow, Integer endRow, Integer startCol, Integer endCol) {
    XMLInputFactory factory = XMLInputFactory.newInstance()
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false)

    Integer sheetCount = 1
    XMLEventReader reader = factory.createXMLEventReader(is)
    try {
      while (reader.hasNext()) {
        XMLEvent event = reader.nextEvent()
        if (event.isStartElement()) {
          StartElement startElement = event.asStartElement()
          if (startElement.name.localPart == 'table') {
            String sheetName = startElement.getAttributeByName(tqn('name')).value
            if (sheet == sheetName || sheet == sheetCount) {
              Sheet s = processSheet(reader, startRow, endRow, startCol, endCol)
              s.name = sheet == sheetName ? sheetName : sheetCount.toString()
              return s
            }
            sheetCount++
          }
        }
      }
    } finally {
      reader.close()
    }
    null
  }

  Sheet processSheet(XMLEventReader reader, int startRow, int endRow, int startColumn, int endColumn) {
    Sheet sheet = new Sheet()
    int rowCount = 1
    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent()
      if (event.isStartElement() && event.asStartElement().name.localPart == 'table-row') {
        int repeatRows = asInteger(event.asStartElement().getAttributeByName(tqn('number-rows-repeated'))?.value ?: 1)
        for (int i = 0; i < repeatRows; i++) {
          if (rowCount >= startRow && rowCount <= endRow) {
            sheet.add(processRow(reader, startColumn, endColumn))
          }
          rowCount++
        }
      }
      if (event.isEndElement() && event.asEndElement().name.localPart == 'table') {
        break
      }
    }
    sheet
  }

  List<?> processRow(XMLEventReader reader, int startColumn, int endColumn) {
    List<Object> row = new ArrayList<>()
    int columnCount = 1
    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent()
      if (event.isStartElement() && event.asStartElement().name.localPart == 'table-cell') {
        StartElement cellElement = event.asStartElement()
        int repeatColumns = asInteger(cellElement.getAttributeByName(tqn('number-columns-repeated'))?.value ?: 1)
        for (int i = 0; i < repeatColumns; i++) {
          if (columnCount >= startColumn && columnCount <= endColumn) {
            row.add(extractValue(reader, cellElement))
          }
          columnCount++
        }
      }
      if (event.isEndElement() && event.asEndElement().name.localPart == 'table-row') {
        break
      }
    }
    row
  }

  static Object extractValue(XMLEventReader reader, StartElement cellElement) {
    Object value = null
    String valueType = cellElement.getAttributeByName(oqn('value-type'))?.value
    if (valueType != null) {
      String attrValue
      value = switch (valueType) {
        case 'boolean' -> Boolean.parseBoolean(cellElement.getAttributeByName(oqn('boolean-value'))?.value)
        case 'float', 'percentage', 'currency' -> asBigDecimal(cellElement.getAttributeByName(oqn('value'))?.value)
        case 'date' -> {
          attrValue = cellElement.getAttributeByName(oqn('date-value'))?.value
          yield attrValue.length() == 10 ? LocalDate.parse(attrValue) : LocalDateTime.parse(attrValue)
        }
        case 'time' -> Duration.parse(cellElement.getAttributeByName(oqn('time-value'))?.value)
        default -> {
          text.setLength(0)
          while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent()
            if (event.isCharacters()) {
              text.append(event.asCharacters().data)
            } else if (event.isStartElement()) {
              def textElement = event.asStartElement()
              if (textElement.name.localPart == 's') {
                int numSpaces = asInteger(textElement.getAttributeByName(textQn('c'))?.value) ?: 1
                text.append(' '.repeat(numSpaces))
              }
            }
            if (event.isEndElement() && event.asEndElement().name.localPart == 'p') {
              break
            }
          }
          yield text.toString()
        }
      }
    }
    value
  }
}