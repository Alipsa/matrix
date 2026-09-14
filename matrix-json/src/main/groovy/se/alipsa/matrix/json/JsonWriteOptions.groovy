package se.alipsa.matrix.json

import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

import java.time.format.DateTimeFormatter

/**
 * Typed options for JSON write operations via the SPI.
 */
class JsonWriteOptions {

  private static final String OPT_INDENT = 'indent'
  private static final String OPT_DATE_FORMAT = 'dateFormat'
  private static final String OPT_DATE_TIME_FORMAT = 'dateTimeFormat'
  private static final String DEFAULT_DATE_FORMAT = 'yyyy-MM-dd'
  private static final String DEFAULT_INDENT = 'false'

  boolean indent = false
  String dateFormat = DEFAULT_DATE_FORMAT
  String dateTimeFormat = null
  Map<String, Closure> columnFormatters = [:]

  JsonWriteOptions indent(boolean value) {
    this.indent = value
    this
  }

  JsonWriteOptions dateFormat(String value) {
    validatePattern(OPT_DATE_FORMAT, value)
    this.dateFormat = value
    this
  }

  /**
   * Set the LocalDateTime format pattern.
   *
   * @param value a date-time pattern, or null for ISO-8601 output
   * @return this options instance for chaining
   */
  JsonWriteOptions dateTimeFormat(String value) {
    validatePattern(OPT_DATE_TIME_FORMAT, value)
    this.dateTimeFormat = value
    this
  }

  JsonWriteOptions columnFormatters(Map<String, Closure> value) {
    validateColumnFormatters(value)
    this.columnFormatters = value ?: [:]
    this
  }

  /**
   * Validates a date or date-time pattern shared by the fluent writer and SPI options.
   *
   * @param name the option name used in validation messages
   * @param pattern the pattern to validate
   * @throws IllegalArgumentException if the pattern is blank, invalid, or null for dateFormat
   */
  static void validatePattern(String name, String pattern) {
    if (pattern == null) {
      if (name == OPT_DATE_FORMAT) {
        throw new IllegalArgumentException("${name} pattern cannot be null or blank")
      }
      return
    }
    if (pattern.trim().isEmpty()) {
      throw new IllegalArgumentException("${name} pattern cannot be null or blank")
    }
    DateTimeFormatter.ofPattern(pattern)
  }

  /**
   * Validates column formatter names and values shared by the fluent writer and SPI options.
   *
   * @param formatters the formatters to validate
   * @throws IllegalArgumentException if a formatter name or value is invalid
   */
  static void validateColumnFormatters(Map formatters) {
    if (formatters == null) {
      return
    }
    formatters.each { Object key, Object value ->
      if (key == null || (key instanceof CharSequence && key.toString().trim().isEmpty())) {
        throw new IllegalArgumentException('columnFormatters column name cannot be null or blank')
      }
      if (!(key instanceof String)) {
        throw new IllegalArgumentException("columnFormatters key must be a String but was ${key.class}")
      }
      if (!(value instanceof Closure)) {
        throw new IllegalArgumentException("columnFormatters value for column '${key}' must be a Closure but was ${value?.class}")
      }
    }
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
      result.dateFormat(OptionMaps.stringValueOrNull(normalized.dateformat))
    }
    if (normalized.containsKey('datetimeformat')) {
      result.dateTimeFormat(OptionMaps.stringValueOrNull(normalized.datetimeformat))
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
        dateFormat: dateFormat,
        dateTimeFormat: dateTimeFormat
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
        new OptionDescriptor(OPT_DATE_FORMAT, String, DEFAULT_DATE_FORMAT, 'Date format pattern for LocalDate values'),
        new OptionDescriptor(OPT_DATE_TIME_FORMAT, String, null, 'Date-time format pattern for LocalDateTime values (default: ISO-8601)'),
        new OptionDescriptor('columnFormatters', Map, null, 'Map of column names to formatting closures')
    ]
  }
}
