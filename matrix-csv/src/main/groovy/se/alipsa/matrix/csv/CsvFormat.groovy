package se.alipsa.matrix.csv

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.DuplicateHeaderMode
import org.apache.commons.csv.QuoteMode

/**
 * Internal immutable CSV format configuration class used by the fluent builder APIs
 * in {@link CsvReader} and {@link CsvWriter}.
 *
 * <p>This class is package-private. Use the fluent API entry points instead:</p>
 * <ul>
 *   <li>{@code CsvReader.read().from(file)}</li>
 *   <li>{@code CsvWriter.write(matrix).to(file)}</li>
 * </ul>
 */
@CompileStatic
@PackageScope
class CsvFormat {

  @PackageScope
  static final String CRLF = '\r\n'

  /** Default CSV format: comma-delimited, double-quote quoted, trimmed, ignoring empty lines. */
  static final CsvFormat DEFAULT = builder().build()

  /**
   * Excel-compatible CSV format aligned with Apache Commons {@link CSVFormat#EXCEL},
   * including CRLF line endings, {@link QuoteMode#ALL_NON_NULL}, no trimming,
   * no skipped empty lines, no ignored surrounding spaces, and support for
   * missing header names.
   */
  static final CsvFormat EXCEL = builder()
      .recordSeparator(CRLF)
      .trim(false)
      .ignoreEmptyLines(false)
      .ignoreSurroundingSpaces(false)
      .quoteMode(QuoteMode.ALL_NON_NULL)
      .allowMissingColumnNames(true)
      .build()

  /** Tab-delimited format (TSV). */
  static final CsvFormat TDF = builder().delimiter('\t' as char).build()

  /** RFC 4180 compliant CSV format with CRLF record separators. */
  static final CsvFormat RFC4180 = builder().recordSeparator(CRLF).build()

  /** The field delimiter character. Default: {@code ','} */
  final char delimiter

  /** The quote character for enclosing fields. Default: {@code '"'} */
  final Character quoteCharacter

  /** The escape character. Default: {@code null} (no escaping) */
  final Character escapeCharacter

  /** The comment marker character. Default: {@code null} (no comments) */
  final Character commentMarker

  /** Whether to trim whitespace from values. Default: {@code true} */
  final boolean trim

  /** Whether to ignore empty lines. Default: {@code true} */
  final boolean ignoreEmptyLines

  /** Whether to ignore spaces around quoted values. Default: {@code true} */
  final boolean ignoreSurroundingSpaces

  /** String to interpret as null when reading. Default: {@code null} (no null substitution) */
  final String nullString

  /** The record separator string. Default: {@code '\n'} */
  final String recordSeparator

  /** Quote mode used by Apache Commons CSV when writing. Default: Apache Commons CSV default. */
  final QuoteMode quoteMode

  /** Whether empty header names are allowed when parsing header rows. Default: {@code false}. */
  final boolean allowMissingColumnNames

  private CsvFormat(Builder builder) {
    this.delimiter = builder.delimiter
    this.quoteCharacter = builder.quoteCharacter
    this.escapeCharacter = builder.escapeCharacter
    this.commentMarker = builder.commentMarker
    this.trim = builder.trim
    this.ignoreEmptyLines = builder.ignoreEmptyLines
    this.ignoreSurroundingSpaces = builder.ignoreSurroundingSpaces
    this.nullString = builder.nullString
    this.recordSeparator = builder.recordSeparator
    this.quoteMode = builder.quoteMode
    this.allowMissingColumnNames = builder.allowMissingColumnNames
  }

  /**
   * Creates a new builder for constructing a {@link CsvFormat} instance.
   *
   * @return a new {@link Builder}
   */
  static Builder builder() {
    new Builder()
  }

  /**
   * Converts this {@link CsvFormat} to an Apache Commons CSV {@link CSVFormat}
   * for internal delegation. This is the only place commons-csv types appear
   * in the new API surface.
   *
   * @return an equivalent {@link CSVFormat}
   */
  @PackageScope
  CSVFormat toCSVFormat() {
    CSVFormat.Builder b = CSVFormat.Builder.create()
        .setDelimiter(delimiter)
        .setTrim(trim)
        .setIgnoreEmptyLines(ignoreEmptyLines)
        .setIgnoreSurroundingSpaces(ignoreSurroundingSpaces)
        .setRecordSeparator(recordSeparator)
        .setAllowMissingColumnNames(allowMissingColumnNames)
        .setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_EMPTY)

    b.setQuote(quoteCharacter)
    if (quoteMode != null) {
      b.setQuoteMode(quoteMode)
    }
    if (escapeCharacter != null) {
      b.setEscape(escapeCharacter)
    }
    if (commentMarker != null) {
      b.setCommentMarker(commentMarker)
    }
    b.setNullString(nullString)
    b.build()
  }

  @Override
  String toString() {
    "CsvFormat[delimiter=${delimiter}, quote=${quoteCharacter}, escape=${escapeCharacter}, " +
        "commentMarker=${commentMarker}, trim=${trim}, ignoreEmptyLines=${ignoreEmptyLines}, " +
        "ignoreSurroundingSpaces=${ignoreSurroundingSpaces}, nullString=${nullString}, " +
        "recordSeparator=${recordSeparator?.inspect()}, quoteMode=${quoteMode}, " +
        "allowMissingColumnNames=${allowMissingColumnNames}]"
  }

  /**
   * Builder for constructing immutable {@link CsvFormat} instances.
   *
   * <p>All fields have sensible defaults matching the behavior of the library's
   * existing default format.</p>
   */
  @CompileStatic
  static class Builder {
    private char delimiter = ',' as char
    private Character quoteCharacter = '"' as Character
    private Character escapeCharacter = null
    private Character commentMarker = null
    private boolean trim = true
    private boolean ignoreEmptyLines = true
    private boolean ignoreSurroundingSpaces = true
    private String nullString = null
    private String recordSeparator = '\n'
    private QuoteMode quoteMode = null
    private boolean allowMissingColumnNames = false

    /**
     * Sets the field delimiter character.
     *
     * @param c the delimiter character
     * @return this builder
     */
    Builder delimiter(char c) {
      this.delimiter = c
      this
    }

    /**
     * Sets the quote character for enclosing fields.
     *
     * @param c the quote character, or {@code null} to disable quoting
     * @return this builder
     */
    Builder quoteCharacter(Character c) {
      this.quoteCharacter = c
      this
    }

    /**
     * Sets the escape character.
     *
     * @param c the escape character, or {@code null} to disable escaping
     * @return this builder
     */
    Builder escapeCharacter(Character c) {
      this.escapeCharacter = c
      this
    }

    /**
     * Sets the comment marker character.
     *
     * @param c the comment marker character, or {@code null} to disable comments
     * @return this builder
     */
    Builder commentMarker(Character c) {
      this.commentMarker = c
      this
    }

    /**
     * Sets whether to trim whitespace from values.
     *
     * @param b {@code true} to trim values
     * @return this builder
     */
    Builder trim(boolean b) {
      this.trim = b
      this
    }

    /**
     * Sets whether to ignore empty lines.
     *
     * @param b {@code true} to ignore empty lines
     * @return this builder
     */
    Builder ignoreEmptyLines(boolean b) {
      this.ignoreEmptyLines = b
      this
    }

    /**
     * Sets whether to ignore spaces around quoted values.
     *
     * @param b {@code true} to ignore surrounding spaces
     * @return this builder
     */
    Builder ignoreSurroundingSpaces(boolean b) {
      this.ignoreSurroundingSpaces = b
      this
    }

    /**
     * Sets the string to interpret as null when reading records.
     *
     * @param s the null string, or {@code null} to disable null substitution
     * @return this builder
     */
    Builder nullString(String s) {
      this.nullString = s
      this
    }

    /**
     * Sets the record separator string.
     *
     * @param s the record separator
     * @return this builder
     */
    Builder recordSeparator(String s) {
      this.recordSeparator = s
      this
    }

    /**
     * Sets the quote mode used when writing values.
     *
     * @param mode the quote mode, or {@code null} to use Apache Commons CSV defaults
     * @return this builder
     */
    Builder quoteMode(QuoteMode mode) {
      this.quoteMode = mode
      this
    }

    /**
     * Sets whether empty header names are allowed when parsing CSV headers.
     *
     * @param allow {@code true} to allow missing column names
     * @return this builder
     */
    Builder allowMissingColumnNames(boolean allow) {
      this.allowMissingColumnNames = allow
      this
    }

    /**
     * Builds an immutable {@link CsvFormat} instance.
     *
     * @return a new {@link CsvFormat}
     */
    CsvFormat build() {
      new CsvFormat(this)
    }
  }
}
