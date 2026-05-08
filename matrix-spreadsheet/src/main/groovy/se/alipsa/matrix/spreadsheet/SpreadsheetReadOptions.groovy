package se.alipsa.matrix.spreadsheet

import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

/**
 * Typed options for spreadsheet read operations via the SPI.
 */
class SpreadsheetReadOptions {

  private static final String OPT_SHEET = 'sheet'
  private static final String OPT_START_COLUMN = 'startColumn'
  private static final String OPT_END_COLUMN = 'endColumn'
  private static final String OPT_SHEET_NAME = 'sheetName'
  private static final String OPT_SHEET_NUMBER = 'sheetNumber'
  private static final String OPT_START_ROW = 'startRow'
  private static final String OPT_END_ROW = 'endRow'
  private static final String DEFAULT_ONE = '1'
  private static final String DEFAULT_AUTO = 'auto-detect'
  private static final String DEFAULT_TRUE = 'true'

  Integer sheetNumber = 1
  String sheetName = null
  int startRow = 1
  Integer endRow = null
  Integer startColumnNumber = 1
  String startColumnName = null
  Integer endColumnNumber = null
  String endColumnName = null
  boolean firstRowAsColNames = true

  SpreadsheetReadOptions sheet(int value) {
    sheetNumber(value)
  }

  SpreadsheetReadOptions sheet(String value) {
    sheetName(value)
  }

  SpreadsheetReadOptions sheetNumber(int value) {
    requireAtLeastOne(value, OPT_SHEET_NUMBER)
    this.sheetNumber = value
    this.sheetName = null
    this
  }

  SpreadsheetReadOptions sheetName(String value) {
    if (value == null) {
      return this
    }
    requireText(value, OPT_SHEET_NAME)
    this.sheetName = value
    this.sheetNumber = null
    this
  }

  SpreadsheetReadOptions startRow(int value) {
    requireAtLeastOne(value, OPT_START_ROW)
    this.startRow = value
    this
  }

  SpreadsheetReadOptions endRow(int value) {
    requireAtLeastOne(value, OPT_END_ROW)
    this.endRow = value
    this
  }

  SpreadsheetReadOptions startColumn(int value) {
    requireAtLeastOne(value, OPT_START_COLUMN)
    this.startColumnNumber = value
    this.startColumnName = null
    this
  }

  SpreadsheetReadOptions startColumn(String value) {
    if (value == null) {
      return this
    }
    requireText(value, OPT_START_COLUMN)
    this.startColumnName = value
    this.startColumnNumber = null
    this
  }

  SpreadsheetReadOptions endColumn(int value) {
    requireAtLeastOne(value, OPT_END_COLUMN)
    this.endColumnNumber = value
    this.endColumnName = null
    this
  }

  SpreadsheetReadOptions endColumn(String value) {
    if (value == null) {
      return this
    }
    requireText(value, OPT_END_COLUMN)
    this.endColumnName = value
    this.endColumnNumber = null
    this
  }

  SpreadsheetReadOptions firstRowAsColNames(boolean value) {
    this.firstRowAsColNames = value
    this
  }

  boolean hasSheetName() {
    sheetName != null
  }

  boolean hasEndColumn() {
    endColumnNumber != null || endColumnName != null
  }

  Map<String, ?> toMap() {
    Map<String, Object> params = [
        startRow          : startRow,
        firstRowAsColNames: firstRowAsColNames
    ]
    if (sheetName != null) {
      params.sheetName = sheetName
    } else if (sheetNumber != null) {
      params.sheetNumber = sheetNumber
    }
    if (endRow != null) {
      params.endRow = endRow
    }
    params.startColumn = startColumnName ?: startColumnNumber
    if (endColumnName != null) {
      params.endColumn = endColumnName
    } else if (endColumnNumber != null) {
      params.endColumn = endColumnNumber
    }
    params
  }

  static SpreadsheetReadOptions fromMap(Map<String, ?> options) {
    SpreadsheetReadOptions result = new SpreadsheetReadOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)

    if (normalized.containsKey('sheetname')) {
      String sheetName = OptionMaps.stringValueOrNull(normalized.sheetname)
      if (sheetName != null) {
        result.sheetName(sheetName)
      }
    } else if (normalized.containsKey('sheetnumber')) {
      Object sheetNumber = normalized.sheetnumber
      if (sheetNumber != null) {
        result.sheetNumber((sheetNumber as Number).intValue())
      }
    } else if (normalized.containsKey(OPT_SHEET)) {
      def sheet = normalized.sheet
      if (Number.isInstance(sheet)) {
        result.sheetNumber((sheet as Number).intValue())
      } else if (sheet != null) {
        result.sheetName(String.valueOf(sheet))
      }
    }

    if (normalized.containsKey('startrow')) {
      Object startRow = normalized.startrow
      if (startRow != null) {
        result.startRow((startRow as Number).intValue())
      }
    }
    if (normalized.containsKey('endrow')) {
      Object endRow = normalized.endrow
      if (endRow != null) {
        result.endRow((endRow as Number).intValue())
      }
    }

    Object startColumn = normalized.containsKey('startcolumn') ? normalized.startcolumn : normalized.startcol
    if (Number.isInstance(startColumn)) {
      result.startColumn((startColumn as Number).intValue())
    } else if (startColumn != null) {
      result.startColumn(String.valueOf(startColumn))
    }

    Object endColumn = normalized.containsKey('endcolumn') ? normalized.endcolumn : normalized.endcol
    if (Number.isInstance(endColumn)) {
      result.endColumn((endColumn as Number).intValue())
    } else if (endColumn != null) {
      result.endColumn(String.valueOf(endColumn))
    }

    if (normalized.containsKey('firstrowascolnames')) {
      Object firstRowAsColNames = normalized.firstrowascolnames
      if (firstRowAsColNames != null) {
        result.firstRowAsColNames(firstRowAsColNames as boolean)
      }
    }
    result
  }

  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor(OPT_SHEET, Object, null, 'Sheet selector as a name or a 1-based sheet number'),
        new OptionDescriptor(OPT_SHEET_NAME, String, null, 'Sheet name to read instead of a sheet number'),
        new OptionDescriptor(OPT_SHEET_NUMBER, Integer, DEFAULT_ONE, '1-based sheet number to read'),
        new OptionDescriptor(OPT_START_ROW, Integer, DEFAULT_ONE, '1-based first row to read'),
        new OptionDescriptor(OPT_END_ROW, Integer, DEFAULT_AUTO, '1-based last row to read'),
        new OptionDescriptor(OPT_START_COLUMN, Object, DEFAULT_ONE, 'Column to start from, as a name like A or a 1-based column number'),
        new OptionDescriptor(OPT_END_COLUMN, Object, DEFAULT_AUTO, 'Column to stop at, as a name like D or a 1-based column number'),
        new OptionDescriptor('firstRowAsColNames', Boolean, DEFAULT_TRUE, 'Whether the first selected row contains column names')
    ]
  }

  private static void requireAtLeastOne(int value, String name) {
    if (value < 1) {
      throw new IllegalArgumentException("$name must be >= 1")
    }
  }

  private static void requireText(String value, String name) {
    if (value.isBlank()) {
      throw new IllegalArgumentException("$name must not be blank")
    }
  }

}
