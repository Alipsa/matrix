package se.alipsa.matrix.spreadsheet.fastods.reader

import groovy.transform.CompileStatic
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
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
 *
 * <h2>Trailing Empty Row Handling</h2>
 * <p>ODS files often encode trailing empty rows with a large {@code table:number-rows-repeated}
 * attribute (e.g., 1048576 rows) to represent the "rest of the sheet". Without special handling,
 * this would cause massive memory consumption and slow processing.</p>
 *
 * <p>This reader uses a {@link #TRAILING_EMPTY_ROW_THRESHOLD} of 1000 rows. When an empty row
 * (all null values) has a repeat count exceeding this threshold, the reader stops processing
 * additional rows. This effectively treats such rows as "end of data" markers.</p>
 *
 * <p>This threshold was chosen to:</p>
 * <ul>
 *   <li>Allow legitimate use cases with up to 1000 intentional empty rows</li>
 *   <li>Prevent runaway memory usage from ODS files with massive repeat counts</li>
 *   <li>Provide consistent, predictable behavior across different ODS generators</li>
 * </ul>
 *
 * <p>If you have a valid use case requiring more than 1000 consecutive empty rows,
 * consider using explicit row ranges in your import call.</p>
 */
@CompileStatic
final class OdsStreamDataReader extends OdsDataReader {
  private static final Logger logger = LogManager.getLogger(OdsStreamDataReader)
  private static final boolean PROFILE = Boolean.getBoolean('matrix.spreadsheet.ods.profile')
  private static final ProfileStats PROFILE_STATS = PROFILE ? new ProfileStats() : null

  /**
   * Maximum number of consecutive empty rows to expand before stopping.
   * ODS files often encode trailing empty rows with huge repeat counts (e.g., 1048576).
   * This threshold prevents runaway memory usage while allowing reasonable padding.
   */
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
    if (PROFILE) {
      PROFILE_STATS.reset()
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
            if (PROFILE) {
              PROFILE_STATS.log(String.valueOf(sheet))
            }
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
    long sheetStart = PROFILE ? System.nanoTime() : 0L
    int rowCount = 1
    while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'table')) {
      if (reader.isStartElement() && reader.localName == 'table-row') {
        if (PROFILE) {
          PROFILE_STATS.physicalRows++
        }
        int repeatRows = asInteger(reader.getAttributeValue(tableUrn, 'number-rows-repeated') ?: 1)

        // Parse this physical row ONCE (consumes until </table-row>)
        List<Object> rowValues = processRow(reader, startColumn, endColumn)
        boolean isUnbounded = endRow == Integer.MAX_VALUE
        boolean exceedsRequested = !isUnbounded && endRow > rowCount + repeatRows
        if (repeatRows > TRAILING_EMPTY_ROW_THRESHOLD && (isUnbounded || exceedsRequested)) {
          boolean isEmptyRow = rowValues == null || rowValues.isEmpty() || rowValues.every { it == null }
          if (isEmptyRow) {
            // ODS files often encode trailing empty rows with a huge repeat count.
            // Stop here to avoid inflating row counts and memory usage.
            break
          }
        }

        // Replicate logically according to repeatRows
        if (repeatRows == 1) {
          if (rowCount >= startRow && rowCount <= endRow) {
            sheet.add(rowValues)
            if (PROFILE) {
              PROFILE_STATS.logicalRows++
            }
          }
          rowCount++
        } else {
          for (int i = 0; i < repeatRows; i++) {
            if (rowCount >= startRow && rowCount <= endRow) {
              sheet.add(new ArrayList<>(rowValues))
              if (PROFILE) {
                PROFILE_STATS.logicalRows++
              }
            }
            rowCount++
          }
        }
        // don't call processRow again for the same physical row
      }
      if (reader.hasNext()) {
        reader.next()
      } else {
        break
      }
    }
    if (PROFILE) {
      PROFILE_STATS.processSheetNanos += System.nanoTime() - sheetStart
    }
    return sheet
  }

  final static List<Object> processRow(final XMLStreamReader reader, final int startColumn, final int endColumn) {
    if (!PROFILE) {
      return processRowInternal(reader, startColumn, endColumn)
    }
    long start = System.nanoTime()
    try {
      return processRowInternal(reader, startColumn, endColumn)
    } finally {
      PROFILE_STATS.processRowNanos += System.nanoTime() - start
    }
  }

  private static List<Object> processRowInternal(final XMLStreamReader reader, final int startColumn, final int endColumn) {
    int expectedColumns = endColumn == Integer.MAX_VALUE ? 16 : Math.max(0, endColumn - startColumn + 1)
    List<Object> row = new ArrayList<>(expectedColumns)
    int columnCount = 1

    // We enter with cursor at <table-row>; consume its children until </table-row>
    while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'table-row')) {
      if (endColumn != Integer.MAX_VALUE && columnCount > endColumn) {
        skipToEndRow(reader)
        break
      }
      if (reader.isStartElement() && reader.localName == 'table-cell') {
        int repeatColumns = asInteger(reader.getAttributeValue(tableUrn, 'number-columns-repeated') ?: 1)
        Object cellValue = extractValue(reader)
        // drain to </table-cell>
        while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'table-cell')) reader.next()
        columnCount = appendRepeatedValue(row, columnCount, repeatColumns, startColumn, endColumn, cellValue)
      } else if (reader.isStartElement() && reader.localName == 'covered-table-cell') {
        int repeatColumns = asInteger(reader.getAttributeValue(tableUrn, 'number-columns-repeated') ?: 1)
        // drain to </covered-table-cell>
        while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'covered-table-cell')) reader.next()
        columnCount = appendRepeatedValue(row, columnCount, repeatColumns, startColumn, endColumn, null)
      }
      reader.next()
    }
    return row
  }

  private static void skipToEndRow(final XMLStreamReader reader) {
    while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'table-row')) {
      reader.next()
    }
  }

  private static int appendRepeatedValue(List<Object> row, int columnCount, int repeatColumns,
                                         int startColumn, int endColumn, Object value) {
    if (repeatColumns == 1) {
      if (columnCount >= startColumn && columnCount <= endColumn) {
        row.add(value)
        if (PROFILE) {
          PROFILE_STATS.cellsAdded++
        }
      } else if (PROFILE) {
        PROFILE_STATS.cellsSkipped++
      }
      return columnCount + 1
    }
    int rangeStart = columnCount
    int rangeEnd = columnCount + repeatColumns - 1
    if (rangeEnd < startColumn || rangeStart > endColumn) {
      if (PROFILE) {
        PROFILE_STATS.cellsSkipped += repeatColumns
      }
      return columnCount + repeatColumns
    }
    int addFrom = Math.max(rangeStart, startColumn)
    int addTo = Math.min(rangeEnd, endColumn)
    int addCount = addTo - addFrom + 1
    for (int i = 0; i < addCount; i++) {
      row.add(value)
    }
    if (PROFILE) {
      PROFILE_STATS.cellsAdded += addCount
      PROFILE_STATS.cellsSkipped += (repeatColumns - addCount)
    }
    return columnCount + repeatColumns
  }

  final static Object extractValue(final XMLStreamReader reader) {
    if (!PROFILE) {
      return extractValueInternal(reader)
    }
    long start = System.nanoTime()
    try {
      return extractValueInternal(reader)
    } finally {
      PROFILE_STATS.extractValueCalls++
      PROFILE_STATS.extractValueNanos += System.nanoTime() - start
    }
  }

  private static Object extractValueInternal(final XMLStreamReader reader) {
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

  @CompileStatic
  private static final class ProfileStats {
    long physicalRows = 0
    long logicalRows = 0
    long cellsAdded = 0
    long cellsSkipped = 0
    long extractValueCalls = 0
    long extractValueNanos = 0
    long processRowNanos = 0
    long processSheetNanos = 0

    void reset() {
      physicalRows = 0
      logicalRows = 0
      cellsAdded = 0
      cellsSkipped = 0
      extractValueCalls = 0
      extractValueNanos = 0
      processRowNanos = 0
      processSheetNanos = 0
    }

    void log(String sheetName) {
      long sheetMs = nanosToMillis(processSheetNanos)
      long rowMs = nanosToMillis(processRowNanos)
      long extractMs = nanosToMillis(extractValueNanos)
      long avgExtract = extractValueCalls == 0 ? 0L : ((extractMs / extractValueCalls) as long)
      String message = "ODS profile ${sheetName}: sheet=${sheetMs} ms, rows(physical/logical)=${physicalRows}/${logicalRows}, " +
          "cells(added/skipped)=${cellsAdded}/${cellsSkipped}, extractValue=${extractMs} ms (calls=${extractValueCalls}, avg=${avgExtract} ms), " +
          "processRow=${rowMs} ms"
      logger.info(message)
    }

    private static long nanosToMillis(long nanos) {
      return (nanos / 1_000_000L) as long
    }
  }
}
