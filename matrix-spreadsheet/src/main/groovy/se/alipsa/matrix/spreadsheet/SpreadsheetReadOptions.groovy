package se.alipsa.matrix.spreadsheet

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

/**
 * Typed options for spreadsheet read operations via the SPI.
 */
@CompileStatic
class SpreadsheetReadOptions {

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
    this.sheetNumber = value
    this.sheetName = null
    this
  }

  SpreadsheetReadOptions sheetName(String value) {
    this.sheetName = value
    this.sheetNumber = null
    this
  }

  SpreadsheetReadOptions startRow(int value) {
    this.startRow = value
    this
  }

  SpreadsheetReadOptions endRow(int value) {
    this.endRow = value
    this
  }

  SpreadsheetReadOptions startColumn(int value) {
    this.startColumnNumber = value
    this.startColumnName = null
    this
  }

  SpreadsheetReadOptions startColumn(String value) {
    this.startColumnName = value
    this.startColumnNumber = null
    this
  }

  SpreadsheetReadOptions endColumn(int value) {
    this.endColumnNumber = value
    this.endColumnName = null
    this
  }

  SpreadsheetReadOptions endColumn(String value) {
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
    params.startCol = startColumnName ?: startColumnNumber
    if (endColumnName != null) {
      params.endCol = endColumnName
    } else if (endColumnNumber != null) {
      params.endCol = endColumnNumber
    }
    params
  }

  static SpreadsheetReadOptions fromMap(Map<String, ?> options) {
    SpreadsheetReadOptions result = new SpreadsheetReadOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)

    if (normalized.containsKey('sheetname')) {
      result.sheetName(String.valueOf(normalized.sheetname))
    } else if (normalized.containsKey('sheetnumber')) {
      result.sheetNumber((normalized.sheetnumber as Number).intValue())
    } else if (normalized.containsKey('sheet')) {
      def sheet = normalized.sheet
      if (sheet instanceof Number) {
        result.sheetNumber((sheet as Number).intValue())
      } else {
        result.sheetName(String.valueOf(sheet))
      }
    }

    if (normalized.containsKey('startrow')) {
      result.startRow((normalized.startrow as Number).intValue())
    }
    if (normalized.containsKey('endrow')) {
      result.endRow((normalized.endrow as Number).intValue())
    }

    Object startColumn = normalized.containsKey('startcolumn') ? normalized.startcolumn : normalized.startcol
    if (startColumn instanceof Number) {
      result.startColumn((startColumn as Number).intValue())
    } else if (startColumn != null) {
      result.startColumn(String.valueOf(startColumn))
    }

    Object endColumn = normalized.containsKey('endcolumn') ? normalized.endcolumn : normalized.endcol
    if (endColumn instanceof Number) {
      result.endColumn((endColumn as Number).intValue())
    } else if (endColumn != null) {
      result.endColumn(String.valueOf(endColumn))
    }

    if (normalized.containsKey('firstrowascolnames')) {
      result.firstRowAsColNames(normalized.firstrowascolnames as boolean)
    }
    result
  }

  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor('sheetName', String, null, 'Sheet name to read instead of a sheet number'),
        new OptionDescriptor('sheetNumber', Integer, '1', '1-based sheet number to read'),
        new OptionDescriptor('startRow', Integer, '1', '1-based first row to read'),
        new OptionDescriptor('endRow', Integer, 'auto-detect', '1-based last row to read'),
        new OptionDescriptor('startColumnName', String, null, 'Column name to start from, e.g. A or C'),
        new OptionDescriptor('startColumnNumber', Integer, '1', '1-based column number to start from'),
        new OptionDescriptor('endColumnName', String, null, 'Column name to stop at, e.g. D or AA'),
        new OptionDescriptor('endColumnNumber', Integer, 'auto-detect', '1-based last column number to read'),
        new OptionDescriptor('firstRowAsColNames', Boolean, 'true', 'Whether the first selected row contains column names')
    ]
  }
}
