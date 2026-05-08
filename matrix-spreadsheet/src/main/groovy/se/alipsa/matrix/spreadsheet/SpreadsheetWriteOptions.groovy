package se.alipsa.matrix.spreadsheet

import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

/**
 * Typed options for spreadsheet write operations via the SPI.
 */
class SpreadsheetWriteOptions {

  private static final String DEFAULT_START = 'A1'
  private static final String OPT_SHEET_NAME = 'sheetName'
  private static final String OPT_START_POSITION = 'startPosition'

  String sheetName = null
  String startPosition = DEFAULT_START

  SpreadsheetWriteOptions sheetName(String value) {
    if (value == null) {
      return this
    }
    requireText(value, OPT_SHEET_NAME)
    this.sheetName = value
    this
  }

  SpreadsheetWriteOptions startPosition(String value) {
    if (value == null) {
      return this
    }
    requireText(value, OPT_START_POSITION)
    this.startPosition = value
    this
  }

  static SpreadsheetWriteOptions fromMap(Map<String, ?> options) {
    SpreadsheetWriteOptions result = new SpreadsheetWriteOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)
    if (normalized.containsKey('sheetname')) {
      String sheetName = OptionMaps.stringValueOrNull(normalized.sheetname)
      if (sheetName != null) {
        result.sheetName(sheetName)
      }
    }
    if (normalized.containsKey('startposition')) {
      String startPosition = OptionMaps.stringValueOrNull(normalized.startposition)
      if (startPosition != null) {
        result.startPosition(startPosition)
      }
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
        new OptionDescriptor(OPT_SHEET_NAME, String, null, 'Sheet name to write to'),
        new OptionDescriptor(OPT_START_POSITION, String, DEFAULT_START, 'Top-left cell for the header row, e.g. B3')
    ]
  }

  private static void requireText(String value, String name) {
    if (value.isBlank()) {
      throw new IllegalArgumentException("$name must not be blank")
    }
  }

}
