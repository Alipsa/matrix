package se.alipsa.matrix.spreadsheet.fastods.reader

import groovy.transform.CompileStatic
import se.alipsa.matrix.spreadsheet.fastods.FastOdsException
import se.alipsa.matrix.spreadsheet.fastods.Sheet
import se.alipsa.matrix.spreadsheet.XmlSecurityUtil

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
 * into a list of rows. It is the default reader for fastods.
 */
@CompileStatic
final class OdsStreamDataReader extends OdsDataReader {
  // Chosen to tolerate large, intentional padding while preventing runaway expansion of trailing empty rows.
  private static final int TRAILING_EMPTY_ROW_THRESHOLD = 1000

  static OdsStreamDataReader create() {
    new OdsStreamDataReader()
  }

  Sheet processContent(final InputStream is, Object sheet, Integer startRow, Integer endRow, Integer startCol, Integer endCol) {
    final XMLInputFactory factory = XmlSecurityUtil.newSecureInputFactory()
    Integer sheetCount = 1
    final XMLStreamReader reader = factory.createXMLStreamReader(is)
    if (sheet == null) {
      throw new FastOdsException("Sheet name or number must be provided but was null")
    }
    try {
      while (reader.hasNext()) {
        reader.next()
        if (reader.isStartElement() && reader.localName == 'table') {
          String sheetName = reader.getAttributeValue(tableUrn, 'name').trim()
          if (sheet == sheetName || sheet == sheetCount) {
            Sheet s = processSheet(reader, startRow, endRow, startCol, endCol)
            if (s == null) {
              throw new FastOdsException("Failed to process '$sheet' in the ODS file")
            }
            s.name = String.valueOf(sheet)
            return s
          }
          sheetCount++
        }
      }
    } finally {
      reader.close()
    }
    throw new FastOdsException("Failed to find sheet '$sheet' in the ODS file")
  }

  final static Sheet processSheet(final XMLStreamReader reader, final int startRow, final int endRow, int startColumn, int endColumn) {
    Sheet sheet = new Sheet()
    int rowCount = 1
    while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'table')) {
      if (reader.isStartElement() && reader.localName == 'table-row') {
        int repeatRows = asInteger(reader.getAttributeValue(tableUrn, 'number-rows-repeated') ?: 1)

        // Parse this physical row ONCE (consumes until </table-row>)
        List<Object> rowValues = processRow(reader, startColumn, endColumn)
        boolean isEmptyRow = rowValues == null || rowValues.isEmpty() || rowValues.every { it == null }

        boolean isUnbounded = endRow == Integer.MAX_VALUE
        boolean exceedsRequested = !isUnbounded && endRow > rowCount + repeatRows
        if (isEmptyRow && repeatRows > TRAILING_EMPTY_ROW_THRESHOLD && (isUnbounded || exceedsRequested)) {
          // ODS files often encode trailing empty rows with a huge repeat count.
          // Stop here to avoid inflating row counts and memory usage.
          break
        }

        // Replicate logically according to repeatRows
        for (int i = 0; i < repeatRows; i++) {
          if (rowCount >= startRow && rowCount <= endRow) {
            sheet.add(new ArrayList<>(rowValues))
          }
          rowCount++
        }
        // don't call processRow again for the same physical row
      }
      if (reader.hasNext()) {
        reader.next()
      } else {
        break
      }
    }
    return sheet
  }

  final static List<Object> processRow(final XMLStreamReader reader, final int startColumn, final int endColumn) {
    List<Object> row = new ArrayList<>()
    int columnCount = 1

    // We enter with cursor at <table-row>; consume its children until </table-row>
    while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'table-row')) {
      if (reader.isStartElement() && reader.localName == 'table-cell') {
        int repeatColumns = asInteger(reader.getAttributeValue(tableUrn, 'number-columns-repeated') ?: 1)
        Object cellValue = extractValue(reader)
        // drain to </table-cell>
        while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'table-cell')) reader.next()
        for (int i = 0; i < repeatColumns; i++) {
          if (columnCount >= startColumn && columnCount <= endColumn) row.add(cellValue)
          columnCount++
        }
      } else if (reader.isStartElement() && reader.localName == 'covered-table-cell') {
        int repeatColumns = asInteger(reader.getAttributeValue(tableUrn, 'number-columns-repeated') ?: 1)
        // drain to </covered-table-cell>
        while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'covered-table-cell')) reader.next()
        for (int i = 0; i < repeatColumns; i++) {
          if (columnCount >= startColumn && columnCount <= endColumn) row.add(null)
          columnCount++
        }
      }
      reader.next()
    }
    return row
  }

  final static Object extractValue(final XMLStreamReader reader) {
    // Cursor is at <table:table-cell ...> here
    String valueType = reader.getAttributeValue(officeUrn, 'value-type')

    // Typed values via attributes
    if (valueType == 'boolean') {
      return Boolean.parseBoolean(reader.getAttributeValue(officeUrn, 'boolean-value'))
    } else if (valueType == 'float' || valueType == 'percentage' || valueType == 'currency') {
      String v = reader.getAttributeValue(officeUrn, 'value')
      return v != null ? asBigDecimal(v) : null
    } else if (valueType == 'date') {
      String v = reader.getAttributeValue(officeUrn, 'date-value')
      return (v != null && v.length() == 10) ? LocalDate.parse(v) : (v != null ? LocalDateTime.parse(v) : null)
    } else if (valueType == 'time') {
      String v = reader.getAttributeValue(officeUrn, 'time-value')
      return v != null ? Duration.parse(v) : null
    }

    // Fallback for strings/unknown types: collect <text:p> content until </table-cell>
    // Also handles empty/self-closing <table-cell/> → returns null
    StringBuilder text = new StringBuilder()
    while (reader.hasNext()) {
      reader.next()

      if (reader.isStartElement()) {
        if (reader.localName == 'p') {
          // Separate multiple <text:p> blocks with newline
          if (text.length() > 0) text.append('\n')
        } else if (reader.localName == 's') {
          // <text:s c="N"/> ⇒ N spaces (default 1)
          int numSpaces = asInteger(reader.getAttributeValue(textUrn, 'c')) ?: 1
          text.append(' '.repeat(numSpaces))
        } else if (reader.localName == 'line-break') {
          text.append('\n')
        } else if (reader.localName == 'tab') {
          text.append('\t')
        }
      } else if (reader.isCharacters()) {
        text.append(reader.getText())
      } else if (reader.isEndElement() && reader.localName == 'table-cell') {
        // stop at end of cell (covers empty/self-closing cells)
        break
      }
    }

    String s = text.toString()
    return s.isEmpty() ? null : s
  }
}
