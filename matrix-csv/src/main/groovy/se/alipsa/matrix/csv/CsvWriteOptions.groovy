package se.alipsa.matrix.csv

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Typed options class for CSV write operations via the SPI.
 *
 * <p>Wraps the write-relevant options as typed fields with fluent setters.</p>
 *
 * <pre>{@code
 * def opts = new CsvWriteOptions()
 *     .delimiter(';' as Character)
 *     .withHeader(false)
 *
 * println CsvWriteOptions.describe()
 * }</pre>
 *
 * @see CsvFormatProvider
 * @see CsvWriter
 */
@CompileStatic
@SuppressWarnings('DuplicateStringLiteral')
class CsvWriteOptions {

  private static final Character DEFAULT_DELIMITER = ',' as Character
  private static final Character DEFAULT_QUOTE = '"' as Character
  private static final Character DEFAULT_ESCAPE = null
  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8
  private static final boolean DEFAULT_WITH_HEADER = true
  private static final String DEFAULT_RECORD_SEPARATOR = '\n'
  private static final String DEFAULT_NULL_STRING = null

  private boolean delimiterConfigured = false

  Character delimiter = DEFAULT_DELIMITER
  Character quote = DEFAULT_QUOTE
  Character escape = DEFAULT_ESCAPE
  boolean withHeader = DEFAULT_WITH_HEADER
  Charset charset = DEFAULT_CHARSET
  String recordSeparator = DEFAULT_RECORD_SEPARATOR
  String nullString = DEFAULT_NULL_STRING

  /** Sets the field delimiter character. */
  CsvWriteOptions delimiter(Character c) {
    if (c == null) {
      throw new IllegalArgumentException('delimiter must not be null')
    }
    this.delimiter = c
    this.delimiterConfigured = true
    this
  }

  /** Sets the field delimiter as a single-character string. */
  CsvWriteOptions delimiter(String s) {
    delimiter(CsvOptionUtil.requiredCharacterValue(s, 'delimiter'))
  }

  /** Sets the quote character. */
  CsvWriteOptions quote(Character c) { this.quote = c; this }

  /** Sets the quote character as a single-character string, or empty to disable quoting. */
  CsvWriteOptions quote(String s) { this.quote = CsvOptionUtil.optionalCharacterValue(s, 'quote'); this }

  /** Sets the escape character. */
  CsvWriteOptions escape(Character c) { this.escape = c; this }

  /** Sets the escape character as a single-character string, or empty to disable escaping. */
  CsvWriteOptions escape(String s) { this.escape = CsvOptionUtil.optionalCharacterValue(s, 'escape'); this }

  /** Sets whether to include column names in the first row. */
  CsvWriteOptions withHeader(boolean b) { this.withHeader = b; this }

  /** Sets the character encoding. */
  CsvWriteOptions charset(Charset cs) { this.charset = cs; this }

  /** Sets the character encoding by name. */
  CsvWriteOptions charset(String cs) { this.charset = Charset.forName(cs); this }

  /** Sets the record separator string. */
  CsvWriteOptions recordSeparator(String sep) { this.recordSeparator = sep; this }

  /** Sets the string to write for null values. */
  CsvWriteOptions nullString(String s) { this.nullString = s; this }

  /**
   * @return true when the delimiter was explicitly configured by the caller
   */
  boolean isDelimiterConfigured() {
    delimiterConfigured
  }

  /**
   * Creates a typed CSV write options instance from a generic SPI option map.
   *
   * @param options the SPI options map
   * @return normalized write options
   */
  static CsvWriteOptions fromMap(Map<String, ?> options) {
    CsvWriteOptions result = new CsvWriteOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)
    normalized.each { String key, Object value ->
      if (key == 'delimiter') {
        result.delimiter(CsvOptionUtil.requiredCharacterValue(value, 'delimiter'))
      } else if (key == 'quote') {
        result.quote(CsvOptionUtil.optionalCharacterValue(value, 'quote'))
      } else if (key == 'escape') {
        result.escape(CsvOptionUtil.optionalCharacterValue(value, 'escape'))
      } else if (key == 'withheader') {
        result.withHeader(CsvOptionUtil.booleanValue(value, 'withHeader'))
      } else if (key == 'charset') {
        if (value instanceof Charset) {
          result.charset(CsvOptionUtil.resolveCharset(value as Charset))
        } else if (value instanceof CharSequence) {
          result.charset(CsvOptionUtil.resolveCharset(value as CharSequence))
        } else {
          throw new IllegalArgumentException("charset must be a Charset or CharSequence but was ${value?.class}")
        }
      } else if (key == 'recordseparator') {
        String recordSeparator = OptionMaps.stringValueOrNull(value)
        if (recordSeparator == null) {
          throw new IllegalArgumentException('recordSeparator must be a String')
        }
        result.recordSeparator(recordSeparator)
      } else if (key == 'nullstring') {
        result.nullString(OptionMaps.stringValueOrNull(value))
      } else {
        throw new IllegalArgumentException("Unknown CsvWriteOptions option: '${key}'")
      }
    }
    result
  }

  /**
   * Returns a human-readable description of all CSV write options.
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
    Map<String, Object> result = [:]
    if (delimiterConfigured || delimiter != DEFAULT_DELIMITER) {
      result.delimiter = delimiter
    }
    if (quote != DEFAULT_QUOTE) {
      result.quote = quote
    }
    if (escape != DEFAULT_ESCAPE) {
      result.escape = escape
    }
    if (withHeader != DEFAULT_WITH_HEADER) {
      result.withHeader = withHeader
    }
    if (charset != DEFAULT_CHARSET) {
      result.charset = charset
    }
    if (recordSeparator != DEFAULT_RECORD_SEPARATOR) {
      result.recordSeparator = recordSeparator
    }
    if (nullString != DEFAULT_NULL_STRING) {
      result.nullString = nullString
    }
    result
  }

  /**
   * Returns the list of option descriptors for CSV write options.
   *
   * @return list of {@link OptionDescriptor} instances
   */
  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor('delimiter', Character, ',', 'The character used to separate values'),
        new OptionDescriptor('quote', Character, '"', 'The character used to enclose fields'),
        new OptionDescriptor('escape', Character, null, 'The escape character used when quoting is disabled or escaping is required'),
        new OptionDescriptor('withHeader', Boolean, 'true', 'Whether to include column names in the first row'),
        new OptionDescriptor('charset', Charset, 'UTF-8', 'The character encoding'),
        new OptionDescriptor('recordSeparator', String, '\\n', 'The record separator string'),
        new OptionDescriptor('nullString', String, null, 'String to write for null values'),
    ]
  }
}
