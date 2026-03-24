package se.alipsa.matrix.csv

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.spi.OptionDescriptor

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
class CsvWriteOptions {

  private static final String BOOLEAN_TRUE = 'true'

  Character delimiter = ',' as Character
  Character quote = '"' as Character
  boolean withHeader = true
  Charset charset = StandardCharsets.UTF_8
  String recordSeparator = '\n'

  /** Sets the field delimiter character. */
  CsvWriteOptions delimiter(Character c) { this.delimiter = c; this }

  /** Sets the quote character. */
  CsvWriteOptions quote(Character c) { this.quote = c; this }

  /** Sets whether to include column names in the first row. */
  CsvWriteOptions withHeader(boolean b) { this.withHeader = b; this }

  /** Sets the character encoding. */
  CsvWriteOptions charset(Charset cs) { this.charset = cs; this }

  /** Sets the character encoding by name. */
  CsvWriteOptions charset(String cs) { this.charset = Charset.forName(cs); this }

  /** Sets the record separator string. */
  CsvWriteOptions recordSeparator(String sep) { this.recordSeparator = sep; this }

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
    [
        delimiter      : delimiter,
        quote          : quote,
        withHeader     : withHeader,
        charset        : charset,
        recordSeparator: recordSeparator,
    ]
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
        new OptionDescriptor('withHeader', Boolean, BOOLEAN_TRUE, 'Whether to include column names in the first row'),
        new OptionDescriptor('charset', Charset, 'UTF-8', 'The character encoding'),
        new OptionDescriptor('recordSeparator', String, '\\n', 'The record separator string'),
    ]
  }
}
