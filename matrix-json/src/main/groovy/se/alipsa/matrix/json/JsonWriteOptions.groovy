package se.alipsa.matrix.json

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

/**
 * Typed options for JSON write operations via the SPI.
 */
@CompileStatic
class JsonWriteOptions {

  boolean indent = false
  String dateFormat = 'yyyy-MM-dd'
  Map<String, Closure> columnFormatters = [:]

  JsonWriteOptions indent(boolean value) {
    this.indent = value
    this
  }

  JsonWriteOptions dateFormat(String value) {
    this.dateFormat = value
    this
  }

  JsonWriteOptions columnFormatters(Map<String, Closure> value) {
    this.columnFormatters = value ?: [:]
    this
  }

  static JsonWriteOptions fromMap(Map<String, ?> options) {
    JsonWriteOptions result = new JsonWriteOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)
    if (normalized.containsKey('indent')) {
      result.indent(normalized.indent as boolean)
    }
    if (normalized.containsKey('dateformat')) {
      String dateFormat = OptionMaps.stringValueOrNull(normalized.dateformat)
      if (dateFormat != null) {
        result.dateFormat(dateFormat)
      }
    }
    if (normalized.containsKey('columnformatters')) {
      def value = normalized.columnformatters
      if (value == null) {
        return result
      }
      if (!(value instanceof Map)) {
        throw new IllegalArgumentException("columnFormatters must be a Map<String, Closure> but was ${value?.class}")
      }
      result.columnFormatters((Map<String, Closure>) value)
    }
    result
  }

  Map<String, ?> toMap() {
    Map<String, Object> options = [
        indent    : indent,
        dateFormat: dateFormat
    ]
    if (!columnFormatters.isEmpty()) {
      options.columnFormatters = columnFormatters
    }
    options
  }

  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor('indent', Boolean, 'false', 'Whether to pretty-print the JSON output'),
        new OptionDescriptor('dateFormat', String, 'yyyy-MM-dd', 'Date format pattern for temporal values'),
        new OptionDescriptor('columnFormatters', Map, null, 'Map of column names to formatting closures')
    ]
  }
}
