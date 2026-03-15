package se.alipsa.matrix.csv

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.spi.OptionDescriptor

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.text.NumberFormat

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
class CsvReadOptions {

  Character delimiter = ',' as Character
  Character quote = '"' as Character
  Character escape = null
  Character commentMarker = null
  List<String> header = null
  boolean firstRowAsHeader = true
  Charset charset = StandardCharsets.UTF_8
  String tableName = null
  List<Class> types = null
  String dateTimeFormat = null
  NumberFormat numberFormat = null
  boolean trim = true
  boolean ignoreEmptyLines = true
  boolean ignoreSurroundingSpaces = true
  String nullString = null
  String duplicateHeaderMode = 'ALLOW_EMPTY'
  String recordSeparator = '\n'

  /** Sets the field delimiter character. */
  CsvReadOptions delimiter(Character c) { this.delimiter = c; this }

  /** Sets the quote character. */
  CsvReadOptions quote(Character c) { this.quote = c; this }

  /** Sets the escape character. */
  CsvReadOptions escape(Character c) { this.escape = c; this }

  /** Sets the comment marker character. */
  CsvReadOptions commentMarker(Character c) { this.commentMarker = c; this }

  /** Sets explicit column header names. */
  CsvReadOptions header(List<String> h) { this.header = h; this }

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

  /** Sets the duplicate header mode. */
  CsvReadOptions duplicateHeaderMode(String mode) { this.duplicateHeaderMode = mode; this }

  /** Sets the record separator string. */
  CsvReadOptions recordSeparator(String sep) { this.recordSeparator = sep; this }

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
    Map<String, ?> m = [:]
    m.delimiter = delimiter
    m.quote = quote
    if (escape != null) m.escape = escape
    if (commentMarker != null) m.commentMarker = commentMarker
    if (header != null) m.header = header
    m.firstRowAsHeader = firstRowAsHeader
    m.charset = charset
    if (tableName != null) m.tableName = tableName
    if (types != null) m.types = types
    if (dateTimeFormat != null) m.dateTimeFormat = dateTimeFormat
    if (numberFormat != null) m.numberFormat = numberFormat
    m.trim = trim
    m.ignoreEmptyLines = ignoreEmptyLines
    m.ignoreSurroundingSpaces = ignoreSurroundingSpaces
    if (nullString != null) m.nullString = nullString
    m.duplicateHeaderMode = duplicateHeaderMode
    m.recordSeparator = recordSeparator
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
        new OptionDescriptor('firstRowAsHeader', Boolean, 'true', 'Whether the first row contains column names'),
        new OptionDescriptor('charset', Charset, 'UTF-8', 'The character encoding'),
        new OptionDescriptor('tableName', String, null, 'The name for the resulting Matrix'),
        new OptionDescriptor('types', List, null, 'List of column types for automatic conversion'),
        new OptionDescriptor('dateTimeFormat', String, null, 'Date/time format pattern for type conversion'),
        new OptionDescriptor('numberFormat', NumberFormat, null, 'NumberFormat for locale-aware number parsing'),
        new OptionDescriptor('trim', Boolean, 'true', 'Whether to trim whitespace from values'),
        new OptionDescriptor('ignoreEmptyLines', Boolean, 'true', 'Whether to skip blank lines'),
        new OptionDescriptor('ignoreSurroundingSpaces', Boolean, 'true', 'Whether to ignore spaces around quoted values'),
        new OptionDescriptor('nullString', String, null, 'String to interpret as null'),
        new OptionDescriptor('duplicateHeaderMode', String, 'ALLOW_EMPTY', 'How to handle duplicate header fields'),
        new OptionDescriptor('recordSeparator', String, '\\n', 'The record separator string'),
    ]
  }
}
