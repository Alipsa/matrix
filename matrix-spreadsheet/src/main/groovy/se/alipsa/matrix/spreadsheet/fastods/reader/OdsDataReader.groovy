package se.alipsa.matrix.spreadsheet.fastods.reader

import groovy.transform.CompileStatic
import se.alipsa.matrix.spreadsheet.fastods.Sheet
import se.alipsa.matrix.spreadsheet.fastods.Spreadsheet

import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.*
import static se.alipsa.matrix.core.ValueConverter.asBigDecimal
import static se.alipsa.matrix.core.ValueConverter.asInteger

/**
 * Minimal reader that discards styles and only reads the content
 * into a list of rows.
 */
@CompileStatic
class OdsDataReader {

  static OdsDataReader create() {
    new OdsDataReader()
  }

  Spreadsheet readOds(InputStream is, Map<Object, List<Integer>> sheets) {
    Spreadsheet spreadsheet = null
    try (Uncompressor unc = new Uncompressor(is)) {
      String entry = unc.nextFile()
      while (entry != null) {
        if (entry == 'content.xml') {
          spreadsheet = processContent(unc.inputStream,sheets)
          break
        } else if (entry == "mimetype") {
          checkMimeType(unc)
        }
        entry = unc.nextFile()
      }
    }
    spreadsheet
  }

  Spreadsheet processContent(InputStream is, Map<Object, List<Integer>> sheets){

    XMLInputFactory factory = XMLInputFactory.newInstance()

    Spreadsheet spreadSheet = new Spreadsheet()
    Integer sheetCount = 1
    //XMLStreamReader reader = factory.createXMLStreamReader(is)
    XMLEventReader reader = factory.createXMLEventReader(is)
    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent()
      if (event.isStartElement()) {
        StartElement startElement = event.asStartElement()
        if (startElement.name.localPart == 'table') {

          String sheetName = startElement.getAttributeByName(tqn('name')).value
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



  Sheet processSheet(XMLEventReader reader, int startRow, int endRow, int startColumn, int endColumn) {
    Sheet sheet = new Sheet()
    XMLEvent event = reader.nextEvent()
    int rowCount = 1
    while(!(event.isEndElement() && event.asEndElement().name.localPart == 'table')) {
      //println " row $rowCount processSheet: element = ${eventTypeName(event.eventType)}: ${elementName(event)}"
      if (event.isStartElement() && event.asStartElement().name.localPart == 'table-row') {
        //println " table-row: row $rowCount Attributes: ${attributes(event.asStartElement())}"
        int repeatRows = asInteger(event.asStartElement().getAttributeByName(tqn('number-rows-repeated'))?.value ?: 1)
        for (int i = 0; i < repeatRows; i++) {
          if (rowCount < startRow) {
            event = reader.nextEvent()
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
      event = reader.nextEvent()
    }
    sheet
  }

  List<?> processRow(XMLEventReader reader, int startColumn, int endColumn) {
    List<?> row = new ArrayList<>()
    XMLEvent event = reader.nextEvent()
    int columnCount = 1
    while(!(event.isEndElement() && event.asEndElement().name.localPart == 'table-row')) {
      //println "   col $columnCount processRow: element = ${eventTypeName(event.eventType)}: ${elementName(event)}"
      if (event.isStartElement() && event.asStartElement().name.localPart == 'table-cell') {
        //println("   processRow columnCount=$columnCount, startColumn=$startColumn, endColumn=$endColumn")
        StartElement cellElement = event.asStartElement()
        //println "   table-cell: column $columnCount Attributes: ${attributes(event.asStartElement())}"
        int repeatColumns = asInteger(cellElement.getAttributeByName(tqn('number-columns-repeated'))?.value ?: 1)
        //println ("   Repeat repeatColumns times")
        for (int i = 0; i < repeatColumns; i++) {
          if (columnCount < startColumn) {
            //println "   processRow: skipping column $columnCount, value=${extractValue(reader, cellElement)}"
            while (!event.isStartElement()) {
              event = reader.nextEvent()
            }
            columnCount++
            continue
          } else if(columnCount > endColumn) {
            //println "   processRow: breaking at column $columnCount"
            break
          }
          def value = extractValue(reader, cellElement)
          row.add(value)
          //println("   processRow -> extract value from row $rowCount, column $columnCount: $value")
          columnCount++
        }
      }
      event = reader.nextEvent()
    }
    //println "Returning row: $row"
    row
  }

  Object extractValue(XMLEventReader reader, StartElement cellElement) {
    Object value = null
    String valueType = cellElement.getAttributeByName(oqn('value-type'))?.value
    // using yield just because it makes it slightly easier to see what the return value is
    //println "     extractValue from type $valueType in element ${cellElement.name.localPart}"
    if (valueType != null) {
      String attrValue
      value = switch (valueType) {
        case 'boolean' -> {
          attrValue = cellElement.getAttributeByName(oqn('boolean-value'))?.value
          yield Boolean.parseBoolean(attrValue)
        }
        case 'float', 'percentage', 'currency' -> {
          attrValue = cellElement.getAttributeByName(oqn('value'))?.value
          yield asBigDecimal(attrValue)
        }
        case 'date' -> {
          attrValue = cellElement.getAttributeByName(oqn('date-value'))?.value
          if (attrValue.length() == 10) {
            yield LocalDate.parse(attrValue)
          } else {
            yield LocalDateTime.parse(attrValue)
          }
        }
        case 'time' -> {
          attrValue = cellElement.getAttributeByName(oqn('time-value'))?.value
          yield Duration.parse(attrValue)
        }
        default -> {
          // extract the text value
          def element = reader.nextEvent()
          //println "start text extract, ${eventTypeName(element.eventType)}"
          while (!element.isStartElement() || 'p' != element.asStartElement().name.localPart)
            element = reader.nextEvent()

          //println "in p, ${eventTypeName(element.eventType)}, ${element.asStartElement().attributes.collect()}"
          boolean isText = true
          String text = null
          while(isText) {
            if (element.isCharacters()) {
              if (text == null) {
                //println "assigned value $text"
                text = element.asCharacters().data
              } else {
                text += element.asCharacters().data
                //println "appended value, now is $text"
              }
            }
            element = reader.nextEvent()
            //println "looking for text in p, ${eventTypeName(element.eventType)}"
            if (element.isEndElement() &&  'p' == element.asEndElement().name.localPart) {
              isText = false
            }
          }
          yield text
        }
      }
    }
    //println "     extractValue returning value $value"
    value
  }

  private static void checkMimeType(Uncompressor uncompressor) throws IOException {
    byte[] buff = new byte[OPENDOCUMENT_MIMETYPE.getBytes().length]
    uncompressor.getInputStream().read(buff)

    String mimetype = new String(buff);
    if (!mimetype.equals(OPENDOCUMENT_MIMETYPE))
      throw new NotAnOdsException("This file doesn't look like an ODS file. Mimetype: " + mimetype);
  }
}
