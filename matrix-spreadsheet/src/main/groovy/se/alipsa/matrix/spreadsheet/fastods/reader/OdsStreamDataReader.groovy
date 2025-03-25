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
 * into a list of rows. In theory this should be more memory efficient then
 * the event reader but the navigation is a bit trickier so needs more testing
 * to see if it warrants switching to this as the default reader.
 */
@CompileStatic
final class OdsStreamDataReader extends OdsDataReader {

  static StringBuilder text = new StringBuilder()

  static OdsStreamDataReader create() {
    new OdsStreamDataReader()
  }

  Sheet processContent(final InputStream is, Object sheet, Integer startRow, Integer endRow, Integer startCol, Integer endCol){

    final XMLInputFactory factory = XMLInputFactory.newInstance()

    Integer sheetCount = 1
    final XMLStreamReader reader = factory.createXMLStreamReader(is)
    while (reader.hasNext()) {
      reader.next()
      if (reader.isStartElement()) {
        String startElement = reader.localName
        if (startElement == 'table') {

          String sheetName = reader.getAttributeValue(tableUrn, 'name')
          //println "sheetCount: $sheetCount, table: $sheetName: Attributes: ${attributes(startElement)}"
          if (sheet == sheetName) {
            //println "reading ${sheetName}, startRow=$startRow, endRow=$endRow, startColumn=$startColumn, endColumn=$endColumn"
            Sheet s = processSheet(reader, startRow, endRow, startCol, endCol)
            s.name = sheetName
            return s
          } else if(sheet == sheetCount) {
            println "reading ${sheetCount}, startRow=$startRow, endRow=$endRow, startColumn=$startCol, endColumn=$endCol"
            Sheet s = processSheet(reader, startRow, endRow, startCol, endCol)
            s.setName(sheetCount)
            return s
          }
          sheetCount++
        }
      }
    }
    return null
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
          text.setLength(0)
          reader.next()
          //println "start text extract, ${eventTypeName(element.eventType)}"
          while (!reader.isStartElement() || 'p' != reader.localName)
            reader.next()

          //println "in p, ${eventTypeName(element.eventType)}, ${element.asStartElement().attributes.collect()}"
          boolean isText = true

          while(isText) {
            if (reader.isCharacters()) {
              //println "assigned value $text"
              String s = reader.getText()
              if (s != null) {
                text.append(s)
              }
            } else if (reader.isStartElement() && reader.localName == 's') {
              def numSpaces = reader.getAttributeValue(textUrn, 'c')
              if (numSpaces != null) {
                char[] repeat = new char[Integer.parseInt(numSpaces)]
                Arrays.fill(repeat, ' ' as char)
                text.append(repeat)
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
