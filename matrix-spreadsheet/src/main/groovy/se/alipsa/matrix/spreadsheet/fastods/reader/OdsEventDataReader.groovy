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

        // Parse this physical row ONCE (consumes until </table-row>)
        List<Object> rowValues = processRow(reader, startColumn, endColumn)

        // Replicate logically
        for (int i = 0; i < repeatRows; i++) {
          if (rowCount >= startRow && rowCount <= endRow) {
            sheet.add(new ArrayList<>(rowValues))
          }
          rowCount++
        }
        continue
      }

      if (event.isEndElement() && event.asEndElement().name.localPart == 'table') break
    }
    sheet
  }

  List<Object> processRow(XMLEventReader reader, int startColumn, int endColumn) {
    List<Object> row = new ArrayList<>()
    int columnCount = 1

    // We enter with the last event being <table-row>; consume until </table-row>
    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent()

      if (event.isStartElement()) {
        StartElement se = event.asStartElement()
        String local = se.name.localPart

        if (local == 'table-cell') {
          int repeatColumns = asInteger(se.getAttributeByName(tqn('number-columns-repeated'))?.value ?: 1)

          // Read this physical cell ONCE (this will consume until </table-cell>)
          Object cellValue = readCellValue(reader, se)

          // Replicate logically
          for (int i = 0; i < repeatColumns; i++) {
            if (columnCount >= startColumn && columnCount <= endColumn) row.add(cellValue)
            columnCount++
          }
          continue
        }

        if (local == 'covered-table-cell') {
          int repeatColumns = asInteger(se.getAttributeByName(tqn('number-columns-repeated'))?.value ?: 1)

          // Drain to end of covered cell
          drainUntilEnd(reader, 'covered-table-cell')

          for (int i = 0; i < repeatColumns; i++) {
            if (columnCount >= startColumn && columnCount <= endColumn) row.add(null)
            columnCount++
          }
          continue
        }
      }

      if (event.isEndElement() && event.asEndElement().name.localPart == 'table-row') break
    }
    row
  }

  static Object readCellValue(XMLEventReader reader, StartElement cellElement) {
    String valueType = cellElement.getAttributeByName(oqn('value-type'))?.value

    if (valueType != null) {
      if (valueType == 'boolean') {
        drainUntilEnd(reader, 'table-cell')
        return Boolean.parseBoolean(cellElement.getAttributeByName(oqn('boolean-value'))?.value)
      } else if (valueType in ['float', 'percentage', 'currency']) {
        drainUntilEnd(reader, 'table-cell')
        String v = cellElement.getAttributeByName(oqn('value'))?.value
        return v != null ? asBigDecimal(v) : null
      } else if (valueType == 'date') {
        drainUntilEnd(reader, 'table-cell')
        String v = cellElement.getAttributeByName(oqn('date-value'))?.value
        return (v != null && v.length() == 10) ? LocalDate.parse(v) : (v != null ? LocalDateTime.parse(v) : null)
      } else if (valueType == 'time') {
        drainUntilEnd(reader, 'table-cell')
        String v = cellElement.getAttributeByName(oqn('time-value'))?.value
        return v != null ? Duration.parse(v) : null
      }
    }

    // Text/unknown types: read <text:p> content until </table-cell>
    StringBuilder sb = new StringBuilder()
    while (reader.hasNext()) {
      XMLEvent ev = reader.nextEvent()
      if (ev.isStartElement()) {
        StartElement se = ev.asStartElement()
        String local = se.name.localPart
        if (local == 'p') {
          if (sb.length() > 0) sb.append('\n') // new paragraph => newline between paragraphs
        } else if (local == 's') {
          int numSpaces = asInteger(se.getAttributeByName(textQn('c'))?.value) ?: 1
          sb.append(' '.repeat(numSpaces))
        } else if (local == 'line-break') {
          sb.append('\n')
        } else if (local == 'tab') {
          sb.append('\t')
        }
      } else if (ev.isCharacters()) {
        sb.append(ev.asCharacters().data)
      } else if (ev.isEndElement()) {
        if (ev.asEndElement().name.localPart == 'table-cell') break
      }
    }
    String s = sb.toString()
    return s.isEmpty() ? null : s
  }


  private static void drainUntilEnd(XMLEventReader reader, String localName) {
    int depth = 0
    while (reader.hasNext()) {
      XMLEvent ev = reader.nextEvent()
      if (ev.isStartElement() && ev.asStartElement().name.localPart == localName) depth++
      if (ev.isEndElement() && ev.asEndElement().name.localPart == localName) {
        if (depth == 0) break
        depth--
      }
    }
  }
}