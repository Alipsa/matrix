package se.alipsa.matrix.json

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

/**
 * Typed options for JSON write operations via the SPI.
 */
@CompileStatic
class JsonWriteOptions {

  private static final String OPT_INDENT = 'indent'
  private static final String OPT_DATE_FORMAT = 'dateFormat'
  private static final String DEFAULT_DATE_FORMAT = 'yyyy-MM-dd'
  private static final String DEFAULT_INDENT = 'false'

  boolean indent = false
  String dateFormat = DEFAULT_DATE_FORMAT
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
    if (normalized.containsKey(OPT_INDENT)) {
      Object value = normalized.get(OPT_INDENT)
      if (value instanceof Boolean) {
        result.indent((boolean) value)
      } else if (value instanceof CharSequence) {
        String normalizedValue = value.toString().trim().toLowerCase(Locale.ROOT)
        if (normalizedValue == 'true') {
          result.indent(true)
        } else if (normalizedValue == DEFAULT_INDENT) {
          result.indent(false)
        } else {
          throw new IllegalArgumentException("${OPT_INDENT} must be 'true' or 'false' but was '${value}'")
        }
      } else if (value != null) {
        throw new IllegalArgumentException("${OPT_INDENT} must be a Boolean or String but was ${value?.class}")
      }
    }
    if (normalized.containsKey('dateformat')) {
      String dateFormat = OptionMaps.stringValueOrNull(normalized.dateformat)
      if (dateFormat != null) {
        result.dateFormat(dateFormat)
      }
    }
    if (normalized.containsKey('columnformatters')) {
      Object value = normalized.columnformatters
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
        new OptionDescriptor(OPT_INDENT, Boolean, DEFAULT_INDENT, 'Whether to pretty-print the JSON output'),
        new OptionDescriptor(OPT_DATE_FORMAT, String, DEFAULT_DATE_FORMAT, 'Date format pattern for temporal values'),
        new OptionDescriptor('columnFormatters', Map, null, 'Map of column names to formatting closures')
    ]
  }
}
