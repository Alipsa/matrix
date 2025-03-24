package se.alipsa.matrix.spreadsheet.fastods.reader

import groovy.transform.CompileStatic
import se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil
import se.alipsa.matrix.spreadsheet.fastods.Sheet
import se.alipsa.matrix.spreadsheet.fastods.Spreadsheet

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

import static se.alipsa.matrix.core.ValueConverter.asBigDecimal
import static se.alipsa.matrix.core.ValueConverter.asInteger
import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.*

/**
 * Minimal reader that discards styles and only reads the content
 * into a list of rows.
 */
@CompileStatic
final class OdsStreamDataReader extends OdsDataReader {

  static OdsStreamDataReader create() {
    new OdsStreamDataReader()
  }

  Spreadsheet processContent(final InputStream is, final Map<Object, List<Integer>> sheets){

    final XMLInputFactory factory = XMLInputFactory.newInstance()

    final Spreadsheet spreadSheet = new Spreadsheet()
    Integer sheetCount = 1
    final XMLStreamReader reader = factory.createXMLStreamReader(is)
    while (reader.hasNext()) {
      reader.next()
      if (reader.isStartElement()) {
        String startElement = reader.localName
        if (startElement == 'table') {

          String sheetName = reader.getAttributeValue(tableUrn, 'name')
          //println "sheetCount: $sheetCount, table: $sheetName: Attributes: ${attributes(startElement)}"
          if (sheets.containsKey(sheetName)) {
            int startRow = sheets[sheetName][0]
            int endRow = sheets[sheetName][1]
            int startColumn = sheets[sheetName][2]
            int endColumn = sheets[sheetName][3]
            //println "reading ${sheetName}, startRow=$startRow, endRow=$endRow, startColumn=$startColumn, endColumn=$endColumn"
            Sheet sheet = processSheet(reader, startRow, endRow, startColumn, endColumn)
            sheet.name = sheetName
            spreadSheet.add(sheetName, sheet)
          } else if(sheets.containsKey(sheetCount)) {
            int startRow = sheets[sheetCount][0]
            int endRow = sheets[sheetCount][1]
            int startColumn = sheets[sheetCount][2]
            int endColumn = sheets[sheetCount][3]
            println "reading ${sheetCount}, startRow=$startRow, endRow=$endRow, startColumn=$startColumn, endColumn=$endColumn"
            Sheet sheet = processSheet(reader, startRow, endRow, startColumn, endColumn)
            sheet.setName(sheetCount)
            spreadSheet.add(sheetCount, sheet)
          }
          sheetCount++
        }
      }
    }
    spreadSheet
  }


  final static Sheet processSheet(final XMLStreamReader reader, final int startRow, final int endRow, int startColumn, int endColumn) {
    Sheet sheet = new Sheet()
    reader.next()
    int rowCount = 1
    while(!(reader.isEndElement() && reader.getLocalName() == 'table')) {
      //println " row $rowCount processSheet: element = ${eventTypeName(event.eventType)}: ${elementName(event)}"
      if (reader.isStartElement() && reader.getLocalName() == 'table-row') {
        //println " table-row: row $rowCount Attributes: ${attributes(event.asStartElement())}"
        int repeatRows = asInteger(reader.getAttributeValue(tableUrn, 'number-rows-repeated') ?: 1)
        for (int i = 0; i < repeatRows; i++) {
          if (rowCount < startRow) {
            reader.next()
            rowCount++
            continue
          } else if (rowCount > endRow) {
            break
          }
          //println " row $rowCount processSheet: process row $rowCount"
          sheet.add(processRow(reader, startColumn, endColumn))
          rowCount++
        }
      }
      reader.next()
    }
    sheet
  }

  final static List<?> processRow(final XMLStreamReader reader, final int startColumn, final int endColumn) {
    List<?> row = new ArrayList<>()
    while (!reader.isStartElement()) {
      reader.next()
    }
    int columnCount = 1
    while(!(reader.isEndElement() && reader.localName == 'table-row')) {
      //println "   col $columnCount processRow: element = ${eventTypeName(event.eventType)}: ${elementName(event)}"
      if (reader.isStartElement() && reader.localName == 'table-cell') {
        //println("   processRow columnCount=$columnCount, startColumn=$startColumn, endColumn=$endColumn")
        //println "   table-cell: column $columnCount Attributes: ${attributes(event.asStartElement())}"
        int repeatColumns = asInteger(reader.getAttributeValue(tableUrn,'number-columns-repeated') ?: 1)
        //println ("   Repeat repeatColumns times")
        for (int i = 0; i < repeatColumns; i++) {
          while (!reader.isStartElement()) {
            reader.next()
          }
          if (columnCount < startColumn) {
            //println "   processRow: skipping column $columnCount, value=${extractValue(reader, cellElement)}"
            columnCount++
            continue
          } else if(columnCount > endColumn) {
            //println "   processRow: breaking at column $columnCount"
            break
          }
          def value = extractValue(reader)
          row.add(value)
          //println("   processRow -> extract value from row $rowCount, column $columnCount: $value")
          columnCount++
        }
      }
      reader.next()
    }
    //println "Returning row: $row"
    row
  }

  final static Object extractValue(final XMLStreamReader reader) {
    Object value = null
    //println "${eventTypeName(reader.getEventType())} : ${attributes(reader)}"
    String valueType = reader.getAttributeValue(officeUrn, 'value-type')
    // using yield just because it makes it slightly easier to see what the return value is
    //println "     extractValue from type $valueType in element ${cellElement.name.localPart}"
    if (valueType != null) {
      String attrValue
      value = switch (valueType) {
        case 'boolean' -> {
          attrValue = reader.getAttributeValue(officeUrn, 'boolean-value')
          yield Boolean.parseBoolean(attrValue)
        }
        case 'float', 'percentage', 'currency' -> {
          attrValue =  reader.getAttributeValue(officeUrn, 'value')
          yield asBigDecimal(attrValue)
        }
        case 'date' -> {
          attrValue =  reader.getAttributeValue(officeUrn, 'date-value')
          if (attrValue.length() == 10) {
            yield LocalDate.parse(attrValue)
          } else {
            yield LocalDateTime.parse(attrValue)
          }
        }
        case 'time' -> {
          attrValue =  reader.getAttributeValue(officeUrn, 'time-value')
          yield Duration.parse(attrValue)
        }
        default -> {
          // extract the text value
          reader.next()
          //println "start text extract, ${eventTypeName(element.eventType)}"
          while (!reader.isStartElement() || 'p' != reader.localName)
            reader.next()

          //println "in p, ${eventTypeName(element.eventType)}, ${element.asStartElement().attributes.collect()}"
          boolean isText = true
          StringBuilder text = new StringBuilder()
          while(isText) {
            if (reader.isCharacters()) {
              //println "assigned value $text"
              String s = reader.getText()
              if (s != null) {
                text.append(s)
              }
            }
            reader.next()
            //println "looking for text in p, ${eventTypeName(element.eventType)}"
            if (reader.isEndElement() &&  'p' == reader.localName) {
              isText = false
            }
          }
          yield text.toString()
        }
      }
    }
    //println "     extractValue returning value $value"
    value
  }
}
