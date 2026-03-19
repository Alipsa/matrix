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
    if (value == null) {
      return this
    }
    requireText(value, 'sheetName')
    this.sheetName = value
    this
  }

  SpreadsheetWriteOptions startPosition(String value) {
    if (value == null) {
      return this
    }
    requireText(value, 'startPosition')
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
        new OptionDescriptor('sheetName', String, null, 'Sheet name to write to'),
        new OptionDescriptor('startPosition', String, 'A1', 'Top-left cell for the header row, e.g. B3')
    ]
  }

  private static void requireText(String value, String name) {
    if (value.isBlank()) {
      throw new IllegalArgumentException("$name must not be blank")
    }
  }
}
