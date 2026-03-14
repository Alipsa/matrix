package se.alipsa.matrix.spreadsheet

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

/**
 * Typed options for spreadsheet write operations via the SPI.
 */
@CompileStatic
class SpreadsheetWriteOptions {

  String sheetName = null
  String startPosition = 'A1'

  SpreadsheetWriteOptions sheetName(String value) {
    this.sheetName = value
    this
  }

  SpreadsheetWriteOptions startPosition(String value) {
    this.startPosition = value
    this
  }

  static SpreadsheetWriteOptions fromMap(Map<String, ?> options) {
    SpreadsheetWriteOptions result = new SpreadsheetWriteOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)
    if (normalized.containsKey('sheetname')) {
      result.sheetName(String.valueOf(normalized.sheetname))
    }
    if (normalized.containsKey('startposition')) {
      result.startPosition(String.valueOf(normalized.startposition))
    }
    result
  }

  Map<String, ?> toMap() {
    Map<String, Object> result = [startPosition: startPosition]
    if (sheetName != null) {
      result.sheetName = sheetName
    }
    result
  }

  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor('sheetName', String, null, 'Sheet name to write to'),
        new OptionDescriptor('startPosition', String, 'A1', 'Top-left cell for the header row, e.g. B3')
    ]
  }
}
