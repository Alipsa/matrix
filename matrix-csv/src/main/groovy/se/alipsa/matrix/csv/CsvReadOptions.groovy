package se.alipsa.matrix.csv

import groovy.transform.CompileStatic

import org.apache.commons.csv.DuplicateHeaderMode

import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.util.Locale

/**
 * Typed options class for CSV read operations via the SPI.
 *
 * <p>Wraps the {@link CsvOption} values as typed fields with fluent setters,
 * making it easy to build a well-documented options configuration.</p>
 *
 * <pre>{@code
 * def opts = new CsvReadOptions()
 *     .delimiter(';' as Character)
 *     .firstRowAsHeader(false)
 *     .charset('ISO-8859-1')
 *
 * println CsvReadOptions.describe()
 * }</pre>
 *
 * @see CsvFormatProvider
 * @see CsvOption
 */
@CompileStatic
@SuppressWarnings(['DuplicateStringLiteral', 'ReturnsNullInsteadOfEmptyCollection'])
class CsvReadOptions {

  private static final String BOOLEAN_TRUE = 'true'
  private static final Character DEFAULT_DELIMITER = ',' as Character
  private static final Character DEFAULT_QUOTE = '"' as Character
  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8
  private static final boolean DEFAULT_FIRST_ROW_AS_HEADER = true
  private static final boolean DEFAULT_TRIM = true
  private static final boolean DEFAULT_IGNORE_EMPTY_LINES = true
  private static final boolean DEFAULT_IGNORE_SURROUNDING_SPACES = true
  private static final DuplicateHeaderMode DEFAULT_DUPLICATE_HEADER_MODE = DuplicateHeaderMode.ALLOW_EMPTY
  private static final String DEFAULT_RECORD_SEPARATOR = '\n'

  private boolean delimiterConfigured = false

  Character delimiter = DEFAULT_DELIMITER
  Character quote = DEFAULT_QUOTE
  Character escape = null
  Character commentMarker = null
  List<String> header = null
  boolean firstRowAsHeader = DEFAULT_FIRST_ROW_AS_HEADER
  Charset charset = DEFAULT_CHARSET
  String tableName = null
  List<Class> types = null
  String dateTimeFormat = null
  NumberFormat numberFormat = null
  boolean trim = DEFAULT_TRIM
  boolean ignoreEmptyLines = DEFAULT_IGNORE_EMPTY_LINES
  boolean ignoreSurroundingSpaces = DEFAULT_IGNORE_SURROUNDING_SPACES
  String nullString = null
  DuplicateHeaderMode duplicateHeaderMode = DEFAULT_DUPLICATE_HEADER_MODE
  String recordSeparator = DEFAULT_RECORD_SEPARATOR

  /** Sets the field delimiter character. */
  CsvReadOptions delimiter(Character c) {
    if (c == null) {
      throw new IllegalArgumentException('delimiter must not be null')
    }
    this.delimiter = c
    this.delimiterConfigured = true
    this
  }

  /** Sets the field delimiter as a single-character string. */
  CsvReadOptions delimiter(String s) {
    delimiter(CsvOptionUtil.requiredCharacterValue(s, 'delimiter'))
  }

  /** Sets the quote character. */
  CsvReadOptions quote(Character c) { this.quote = c; this }

  /** Sets the quote character as a single-character string, or empty to disable quoting. */
  CsvReadOptions quote(String s) { this.quote = CsvOptionUtil.optionalCharacterValue(s, 'quote'); this }

  /** Sets the escape character. */
  CsvReadOptions escape(Character c) { this.escape = c; this }

  /** Sets the escape character as a single-character string, or empty to disable escaping. */
  CsvReadOptions escape(String s) { this.escape = CsvOptionUtil.optionalCharacterValue(s, 'escape'); this }

  /** Sets the comment marker character. */
  CsvReadOptions commentMarker(Character c) { this.commentMarker = c; this }

  /** Sets the comment marker as a single-character string, or empty to disable comments. */
  CsvReadOptions commentMarker(String s) { this.commentMarker = CsvOptionUtil.optionalCharacterValue(s, 'commentMarker'); this }

  /** Sets explicit column header names. */
  CsvReadOptions header(List<String> h) {
    this.header = h
    if (h != null) {
      this.firstRowAsHeader = false
    }
    this
  }

  /** Sets explicit column header names from an array. */
  CsvReadOptions header(String... h) { header(h == null ? null : h.toList()) }

  /** Sets whether the first row contains column names. */
  CsvReadOptions firstRowAsHeader(boolean b) { this.firstRowAsHeader = b; this }

  /** Sets the character encoding. */
  CsvReadOptions charset(Charset cs) { this.charset = cs; this }

  /** Sets the character encoding by name. */
  CsvReadOptions charset(String cs) { this.charset = Charset.forName(cs); this }

  /** Sets the name for the resulting Matrix. */
  CsvReadOptions tableName(String name) { this.tableName = name; this }

  /** Sets column types for automatic conversion. */
  CsvReadOptions types(List<Class> t) { this.types = t; this }

  /** Sets the date/time format pattern for type conversion. */
  CsvReadOptions dateTimeFormat(String fmt) { this.dateTimeFormat = fmt; this }

  /** Sets the NumberFormat for locale-aware number parsing. */
  CsvReadOptions numberFormat(NumberFormat nf) { this.numberFormat = nf; this }

  /** Sets whether to trim whitespace from values. */
  CsvReadOptions trim(boolean b) { this.trim = b; this }

  /** Sets whether to ignore empty lines. */
  CsvReadOptions ignoreEmptyLines(boolean b) { this.ignoreEmptyLines = b; this }

  /** Sets whether to ignore spaces around quoted values. */
  CsvReadOptions ignoreSurroundingSpaces(boolean b) { this.ignoreSurroundingSpaces = b; this }

  /** Sets the string to interpret as null. */
  CsvReadOptions nullString(String s) { this.nullString = s; this }

  /** Sets how duplicate CSV header names are handled. */
  CsvReadOptions duplicateHeaderMode(DuplicateHeaderMode mode) {
    this.duplicateHeaderMode = mode == null ? DEFAULT_DUPLICATE_HEADER_MODE : mode
    this
  }

  /** Sets the duplicate header mode from a case-insensitive enum name. */
  CsvReadOptions duplicateHeaderMode(String mode) {
    duplicateHeaderMode(DuplicateHeaderMode.valueOf(String.valueOf(mode).trim().toUpperCase(Locale.ROOT)))
  }

  /** Sets the record separator string. */
  CsvReadOptions recordSeparator(String sep) { this.recordSeparator = sep; this }

  /**
   * @return true when the delimiter was explicitly configured by the caller
   */
  boolean isDelimiterConfigured() {
    delimiterConfigured
  }

  /**
   * Creates a typed CSV read options instance from a generic SPI option map.
   *
   * @param options the SPI options map
   * @return normalized read options
   */
  static CsvReadOptions fromMap(Map<String, ?> options) {
    CsvReadOptions result = new CsvReadOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)
    normalized.each { String key, Object value ->
      if (key == 'delimiter') {
        result.delimiter(CsvOptionUtil.requiredCharacterValue(value, 'delimiter'))
      } else if (key == 'quote') {
        result.quote(CsvOptionUtil.optionalCharacterValue(value, 'quote'))
      } else if (key == 'escape') {
        result.escape(CsvOptionUtil.optionalCharacterValue(value, 'escape'))
      } else if (key == 'commentmarker') {
        result.commentMarker(CsvOptionUtil.optionalCharacterValue(value, 'commentMarker'))
      } else if (key == 'header') {
        result.header(resolveHeader(value))
      } else if (key == 'firstrowasheader') {
        result.firstRowAsHeader(CsvOptionUtil.booleanValue(value, 'firstRowAsHeader'))
      } else if (key == 'charset') {
        result.charset(resolveCharset(value))
      } else if (key == 'tablename' || key == 'matrixname') {
        String tableName = OptionMaps.stringValueOrNull(value)
        if (tableName != null) {
          result.tableName(tableName)
        }
      } else if (key == 'types') {
        result.types(resolveTypes(value))
      } else if (key == 'datetimeformat') {
        result.dateTimeFormat(OptionMaps.stringValueOrNull(value))
      } else if (key == 'numberformat') {
        if (!(value instanceof NumberFormat)) {
          throw new IllegalArgumentException("numberFormat must be a NumberFormat but was ${value?.class}")
        }
        result.numberFormat(value as NumberFormat)
      } else if (key == 'trim') {
        result.trim(CsvOptionUtil.booleanValue(value, 'trim'))
      } else if (key == 'ignoreemptylines') {
        result.ignoreEmptyLines(CsvOptionUtil.booleanValue(value, 'ignoreEmptyLines'))
      } else if (key == 'ignoresurroundingspaces') {
        result.ignoreSurroundingSpaces(CsvOptionUtil.booleanValue(value, 'ignoreSurroundingSpaces'))
      } else if (key == 'nullstring') {
        result.nullString(OptionMaps.stringValueOrNull(value))
      } else if (key == 'duplicateheadermode') {
        if (value instanceof DuplicateHeaderMode) {
          result.duplicateHeaderMode(value as DuplicateHeaderMode)
        } else {
          result.duplicateHeaderMode(OptionMaps.stringValueOrNull(value))
        }
      } else if (key == 'recordseparator') {
        String recordSeparator = OptionMaps.stringValueOrNull(value)
        if (recordSeparator == null) {
          throw new IllegalArgumentException('recordSeparator must be a String')
        }
        result.recordSeparator(recordSeparator)
      } else {
        throw new IllegalArgumentException("Unknown CsvReadOptions option: '${key}'")
      }
    }
    result
  }

  /**
   * Returns a human-readable description of all CSV read options.
   *
   * @return formatted table of option descriptors
   */
  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  /**
   * Converts this options object to a Map suitable for the SPI.
   *
   * @return map of option names to their values
   */
  Map<String, ?> toMap() {
    Map<String, Object> m = [:]
    if (delimiterConfigured || delimiter != DEFAULT_DELIMITER) {
      m.delimiter = delimiter
    }
    if (quote != DEFAULT_QUOTE) {
      m.quote = quote
    }
    if (escape != null) {
      m.escape = escape
    }
    if (commentMarker != null) {
      m.commentMarker = commentMarker
    }
    if (header != null) {
      m.header = header
    }
    if (header == null && firstRowAsHeader != DEFAULT_FIRST_ROW_AS_HEADER) {
      m.firstRowAsHeader = firstRowAsHeader
    }
    if (charset != DEFAULT_CHARSET) {
      m.charset = charset
    }
    if (tableName != null) {
      m.tableName = tableName
    }
    if (types != null) {
      m.types = types
    }
    if (dateTimeFormat != null) {
      m.dateTimeFormat = dateTimeFormat
    }
    if (numberFormat != null) {
      m.numberFormat = numberFormat
    }
    if (trim != DEFAULT_TRIM) {
      m.trim = trim
    }
    if (ignoreEmptyLines != DEFAULT_IGNORE_EMPTY_LINES) {
      m.ignoreEmptyLines = ignoreEmptyLines
    }
    if (ignoreSurroundingSpaces != DEFAULT_IGNORE_SURROUNDING_SPACES) {
      m.ignoreSurroundingSpaces = ignoreSurroundingSpaces
    }
    if (nullString != null) {
      m.nullString = nullString
    }
    if (duplicateHeaderMode != DEFAULT_DUPLICATE_HEADER_MODE) {
      m.duplicateHeaderMode = duplicateHeaderMode
    }
    if (recordSeparator != DEFAULT_RECORD_SEPARATOR) {
      m.recordSeparator = recordSeparator
    }
    m
  }

  /**
   * Returns the list of option descriptors for CSV read options.
   *
   * @return list of {@link OptionDescriptor} instances
   */
  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor('delimiter', Character, ',', 'The character used to separate values'),
        new OptionDescriptor('quote', Character, '"', 'The character used to enclose fields'),
        new OptionDescriptor('escape', Character, null, 'The escape character'),
        new OptionDescriptor('commentMarker', Character, null, 'The character that marks comment lines'),
        new OptionDescriptor('header', List, null, 'Explicit list of column names'),
        new OptionDescriptor('firstRowAsHeader', Boolean, BOOLEAN_TRUE, 'Whether the first row contains column names'),
        new OptionDescriptor('charset', Charset, 'UTF-8', 'The character encoding'),
        new OptionDescriptor('tableName', String, null, 'The name for the resulting Matrix'),
        new OptionDescriptor('types', List, null, 'List of column types for automatic conversion'),
        new OptionDescriptor('dateTimeFormat', String, null, 'Date/time format pattern for type conversion'),
        new OptionDescriptor('numberFormat', NumberFormat, null, 'NumberFormat for locale-aware number parsing'),
        new OptionDescriptor('trim', Boolean, BOOLEAN_TRUE, 'Whether to trim whitespace from values'),
        new OptionDescriptor('ignoreEmptyLines', Boolean, BOOLEAN_TRUE, 'Whether to skip blank lines'),
        new OptionDescriptor('ignoreSurroundingSpaces', Boolean, BOOLEAN_TRUE, 'Whether to ignore spaces around quoted values'),
        new OptionDescriptor('nullString', String, null, 'String to interpret as null'),
        new OptionDescriptor('duplicateHeaderMode', DuplicateHeaderMode, DEFAULT_DUPLICATE_HEADER_MODE, 'How to handle duplicate header fields'),
        new OptionDescriptor('recordSeparator', String, '\\n', 'The record separator string'),
    ]
  }

  private static Charset resolveCharset(Object value) {
    if (value instanceof Charset) {
      return CsvOptionUtil.resolveCharset(value as Charset)
    }
    if (value instanceof CharSequence) {
      return CsvOptionUtil.resolveCharset(value as CharSequence)
    }
    throw new IllegalArgumentException("charset must be a Charset or CharSequence but was ${value?.class}")
  }

  private static List<String> resolveHeader(Object value) {
    if (value == null) {
      return null
    }
    if (value instanceof String[]) {
      return (value as String[]).toList()
    }
    if (value instanceof Collection) {
      return (value as Collection).collect { item -> String.valueOf(item) }
    }
    if (value instanceof CharSequence) {
      return [String.valueOf(value)]
    }
    throw new IllegalArgumentException("header must be a List, array, or String but was ${value?.class}")
  }

  private static List<Class> resolveTypes(Object value) {
    if (value == null) {
      return null
    }
    if (value instanceof Class[]) {
      return (value as Class[]).toList()
    }
    if (value instanceof Collection) {
      Collection values = value as Collection
      if (values.any { !(it instanceof Class) }) {
        throw new IllegalArgumentException('types must contain only Class values')
      }
      return values.collect { it as Class }
    }
    throw new IllegalArgumentException("types must be a Collection<Class> or Class[] but was ${value?.class}")
  }
}
