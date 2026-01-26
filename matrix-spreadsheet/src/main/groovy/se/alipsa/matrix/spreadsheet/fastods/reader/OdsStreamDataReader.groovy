package se.alipsa.matrix.spreadsheet.fastods.reader

import groovy.transform.CompileStatic
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import se.alipsa.matrix.spreadsheet.fastods.FastOdsException
import se.alipsa.matrix.spreadsheet.fastods.Sheet

import javax.xml.stream.XMLStreamReader
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

import static se.alipsa.matrix.core.ValueConverter.asBigDecimal
import static se.alipsa.matrix.core.ValueConverter.asInteger
import static se.alipsa.matrix.spreadsheet.fastods.OdsXmlUtil.*
import static se.alipsa.matrix.spreadsheet.fastods.reader.OptimizedXMLInputFactory.INSTANCE

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

  /**
   * Profiling statistics collector. Uses Null Object pattern to avoid branch overhead.
   * Set JVM system property -Dmatrix.spreadsheet.ods.profile=true to enable profiling.
   */
  private static final ProfileStats PROFILE_STATS = Boolean.getBoolean('matrix.spreadsheet.ods.profile')
      ? new RealProfileStats()
      : new NoOpProfileStats()

  /**
   * Maximum number of consecutive empty rows to expand before stopping.
   * ODS files often encode trailing empty rows with huge repeat counts (e.g., 1048576).
   * This threshold prevents runaway memory usage while allowing reasonable padding.
   */
  private static final int TRAILING_EMPTY_ROW_THRESHOLD = 1000

  /**
   * Adaptive row capacity tracking.
   * Learns the actual row width from parsed data to minimize ArrayList resizing.
   * Initialized to 16 (typical minimum), grows to match the widest row seen.
   */
  private int rowCapacity = 16

  static OdsStreamDataReader create() {
    new OdsStreamDataReader()
  }

  Sheet processContent(final InputStream is, Object sheet, Integer startRow, Integer endRow, Integer startCol, Integer endCol) {
    Integer sheetCount = 1
    final XMLStreamReader reader = INSTANCE.createXMLStreamReader(is)
    if (sheet == null) {
      throw new FastOdsException("Sheet name or number must be provided but was null")
    }
    PROFILE_STATS.reset()
    try {
      while (reader.hasNext()) {
        reader.next()
        if (reader.isStartElement() && reader.localName == 'table') {
          String sheetName = reader.getAttributeValue(tableUrn, 'name').trim()
          if (sheet == sheetName || sheet == sheetCount) {
            // Reset rowCapacity for each sheet to learn its specific width
            rowCapacity = 16
            Sheet s = processSheet(reader, startRow, endRow, startCol, endCol)
            if (s == null) {
              throw new FastOdsException("Failed to process '$sheet' in the ODS file")
            }
            s.name = String.valueOf(sheet)
            PROFILE_STATS.log(String.valueOf(sheet))
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

  final Sheet processSheet(final XMLStreamReader reader, final int startRow, final int endRow, int startColumn, int endColumn) {
    // Cache URN constant locally for hot path
    final String TABLE_URN = tableUrn

    Sheet sheet = new Sheet()
    long sheetStart = System.nanoTime()
    int rowCount = 1
    while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'table')) {
      if (reader.isStartElement() && reader.localName == 'table-row') {
        PROFILE_STATS.incrementPhysicalRows()
        int repeatRows = asInteger(reader.getAttributeValue(TABLE_URN, 'number-rows-repeated') ?: 1)

        // Parse this physical row ONCE (consumes until </table-row>), using learned capacity
        List<Object> rowValues = processRow(reader, startColumn, endColumn, rowCapacity)

        // Learn from actual row size for future rows
        rowCapacity = Math.max(rowCapacity, rowValues.size())

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
            PROFILE_STATS.incrementLogicalRows()
          }
          rowCount++
        } else {
          for (int i = 0; i < repeatRows; i++) {
            if (rowCount >= startRow && rowCount <= endRow) {
              sheet.add(new ArrayList<>(rowValues))
              PROFILE_STATS.incrementLogicalRows()
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
    PROFILE_STATS.addProcessSheetTime(System.nanoTime() - sheetStart)
    return sheet
  }

  final static List<Object> processRow(final XMLStreamReader reader, final int startColumn, final int endColumn, final int initialCapacity) {
    long start = System.nanoTime()
    try {
      return processRowInternal(reader, startColumn, endColumn, initialCapacity)
    } finally {
      PROFILE_STATS.addProcessRowTime(System.nanoTime() - start)
    }
  }

  private static List<Object> processRowInternal(final XMLStreamReader reader, final int startColumn, final int endColumn, final int initialCapacity) {
    // Cache URN constant locally for hot path
    final String TABLE_URN = tableUrn

    // Use adaptive capacity if available, otherwise calculate from range
    int capacity = initialCapacity > 0 ? initialCapacity :
        (endColumn == Integer.MAX_VALUE ? 16 : Math.max(0, endColumn - startColumn + 1))
    List<Object> row = new ArrayList<>(capacity)
    int columnCount = 1

    // We enter with cursor at <table-row>; consume its children until </table-row>
    while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'table-row')) {
      if (endColumn != Integer.MAX_VALUE && columnCount > endColumn) {
        skipToEndRow(reader)
        break
      }
      if (reader.isStartElement() && reader.localName == 'table-cell') {
        int repeatColumns = asInteger(reader.getAttributeValue(TABLE_URN, 'number-columns-repeated') ?: 1)
        Object cellValue = extractValue(reader)
        // drain to </table-cell>
        while (reader.hasNext() && !(reader.isEndElement() && reader.localName == 'table-cell')) reader.next()
        columnCount = appendRepeatedValue(row, columnCount, repeatColumns, startColumn, endColumn, cellValue)
      } else if (reader.isStartElement() && reader.localName == 'covered-table-cell') {
        int repeatColumns = asInteger(reader.getAttributeValue(TABLE_URN, 'number-columns-repeated') ?: 1)
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
        PROFILE_STATS.incrementCellsAdded()
      } else {
        PROFILE_STATS.incrementCellsSkipped()
      }
      return columnCount + 1
    }
    int rangeStart = columnCount
    int rangeEnd = columnCount + repeatColumns - 1
    if (rangeEnd < startColumn || rangeStart > endColumn) {
      PROFILE_STATS.incrementCellsSkipped(repeatColumns)
      return columnCount + repeatColumns
    }
    int addFrom = Math.max(rangeStart, startColumn)
    int addTo = Math.min(rangeEnd, endColumn)
    int addCount = addTo - addFrom + 1
    for (int i = 0; i < addCount; i++) {
      row.add(value)
    }
    PROFILE_STATS.incrementCellsAdded(addCount)
    PROFILE_STATS.incrementCellsSkipped(repeatColumns - addCount)
    return columnCount + repeatColumns
  }

  final static Object extractValue(final XMLStreamReader reader) {
    long start = System.nanoTime()
    try {
      return extractValueInternal(reader)
    } finally {
      PROFILE_STATS.addExtractValueTime(System.nanoTime() - start)
    }
  }

  private static Object extractValueInternal(final XMLStreamReader reader) {
    // Cache URN constants locally to reduce field access overhead
    final String OFFICE_URN = officeUrn
    final String TEXT_URN = textUrn

    // Cursor is at <table:table-cell ...> here
    String valueType = reader.getAttributeValue(OFFICE_URN, 'value-type')

    // Fast path: typed values extracted from attributes (90% of cells)
    // Using switch for better JIT optimization vs if-else chain
    if (valueType != null) {
      switch (valueType) {
        case 'boolean':
          return extractBooleanValue(reader, OFFICE_URN)
        case 'float':
        case 'percentage':
        case 'currency':
          return extractNumericValue(reader, OFFICE_URN)
        case 'date':
          return extractDateValue(reader, OFFICE_URN)
        case 'time':
          return extractTimeValue(reader, OFFICE_URN)
        default:
          // fall through to text extraction for unknown types
          break
      }
    }

    // Slow path: text content from child elements (10% of cells)
    return extractTextContent(reader, TEXT_URN)
  }

  /**
   * Extract boolean value from office:boolean-value attribute.
   */
  private static Boolean extractBooleanValue(final XMLStreamReader reader, final String officeUrn) {
    String v = reader.getAttributeValue(officeUrn, 'boolean-value')
    return v != null ? Boolean.parseBoolean(v) : null
  }

  /**
   * Extract numeric value from office:value attribute.
   * Handles float, percentage, and currency types.
   */
  private static BigDecimal extractNumericValue(final XMLStreamReader reader, final String officeUrn) {
    String v = reader.getAttributeValue(officeUrn, 'value')
    return v != null ? asBigDecimal(v) : null
  }

  /**
   * Extract date/datetime value from office:date-value attribute.
   * Returns LocalDate for date-only values (10 chars: YYYY-MM-DD),
   * LocalDateTime for datetime values (19+ chars: YYYY-MM-DDTHH:MM:SS).
   */
  private static Object extractDateValue(final XMLStreamReader reader, final String officeUrn) {
    String v = reader.getAttributeValue(officeUrn, 'date-value')
    if (v == null) return null
    // Date format: YYYY-MM-DD (10 chars) vs DateTime: YYYY-MM-DDTHH:MM:SS
    return v.length() == 10 ? LocalDate.parse(v) : LocalDateTime.parse(v)
  }

  /**
   * Extract time duration value from office:time-value attribute.
   */
  private static Duration extractTimeValue(final XMLStreamReader reader, final String officeUrn) {
    String v = reader.getAttributeValue(officeUrn, 'time-value')
    return v != null ? Duration.parse(v) : null
  }

  /**
   * Extract text content from child elements (text:p, text:s, etc.).
   * Pre-allocates StringBuilder with 64-char capacity to reduce resizing.
   * Handles empty/self-closing cells by returning null.
   */
  private static String extractTextContent(final XMLStreamReader reader, final String textUrn) {
    // Pre-allocate typical cell text size to reduce StringBuilder resizing
    StringBuilder text = new StringBuilder(64)

    while (reader.hasNext()) {
      int eventType = reader.next()

      // Fast path: character data (most common in text cells)
      if (eventType == XMLStreamReader.CHARACTERS ||
          eventType == XMLStreamReader.CDATA ||
          eventType == XMLStreamReader.SPACE) {
        text.append(reader.getText())
        continue
      }

      // Element handling
      if (eventType == XMLStreamReader.START_ELEMENT) {
        String localName = reader.localName  // Cache to avoid repeated calls
        switch (localName) {
          case 'p':
            // Separate multiple <text:p> blocks with newline
            if (text.length() > 0) text.append('\n')
            break
          case 's':
            // <text:s c="N"/> ⇒ N spaces (default 1)
            int numSpaces = asInteger(reader.getAttributeValue(textUrn, 'c')) ?: 1
            text.append(' '.repeat(numSpaces))
            break
          case 'line-break':
            text.append('\n')
            break
          case 'tab':
            text.append('\t')
            break
        }
      } else if (eventType == XMLStreamReader.END_ELEMENT && reader.localName == 'table-cell') {
        // Stop at end of cell (covers empty/self-closing cells)
        break
      }
    }

    String s = text.toString()
    return s.isEmpty() ? null : s
  }

  /**
   * Abstract base class for profiling statistics.
   * Uses Null Object pattern to avoid branching overhead in hot paths.
   */
  @CompileStatic
  private static abstract class ProfileStats {
    abstract void reset()
    abstract void log(String sheetName)
    abstract void incrementPhysicalRows()
    abstract void incrementLogicalRows()
    abstract void incrementCellsAdded()
    abstract void incrementCellsSkipped()
    abstract void incrementCellsSkipped(int count)
    abstract void incrementCellsAdded(int count)
    abstract void addExtractValueTime(long nanos)
    abstract void addProcessRowTime(long nanos)
    abstract void addProcessSheetTime(long nanos)

    // Provide property-like access for compatibility
    void setPhysicalRows(long value) { /* no-op by default */ }
    void setLogicalRows(long value) { /* no-op by default */ }
    void setCellsAdded(long value) { /* no-op by default */ }
    void setCellsSkipped(long value) { /* no-op by default */ }
    void setExtractValueCalls(long value) { /* no-op by default */ }
    void setExtractValueNanos(long value) { /* no-op by default */ }
    void setProcessRowNanos(long value) { /* no-op by default */ }
    void setProcessSheetNanos(long value) { /* no-op by default */ }

    // Support ++ operator
    ProfileStats next() { return this }
  }

  /**
   * No-op implementation for production use (profiling disabled).
   * All methods are empty to eliminate overhead.
   */
  @CompileStatic
  private static final class NoOpProfileStats extends ProfileStats {
    @Override void reset() {}
    @Override void log(String sheetName) {}
    @Override void incrementPhysicalRows() {}
    @Override void incrementLogicalRows() {}
    @Override void incrementCellsAdded() {}
    @Override void incrementCellsSkipped() {}
    @Override void incrementCellsSkipped(int count) {}
    @Override void incrementCellsAdded(int count) {}
    @Override void addExtractValueTime(long nanos) {}
    @Override void addProcessRowTime(long nanos) {}
    @Override void addProcessSheetTime(long nanos) {}
  }

  /**
   * Real implementation for profiling use.
   * Collects and logs detailed performance statistics.
   */
  @CompileStatic
  private static final class RealProfileStats extends ProfileStats {
    long physicalRows = 0
    long logicalRows = 0
    long cellsAdded = 0
    long cellsSkipped = 0
    long extractValueCalls = 0
    long extractValueNanos = 0
    long processRowNanos = 0
    long processSheetNanos = 0

    @Override
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

    @Override
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

    @Override void incrementPhysicalRows() { physicalRows++ }
    @Override void incrementLogicalRows() { logicalRows++ }
    @Override void incrementCellsAdded() { cellsAdded++ }
    @Override void incrementCellsSkipped() { cellsSkipped++ }
    @Override void incrementCellsSkipped(int count) { cellsSkipped += count }
    @Override void incrementCellsAdded(int count) { cellsAdded += count }

    @Override
    void addExtractValueTime(long nanos) {
      extractValueCalls++
      extractValueNanos += nanos
    }

    @Override
    void addProcessRowTime(long nanos) {
      processRowNanos += nanos
    }

    @Override
    void addProcessSheetTime(long nanos) {
      processSheetNanos += nanos
    }

    private static long nanosToMillis(long nanos) {
      return (nanos / 1_000_000L) as long
    }
  }
}
